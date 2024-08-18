package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.join;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author luohuang
 */
public class Commit implements Serializable {
    /*
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    public static final File COMMITS_DIR = join(Repository.GITLET_DIR, "commits");
    private String message = "initial commit";
    private Date timestamp = new Date();
    private String firFarther = ""; // this need to be initial
    private String secFarther = "";
    // fileName -> blobId
    TreeMap<String, String> blobs = new TreeMap<>();

    public Commit() {
        message = "initial commit";
        timestamp.setTime(0);
        firFarther = "";
        secFarther = "";
    }

    public void initCommit() {
        this.message = "initial commit";
        this.firFarther = "";
        this.secFarther = "";
        this.timestamp.setTime(0);
    }

    public Commit(String msg, String fa1, String fa2, TreeMap<String, String> cmtMap) {
        this.message = msg;
        this.firFarther = fa1;
        this.secFarther = fa2;
        this.blobs = cmtMap;
        this.timestamp.getTime();
    }

    public String write() {
        if (!COMMITS_DIR.exists()) {
            COMMITS_DIR.mkdir();
        }
//        String hash = getHash();
        String hash = Utils.sha1(Utils.serialize(this));
        String sp = File.separator;
//        File commitFile = join(Repository.GITLET_DIR, "commits" + sp + hash);
        File commitFile = Utils.join(COMMITS_DIR, hash);
        if (!commitFile.exists()) {
            try {
                commitFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.writeObject(commitFile, this);
        return hash;
    }

    public String getHash() {
        return Utils.sha1((Object) Utils.serialize(this));
        //
    }

    public String getParent() {
        return firFarther;
    }

    public TreeMap<String, String> getBlobs() {
        return blobs;
    }

    public static Commit read(String hash) {
        if (hash.length() == 8) {
            List<String> list = Utils.plainFilenamesIn(COMMITS_DIR);
            for (String fileName : list) {
                // wrong: if (fileName.StartWith(hash))
//                if (fileName.substring(0, 8).equals(sha1)) {
                if (fileName.startsWith(hash)) {
                    hash = fileName;
                    break;
                }
            }
        }
        File cmtFile = join(COMMITS_DIR, hash);
        if (!cmtFile.exists()) {
            return null;
        }
        return Utils.readObject(cmtFile, Commit.class);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===\n");
        // SHA1
        sb.append("commit ").append(Utils.sha1(Utils.serialize(this))).append("\n");
        // Merge
        if (!secFarther.equals("")) {
            sb.append("Merge: ").append(firFarther.substring(0, 7)).append(" ");
            sb.append(secFarther.substring(0, 7)).append("\n");
        }
        // TimeStamp
        sb.append("Date: ");
        SimpleDateFormat format = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        sb.append(format.format(timestamp)).append("\n");
        // Message
        sb.append(message).append("\n");
        return sb.toString();
    }

    /** Merge blobs
     * @author luohuang
     */
    public static TreeMap<String, String> mergeBlobs(Commit curCommit, StagingArea curStage) {
        TreeMap<String, String> cmtBlobs = curCommit.getBlobs();
        // delete existed file in RemoveFile(Staging area)
        for (String fileName : curStage.getRemoveFile()) {
            cmtBlobs.remove(fileName);
        }
        for (Map.Entry<String, String> entry : curStage.getBlobMap().entrySet()) {
            cmtBlobs.put(entry.getKey(), entry.getValue());
        }
        return cmtBlobs;
    }

    /** Return all commit log
     * @author luo_huang
     */
    public static List<String> getAllCommitLog() {
        List<String> res = new ArrayList<>();
        List<String> arr = Utils.plainFilenamesIn(COMMITS_DIR);
        for (String fileName : arr) {
            Commit cmt = Commit.read(fileName);
            res.add(cmt.toString());
        }
        return res;
    }

    public static List<String> find(String msg) {
        List<String> res = new ArrayList<>();
        List<String> arr = Utils.plainFilenamesIn(COMMITS_DIR);
        for (String fileName : arr) {
            Commit cmt = read(fileName);
            if (cmt.message.equals(msg)) {
//                res.add(cmt.toString());   bugggggg
                res.add(Utils.sha1(Utils.serialize(cmt)));
            }
        }
        return res;

    }
}

