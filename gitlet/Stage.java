package gitlet;


//DONE
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import static gitlet.Utils.*;

public class Stage implements Serializable {
    private final HashMap<String, String> filenamToBlobId = new HashMap<>();
    public HashMap<String, String> getFilenamToBlobId(){
        return this.filenamToBlobId;
    }
    public boolean isNewBlobId(Blob blob) {
        if (!filenamToBlobId.containsValue(blob.getBlobId())) {
            return true;
        }
        return false;
    }
    public boolean isNewBlobId(String blobId) {
        if (!filenamToBlobId.containsValue(blobId)) {
            return true;
        }
        return false;
    }
    public boolean isNewFilename(Blob blob) {
        if (!filenamToBlobId.containsKey(blob.getFilename())) {
            return true;
        }
        return false;
    }
    public boolean isNewFilename(String filename) {
        if (!filenamToBlobId.containsKey(filename)) {
            return true;
        }
        return false;
    }
    public void deleteBlob(Blob blob) {
        if (!isNewBlobId(blob)) {
            this.filenamToBlobId.remove(blob.getFilename());
        }
    }
    public void deleteBlob(String filename) {
        if (!isNewFilename(filename)) {
            this.filenamToBlobId.remove(filename);
        }
    }
    public void addBlob(Blob blob) {
        if (isNewBlobId(blob)) {
            this.filenamToBlobId.put(blob.getFilename(), blob.getBlobId());
        }
    }
    public void saveAddition() {
        writeObject(Repository.ADDSTAGING_FILE, this);
    }
    public void saveRemoval() {
        writeObject(Repository.REMSTAGING_FILE, this);
    }
    public static Stage getAddition() {
        return readObject(Repository.ADDSTAGING_FILE, Stage.class);
    }
    public static Stage getRemoval() {
        return readObject(Repository.REMSTAGING_FILE, Stage.class);
    }
    public void clear() {
        this.filenamToBlobId.clear();
    }
    public List<Blob> getBlobList() {
        Blob blob;
        List<Blob> blobList = new ArrayList<>();
        for (String id : this.filenamToBlobId.values()) {
            blob = getBlobByBlobId(id);
            blobList.add(blob);
        }
        return blobList;
    }
    private Blob getBlobByBlobId(String blobId) {
        return readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
    }
    public boolean isEmpty() {
        return this.filenamToBlobId.isEmpty();
    }
}