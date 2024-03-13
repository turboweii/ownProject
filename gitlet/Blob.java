package gitlet;

//DONE
/* Represents a blob class.
 *
 *  @author turbo
 */
import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.*;

public class Blob implements Serializable {

    private final byte[] contents;
    private final String blobId;
    private final String filename;
    private final File file;
    public Blob(File file) {
        this.contents = readContents(file);
        this.filename = file.getName();
        this.file = file;
        this.blobId = sha1(this.filename, this.contents);
    }
    public String getBlobId() {
        return this.blobId;
    }
    public String getFilename() {
        return this.filename;
    }
    public byte[] getContents() {
        return this.contents;
    }
    public void blobSave() {
        File f = join(Repository.BLOB_DIR, this.blobId);
        writeObject(f, this);
    }
}