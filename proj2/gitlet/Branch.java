package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class Branch implements Serializable {
    public static final File BRANCH_FILE = Utils.join(Repository.GITLET_DIR, "branches");
    private String branchName;
    private String lastCommit;

    public Branch(String brName, String lastCmt) {
        this.branchName = brName;
        this.lastCommit = lastCmt;
    }

    public void updateLastCommit(String cmt) {
        this.lastCommit = cmt;
    }

    public String getLastCommit() {
        return lastCommit;
    }

    public static void setCommitId(String filename, String commitId) {
        File file = Utils.join(BRANCH_FILE, filename);
        Utils.writeContents(file, commitId);
    }

    public static String getCommitId(String filename) {
        // 用fileName来得到提交Id
        File file = Utils.join(BRANCH_FILE, filename);
        if (file.exists()) {
            return Utils.readContentsAsString(file);
        } else {
            return null;
        }
    }

    public void write() {
        if (!BRANCH_FILE.exists()) {
            BRANCH_FILE.mkdir();
        }
        File branchFile = Utils.join(BRANCH_FILE, branchName);
        if (!branchFile.exists()) {
            try {
                branchFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.writeObject(branchFile, this);
    }

    public static Branch read(String filename) {
        File branchFile = Utils.join(BRANCH_FILE, filename);
        if (!branchFile.exists()) {
            return null;
        }
        return Utils.readObject(branchFile, Branch.class);
    }

    public static void remove(String branchName) {
        File file = Utils.join(BRANCH_FILE, branchName);
        if (!file.isDirectory() && file.exists()) {
            file.delete();
        }
    }

    public static List<String> allBranches() {
        return Utils.plainFilenamesIn(BRANCH_FILE);
    }
}
