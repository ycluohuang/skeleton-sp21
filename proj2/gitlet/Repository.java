package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TreeMap;
import java.util.Set;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  does at a high level.
 * // Thanks ZonePG
 *  @author luohuang
 */
public class Repository implements Serializable {


    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The current pointer */
    private String head;
    /** The current branch */
    private String branch;

    public Repository() {
        if (GITLET_DIR.exists()) {
            String sp = File.separator;
            File repo = join(GITLET_DIR, "REPO");
            if (repo.exists()) {
                Repository repoObj = Utils.readObject(repo, Repository.class);
                head = repoObj.head;
                branch = repoObj.branch;
            }
        }
    }

    public void init() {
        if(GITLET_DIR.exists()) { // error in 2024/8/12/3:30 - 4:58
            Utils.errorPrint("A Gitlet version-control system already exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        //create init commit
        Commit cmt = new Commit();
//        cmt.initCommit();
        head = cmt.write();
        // create master branch
        Branch br = new Branch("master", Utils.sha1(Utils.serialize(cmt)));
        br.write();
        branch = "master";
        save();
    }

    public void add(String fileName) {
//        File file = new File(GITLET_DIR, fileName);
        File file = new File(fileName);
        if (!file.exists()) {
            errorPrint("File does not exist."); // bug
        }
        // copy file to blob directory
        Blob blob = new Blob(file);
        String blobId = blob.write();
        //update staging area
        File stageFile = join(StagingArea.STAGE_FILE, blobId);
        StagingArea stage = new StagingArea();
        stage.add(fileName, blobId, head);
        stage.write();
    }

    public void commitCommand(String msg) {
        if (msg == null || msg.isEmpty()) {
            errorPrint("Please enter a commit message.");
        }
        Commit oldCmt = Commit.read(head);
        StagingArea stagingArea = new StagingArea(); // load all data from Stage-area
        if (stagingArea.isEmpty()) {
            errorPrint("No changes added to the commit.");
        }
        TreeMap<String, String> cmtBlobs = Commit.mergeBlobs(oldCmt, stagingArea);
        Commit newCommit = new Commit(msg, head, "", cmtBlobs);
        head = newCommit.write();
        stagingArea.clear();

        //read current branch and update branch
        Branch br = Branch.read(branch);
        br.updateLastCommit(head);
        br.write();
        save();
    }

    public void rm(String fileName) {
        File stageFile = Utils.join(StagingArea.STAGE_FILE);
        StagingArea stagingArea;
        if (!stageFile.exists()) {
            stagingArea = new StagingArea();
        } else {
            stagingArea = Utils.readObject(stageFile, StagingArea.class);
        }
        stagingArea.remove(fileName, head);
        stagingArea.write();
    }

    public void log() {
        String cmtId = head;
        while (!cmtId.equals("")) {
            Commit cmt = Commit.read(cmtId);
            System.out.println(cmt.toString());
            cmtId = cmt.getParent();
        }
    }

    public void globalLog() {
        List<String> list = Commit.getAllCommitLog();
        list.forEach(System.out::println);
    }

    public void find(String msg) {
        List<String> list = Commit.find(msg);
        if (list.isEmpty()) {
            errorPrint("Found no commit with that message.");
        }
        list.forEach(System.out::println);
    }

    public void status() {
        // Branch status
        System.out.println("=== Branches ===");
        for (String branchName : Branch.allBranches()) {
            if (branchName.equals(this.branch)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println();

        // Staged Files
        System.out.println("=== Staged Files ===");
        StagingArea stage = new StagingArea();
        for (String fileName : stage.getStagedFiles()) {
            System.out.println(fileName);
        }
        System.out.println();

        // Removed Files
        System.out.println("=== Removed Files ===");
        for (String fileName : stage.getRemoveFile()) {
            System.out.println(fileName);
        }
        System.out.println();

        //  Modifications
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String fileName : stage.getUnstagedFiles(head)) {
            System.out.println(fileName);
        }
        System.out.println();

        // Untracked Files
        System.out.println("=== Untracked Files ===");
        for (String fileName : stage.getUntrackedFiles(head)) {
            System.out.println(fileName);
        }
        System.out.println();
    }

    public void checkout(String cmtId, String fileName) {
        if (cmtId.equals("head")) {
            cmtId = head;
        }
        Commit cmt = Commit.read(cmtId);
        if (cmt == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        if (!cmt.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobId = cmt.getBlobs().get(fileName);
        byte[] fileContent = Blob.getBlobByte(blobId);
        Utils.writeContents(new File(fileName), fileContent);
    }

    public void checkout(String branchName) {
        if (branchName.equals(branch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        Branch br = Branch.read(branchName);
        if (br == null) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        updateCheckout(br.getLastCommit());
        // update repo
        branch = branchName;
        head = br.getLastCommit();
        save();
        // clear stage-area
        StagingArea stagingArea = new StagingArea();
        stagingArea.clear();
        stagingArea.write();
    }

    public void updateCheckout(String commitId) {
        // read target the newest commit
        Commit cmt = Commit.read(commitId);
        Set<String> cmtFiles = cmt.getBlobs().keySet();
        // read the newest commit in HEAD
        Commit curCmt = Commit.read(head);
        Set<String> curCmtFiles = curCmt.getBlobs().keySet();
        // check untracked files and overwrite them
        checkOverwritten(cmt, curCmt);
        // update target file
        for (String fileName : cmtFiles) {
            byte[] fileContent = Blob.getBlobByte(cmt.getBlobs().get(fileName));
            Utils.writeContents(new File(fileName), fileContent);
        }
        // delete current branch files which don't exist in target branch files.
        for (String fileName : curCmtFiles) {
            if (!cmtFiles.contains(fileName)) {
                Utils.restrictedDelete(new File(fileName));
            }
        }
    }

    public void checkOverwritten(Commit cmt, Commit curCmt) {
        // get CWD files
        List<String> curDirFiles = Utils.plainFilenamesIn(CWD);
        // get files in target the newest commit
        Set<String> cmtFiles = cmt.getBlobs().keySet();

        for (String fileName : curDirFiles) {
            if (!curCmt.getBlobs().containsKey(fileName)) {
                if (cmtFiles.contains(fileName)) {
                    String fileId = cmt.getBlobs().get(fileName);
                    if (!fileId.equals(Utils.readContents(new File(fileName)))) {
                        System.out.println("There is an untracked file in the way;" +
                                " delete it, or add and commit it first.");
                        System.exit(0);
                    }
                }
            }
        }
    }

    public void creatBranch(String branchName) {
        if (Branch.allBranches().contains(branchName)) {
            errorPrint("A branch with that name already exists.");
        }
        Branch br = new Branch(branchName, head);
        br.write();
    }

    public void removeBranch(String branchName) {
        if (!Branch.allBranches().contains(branchName)) {
            errorPrint("A branch with that name does not exist.");
        }
        if (branchName.equals(branch)) {
            errorPrint("Cannot remove the current branch.");
        }
        Branch.remove(branchName);
    }

    public void reset(String cmtId) {
        Commit cmt = Commit.read(cmtId);
        if (cmt == null) {
            errorPrint("No commit with that id exists.");
        }
        updateCheckout(cmtId);
        save();

        StagingArea stagingArea = new StagingArea();
        stagingArea.clear();
        stagingArea.write();
    }


    public void merge(String branchName) {
        StagingArea stage = new StagingArea();
        Branch otherBr = Branch.read(branchName);
        String splitId = checkFailCases(branchName, stage, otherBr);

        Commit splitCmt = Commit.read(splitId);
        Commit otherCmt = Commit.read(otherBr.getLastCommit());
        Commit headCmt = Commit.read(head);
        checkOverwritten(otherCmt, headCmt);

        TreeMap<String, String> headBlobs = headCmt.getBlobs();
        TreeMap<String, String> otherBlobs = otherCmt.getBlobs();
        TreeMap<String, String> splitBlobs = splitCmt.getBlobs();
        boolean confilct = false;
        for (String fileName : headBlobs.keySet()) {
            String headBlobId = headBlobs.get(fileName);
            String otherBlobId = otherBlobs.getOrDefault(fileName, "");
            String splitBlobId = splitBlobs.getOrDefault(fileName, "");

            if (splitBlobId.equals(headBlobId) ) {
                if (!otherBlobs.containsKey(fileName)) { // 6
                    continue;
                } else if (!otherBlobId.equals(headBlobId)) { // 1
                    checkout(otherBr.getLastCommit(), fileName);
                    stage.add(fileName, otherBlobId, head);
                }
            }
            if (!otherBlobs.containsKey(fileName) && splitBlobId.isEmpty()) { //4
                continue;
            }
            if (otherBlobs.equals(headBlobs)) {  // 3.1
                continue;
            }
            if (splitBlobId.equals(headBlobId) || splitBlobId.equals(otherBlobId)) {
                continue; // 3.2
            } else {
                confilct = true;
                dC(stage, headBlobs, otherBlobs, fileName);
            }
        }

        for (String fileName : otherBlobs.keySet()) {
            String otherBlobId = otherBlobs.getOrDefault(fileName, "");
            String splitBlobId = splitBlobs.getOrDefault(fileName, "");
            String headBlobId = headBlobs.get(fileName);

            if (splitBlobId.equals(headBlobId)) {
                if (!headBlobs.containsKey(fileName)) { // 7
                    continue;
                } else if (!headBlobId.equals(otherBlobId)) { // 2
                    continue;
                }
            }
            if (!headBlobs.containsKey(fileName) && splitBlobId.isEmpty()) {
                checkout(otherBr.getLastCommit(), fileName); // 5
                continue;
            }
        }
        if (confilct) {
            System.out.println("Encountered a merge conflict.");
        }
        String msg = "Merged " + branchName + " into " + branch + ".";
        TreeMap<String, String> cmtMap = Commit.mergeBlobs(headCmt, stage);
        Commit cmt = new Commit(msg, head, otherBr.getLastCommit(), cmtMap);
        head = cmt.write();
        stage.clear();
        Branch br = Branch.read(branch);
        br.updateLastCommit(head);
        br.write();
        save();
    }

    private void dC(StagingArea stage, TreeMap<String, String> headBlobs, TreeMap<String, String> otherBlobs, String fileName) {
        byte[] file1 = Blob.getBlobByte(headBlobs.get(fileName));
        String content1 = new String(file1, StandardCharsets.UTF_8);
        String content2;
        if (!otherBlobs.containsKey(fileName)) {
            content2 = "";
        } else {
            byte[] file2 = Blob.getBlobByte(otherBlobs.get(fileName));
            content2 = new String(file2, StandardCharsets.UTF_8);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<<<<<<< HEAD\n");
        sb.append(content1);
        sb.append("=======\n");
        sb.append(content2);
        sb.append(">>>>>>>\n");
        File conFile = new File(fileName);
        Utils.writeContents(conFile, sb.toString());
        Blob conBlob = new Blob(conFile);
        stage.add(fileName, conBlob.write(), head);
    }

    private String checkFailCases(String branchName, StagingArea stage, Branch otherBr) {
        if (!stage.isEmpty()) {
            errorPrint("You have uncommitted changes.");
        }
        if (Branch.read(branchName) == null) {
            errorPrint("A branch with that name does not exist.");
        }
        if (otherBr == Branch.read(branchName)) {
            errorPrint("Cannot merge a branch with itself.");
        }
        String otherLastCmt = otherBr.getLastCommit();
        String splitId = lca(head, otherLastCmt);
        if (splitId.equals(otherLastCmt)) {
            errorPrint("Given branch is an ancestor of the current branch.");
        }
        if (splitId.equals(head)) {
            checkout(branchName);
            errorPrint("Current branch fast-forwarded.");
        }
        return splitId;
    }

    private String lca(String cmt1, String cmt2) {
        // find the latest common ancestor
        List<String> cmt1Files = Utils.plainFilenamesIn(cmt1);
        while (!cmt1.isEmpty()) {
            cmt1Files.add(cmt1);
            Commit tmp = Commit.read(cmt1);
            cmt1 = tmp.getParent();
        }
        while (!cmt2.isEmpty()) {
            if (cmt1Files.contains(cmt2)) {
                return cmt2;
            }
            Commit tmp = Commit.read(cmt2);
            cmt2 = tmp.getParent();
        }
        return "";
    }

    // save repository status to disk.
    // bug: this function need the Repository() to creat REPO file,
    // otherwise it will cause NullPointer.
    public void save() {
        File repo = join(GITLET_DIR, "REPO");
        if (!repo.exists()) {
            try {
                repo.createNewFile();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
        Utils.writeObject(repo, this);
    }


    public static boolean isRepo() {
        return GITLET_DIR.exists();
    }

}
