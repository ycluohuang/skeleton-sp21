package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

// used in Commit.java
public class Blob implements Serializable {
    public static final File BLOB_DIR = Utils.join(Repository.GITLET_DIR, "blobs");

    byte[] blobByte;

    public Blob(File file) {
        BLOB_DIR.mkdir();
        blobByte = Utils.readContents(file);
    }

    public static byte[] getBlobByte(String blobId) {
        File file = Utils.join(BLOB_DIR, blobId);
        Blob b = Utils.readObject(file, Blob.class);
        return b.blobByte;
    }


    public String write() {
        //  wrong: String hs = getHash(); bug bug bug
        String hs = Utils.sha1(Utils.serialize(this));
        File newPath = Utils.join(BLOB_DIR, hs);
        if (!newPath.exists()) {
            try {
                newPath.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeObject(newPath, this);
        }
        return hs;
    }

}
