package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class StagingArea implements Serializable {
    private static final File CWD = new File(System.getProperty("user.dir"));
    // filename -> blobId
    private TreeMap<String, String> blobMap = new TreeMap<>();
    // filename
    private HashSet<String> removeFile = new HashSet<>();

    public static final File STAGE_FILE = Utils.join(Repository.GITLET_DIR, "Stage");

    public StagingArea() {
        if (!STAGE_FILE.exists()) {
            try {
                STAGE_FILE.createNewFile();
                write();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
        blobMap = Utils.readObject(STAGE_FILE, StagingArea.class).blobMap;
        removeFile = Utils.readObject(STAGE_FILE, StagingArea.class).removeFile;
    }

    public static StagingArea getStagingArea() {
        return Utils.readObject(STAGE_FILE, StagingArea.class);
    }

    public TreeMap<String, String> getBlobMap() {
        return blobMap;
    }

    public HashSet<String> getRemoveFile() {
        return removeFile;
    }


    public boolean isEmpty() {
        return blobMap.isEmpty() && removeFile.isEmpty();
    }

    public void clear() {
        blobMap = new java.util.TreeMap<>();
        removeFile = new HashSet<>();
        this.write();
    }

    public void write() {
        Utils.writeObject(STAGE_FILE, this);
    }

    public void add(String fileName, String blobName, String head) { // blobName is hash
        Commit headCmt = Commit.read(head); // return the content of head file
        TreeMap<String, String> oldBlobs = headCmt.getBlobs();
        if (oldBlobs == null) {
            System.out.println("Stage add warning!");
            System.exit(0);
        }
        if (removeFile.contains(fileName)) {
            removeFile.remove(fileName);
        }
        if (oldBlobs.getOrDefault(fileName, "").equals(blobName)) {
            if (blobMap.containsKey(fileName)) {
                blobMap.remove(fileName);
            }
        } else {  // i mess up the else, and put it in the above if, it gets trouble.
            blobMap.put(fileName, blobName);
        }

    }

    public void remove(String fileName, String head) {
        boolean flag = true; // whether delete the fileName
        if (blobMap.containsKey(fileName)) {
            flag = false;
        }
        Commit headCmt = Commit.read(head);
        if (headCmt.getBlobs().containsKey(fileName)) {
            flag = false;
            removeFile.add(fileName);
            Utils.restrictedDelete(fileName);
        }
        if (flag) {
            Utils.errorPrint("No reason to remove the file.");
        }
    }

    public String[] getStagedFiles() { // used in status
        String[] files = blobMap.keySet().toArray(new String[0]);
        Arrays.sort(files);
        return files;
    }

    public String[] getUnstagedFiles(String head) { // used in status
        List<String> unstagedFiles = new ArrayList<>();
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        Commit headCmt = Commit.read(head);
        for (String file : allFiles) {
            File f = new File(file);
            Blob b = new Blob(f);
            String sha1 = Utils.sha1(Utils.serialize(b));
            if (headCmt.getBlobs().containsKey(file) && !blobMap.containsKey(file)) {
                if (!sha1.equals(headCmt.getBlobs().get(file))) { // file content had been modified
                    unstagedFiles.add(file + " (modified)");
                }
            } else if (blobMap.containsKey(file) && !sha1.equals(blobMap.get(file))) {
                unstagedFiles.add(file + " (modified)");
            }
        }
        for (String file : blobMap.keySet()) {
            if (!allFiles.contains(file)) {
                unstagedFiles.add(file + " (delete)");
            }
        }
        for (String file : headCmt.getBlobs().keySet()) {
            if (!allFiles.contains(file) && !removeFile.contains(file)) {
                if (unstagedFiles.contains(file + " (deleted)")) {
                    unstagedFiles.add(file + " (delete)");
                }
            }
        }
        String[] files = unstagedFiles.toArray(new String[0]);
        Arrays.sort(files);
        return files;
    }

    /**
     * @author luohuang
     */
    public String[] getUntrackedFiles(String head) {
        List<String> untrackedFiles = new ArrayList<>();
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        Commit headCmt = Commit.read(head);
        for (String file : allFiles) {
            if (!blobMap.containsKey(file)) {
                if (!headCmt.getBlobs().containsKey(file)) {
                    untrackedFiles.add(file);
                } else if (removeFile.contains(file)) {
                    untrackedFiles.add(file);
                }
            }
        }
        String[] files = untrackedFiles.toArray(new String[0]);
        Arrays.sort(files);
        return files;
    }

}


