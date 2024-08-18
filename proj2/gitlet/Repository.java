package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
        if (GITLET_DIR.exists()) { // error in 2024/8/12/3:30 - 4:58
            errorPrint("A Gitlet version-control system already exists in the current directory.");
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
        Map<String, String> cmtBlobs = Commit.mergeBlobs(oldCmt, stagingArea);
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

    private void checkOverwritten(Commit otherCmt,  Commit curCmt) {
        // current directory files
        List<String> curDirFiles = Utils.plainFilenamesIn(CWD);
        // other branch's latest files
        Set<String> cmtFiles = otherCmt.getBlobs().keySet();
        for (String fileName : curDirFiles) {
            if (!curCmt.getBlobs().containsKey(fileName)) {
                if (cmtFiles.contains(fileName)) {
                    String fileID = otherCmt.getBlobs().get(fileName);
                    if (!fileID.equals(Utils.readContents(new File(fileName)))) {
                        System.out.print("There is an untracked file in the way;");
                        System.out.print(" delete it, or add and commit it first.\n");
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

    /**
     * @author luohuang
     */
    public void merge(String branchName) {
        StagingArea stage = new StagingArea();
        Branch otherBr = Branch.read(branchName);
        String splitId = checkFailCases(branchName, stage, otherBr);

        Commit splitCmt = Commit.read(splitId);
        Commit otherCmt = Commit.read(otherBr.getLastCommit());
        Commit headCmt = Commit.read(head);
        checkOverwritten(otherCmt, headCmt);

        Map<String, String> headBlobs = headCmt.getBlobs();
        Map<String, String> otherBlobs = otherCmt.getBlobs();
        Map<String, String> splitBlobs = splitCmt.getBlobs();
        boolean conflict = false;
        for (String file : headBlobs.keySet()) {

            if (splitBlobs.getOrDefault(file, "").equals(headBlobs.get(file))) {
                if (!otherBlobs.containsKey(file)) { // 6
                    stage.remove(file, head);
                    continue;
                } else if (!otherBlobs.getOrDefault(file, "").equals(headBlobs.get(file))) { // 1
                    checkout(otherBr.getLastCommit(), file);
                    stage.add(file, otherBlobs.getOrDefault(file, ""), head);
                    continue;
                }
            }
            if (!otherBlobs.containsKey(file) && !splitBlobs.containsKey(file)) { //4
                continue;
            }
            if (otherBlobs.getOrDefault(file, "").equals(headBlobs.get(file))) {  // 3.1
                continue;
            }
            String splitID2 = splitBlobs.getOrDefault(file, "");
            if (!otherBlobs.getOrDefault(file, "").equals(headBlobs.get(file))) { //3.2
                if (splitBlobs.getOrDefault(file, "").equals(headBlobs.get(file))) {
                    continue;
                } else if (splitID2.equals(otherBlobs.getOrDefault(file, ""))) {
                    continue;
                } else {
                    conflict = true;
                    dC(stage, headBlobs, otherBlobs, file);
                }
            }
        }

        for (String file : otherBlobs.keySet()) {
//   if (splitBlobs.getOrDefault(file, "").equals(headBlobs.getOrDefault(file, ""))) bugggg!!!
            if (splitBlobs.getOrDefault(file, "").equals(otherBlobs.getOrDefault(file, ""))) {
                if (!headBlobs.containsKey(file)) { // 7
                    continue;
                } else if (!headBlobs.getOrDefault(file, "").equals(otherBlobs.get(file))) { // 2
                    continue;
                }
            }
            if (!headBlobs.containsKey(file) && !splitBlobs.containsKey(file)) {
                checkout(otherBr.getLastCommit(), file); // 5
                stage.add(file, otherBlobs.get(file), head);
                continue;
            }
        }
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        String msg = "Merged " + branchName + " into " + this.branch + ".";
        Map<String, String> cmtMap = Commit.mergeBlobs(headCmt, stage);
        Commit cmt = new Commit(msg, head, otherBr.getLastCommit(), cmtMap);
        head = cmt.write();
        stage.clear();
        Branch br = Branch.read(branch);
        br.updateLastCommit(head);
        br.write();
        save();
    }


    /**
     * @author luohuang
     */
    private void dC(StagingArea st, Map<String, String> hB, Map<String, String> oB, String f) {
        byte[] f1 = Blob.getBlobByte(hB.getOrDefault(f, ""));
        String content1 = new String(f1, StandardCharsets.UTF_8);
        String content2;
        if (!oB.containsKey(f)) {
            content2 = "";
        } else {
            byte[] f2 = Blob.getBlobByte(oB.get(f));
            content2 = new String(f2, StandardCharsets.UTF_8);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<<<<<<< HEAD\n");
        sb.append(content1);
        sb.append("=======\n");
        sb.append(content2);
        sb.append(">>>>>>>\n");
        File conFile = new File(f);
        Utils.writeContents(conFile, sb.toString());
        Blob conBlob = new Blob(conFile);
        st.add(f, conBlob.write(), head);
    }
    
    /**
     * @author luohuang
     */
    private String checkFailCases(String branchName, StagingArea stage, Branch otherBr) {
        if (!stage.isEmpty()) {
            errorPrint("You have uncommitted changes.");
        }
        if (otherBr == null) {
            errorPrint("A branch with that name does not exist.");
        }
        if (branch.equals(branchName)) {
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

    /**
     * @author luohuang
     */
    private String lca(String cmt1, String cmt2) {
        // find the latest common ancestor
//        List<String> cmt1Files = Utils.plainFilenamesIn(cmt1);  bugggg!!!
        List<String> cmt1List = new ArrayList<>();
        while (!cmt1.equals("")) {
            cmt1List.add(cmt1);
            Commit tmp = Commit.read(cmt1);
            cmt1 = tmp.getParent();
        }
        while (!cmt2.equals("")) {
            if (cmt1List.contains(cmt2)) {
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
