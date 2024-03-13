package gitlet;


//DONE

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author turbo
 */
public class Commit implements Serializable {
    /**
     *
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private final String message;
    private final String ownCommitId;
    private final HashMap<String, String> filenameToBlobsId;             //key:filename,value:blobId
    private final Date timeStamp;
    private final String parentCommitId;
    private final String secondCommitId;
    public Commit() {
        this.message = "initial commit";
        this.timeStamp = new Date(0);
        this.parentCommitId = null;
        this.secondCommitId = null;
        this.filenameToBlobsId = new HashMap<>();
        this.ownCommitId = sha1(message, getTimeStampString(this.timeStamp),
                filenameToBlobsId.toString());
    }
    public Commit(String message, String parentCommitId, String secondCommitId) {
        this.message = message;
        this.timeStamp = new Date();
        this.parentCommitId = parentCommitId;
        this.secondCommitId = secondCommitId;
        Commit parent = getCommitByCommitId(this.parentCommitId);
        this.filenameToBlobsId = new HashMap<>();
        assert parent != null;
        filenameToBlobsId.putAll(parent.filenameToBlobsId);
        this.ownCommitId = sha1(message, getTimeStampString(this.timeStamp),
                filenameToBlobsId.toString());
    }
    public static Commit getCommitByCommitId(String commitId) {
        if (commitId.length() < 40) {
            List<String> commitIdList = plainFilenamesIn(Repository.COMMIT_DIR);
            assert commitIdList != null;
            for (String find : commitIdList) {
                if (find.startsWith(commitId)) {
                    commitId = find;
                    break;
                }
            }
        }
        File file = join(Repository.COMMIT_DIR, commitId);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Commit.class);
    }
    public HashMap<String, String> getFilenameToBlobsId() {
        return this.filenameToBlobsId;
    }
    public String getMessage() {
        return this.message;
    }
    public String getOwnCommitId() {
        return this.ownCommitId;
    }
    public String getParentCommitId() {
        return this.parentCommitId;
    }
    public String getBlobIdByFilename(String filename) {
        return this.filenameToBlobsId.get(filename);
    }
    public String getSecondCommitId() {
        return this.secondCommitId;
    }
    public static String getTimeStampString(Date timeStamp) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(timeStamp);
    }
    public void commitSave() {
        File file = join(Repository.COMMIT_DIR, this.ownCommitId);
        writeObject(file, this);
    }
    public void logHelper() {
        System.out.println("===");
        System.out.println("commit " + this.getOwnCommitId());
        System.out.println("Date: " + getTimeStampString(this.timeStamp));
        System.out.println(this.message + "\n");
    }
    public List<String> getCommitFilenamesList() {
        List<String> filenameList = new ArrayList<>();
        List<Blob> blobList = this.getBlobList();
        for (Blob blob : blobList) {
            filenameList.add(blob.getFilename());
        }
        return filenameList;
    }
    public List<String> getCommitBlobIdList() {
        List<String> blobIdList = new ArrayList<>();
        List<Blob> blobList = this.getBlobList();
        for (Blob blob : blobList) {
            blobIdList.add(blob.getBlobId());
        }
        return blobIdList;
    }
    private List<Blob> getBlobList() {
        List<Blob> blobList = new ArrayList<>();
        for (String blobId : this.filenameToBlobsId.values()) {
            Blob blob = getBlobByBlobId(blobId);
            blobList.add(blob);
        }
        return blobList;
    }
    private Blob getBlobByBlobId(String blobId) {
        return readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
    }
    public static boolean whetherFilenameExists(String filename, Commit commit) {
        return commit.getFilenameToBlobsId().containsKey(filename);
    }
}
