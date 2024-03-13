package gitlet;



import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static gitlet.Utils.*;

//

/** Represents a gitlet repository.
 *
 *  does at a high level.
 *
 *  @author turbo
 */
public class Repository {
    /**
     *
     *      .gitlet
     *          files
     *          commit dic
     *              --commitId
     *                  commit
     *          blob dic
     *              --blobId
     *                  blob
     *          addition
     *          removal
     *          head
     *          branch
     *              --"master"
     *                  commitId
     *          /remote
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    public static File CWD = new File(System.getProperty("user.dir"));
    public static File GITLET_DIR = join(CWD, ".gitlet");
    public static File COMMIT_DIR = join(Repository.GITLET_DIR, "commit");
    public static File BLOB_DIR = join(Repository.GITLET_DIR, "blobs");
    public static File ADDSTAGING_FILE = join(Repository.GITLET_DIR, "addstaging");
    public static File REMSTAGING_FILE = join(Repository.GITLET_DIR, "remstaging");
    public static File HEAD_FILE = join(Repository.GITLET_DIR, "head");
    public static File BRANCH_DIR = join(Repository.GITLET_DIR, "branch");
    public static File REMOTE_DIR = join(Repository.GITLET_DIR, "remotename");
    public static File REMOTES_DIR = join(Repository.GITLET_DIR, "/remote");


    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        }
        makedirHelper();
        Commit initialCommit = new Commit();
        initialCommit.commitSave();
        Stage addition = new Stage();
        addition.saveAddition();
        Stage removal = new Stage();
        removal.saveRemoval();
        Branch.setCommitId("master", initialCommit.getOwnCommitId());
        HEAD.setHeadBranch("master");
    }

    public static void makedirHelper() {
        GITLET_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BLOB_DIR.mkdir();
        BRANCH_DIR.mkdir();
        REMOTE_DIR.mkdir();
        REMOTES_DIR.mkdir();
    }

    public static void add(String filename) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        Commit currentCommit = getCurrentCommit();
        File file = join(CWD, filename);
        if (!file.exists()) {
            System.out.println("File does not exist");
            System.exit(0);
        }
        Blob blob = new Blob(file);
        String blobId = blob.getBlobId();
        Stage addition = Stage.getAddition();
        Stage removal = Stage.getRemoval();
        HashMap<String, String> currentCommitBlob = currentCommit.getFilenameToBlobsId();
        if (currentCommitBlob.containsKey(filename) && currentCommitBlob.containsValue(blobId)
                && removal.isNewBlobId(blobId)) {
            return;
        }
        if (!removal.isNewBlobId(blobId)) {
            removal.deleteBlob(blob.getFilename());
            removal.saveRemoval();
        } else if (removal.isNewBlobId(blobId)) {
            if (!currentCommitBlob.containsKey(blobId) && addition.isNewBlobId(blobId)) {
                blob.blobSave();
                if (!addition.isNewFilename(blob)) {
                    addition.deleteBlob(blob);
                }
                addition.addBlob(blob);
                addition.saveAddition();
            }
        }
    }

    public static void commit(String message) {
        if (message.isEmpty()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        Stage addition = Stage.getAddition();
        Stage removal = Stage.getRemoval();
        if (addition.isEmpty() && removal.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        Commit newCommit = new Commit(message, getCurrentCommitId(), null);
        for (Map.Entry<String, String> temp:addition.getFilenamToBlobId().entrySet()) {
            String filename = temp.getKey();
            String blobId = temp.getValue();
            newCommit.getFilenameToBlobsId().put(filename, blobId);
        }
        for (Map.Entry<String, String> temp: removal.getFilenamToBlobId().entrySet()) {
            String filename = temp.getKey();
            newCommit.getFilenameToBlobsId().remove(filename);
        }
        addition.clear();
        removal.clear();
        addition.saveAddition();
        removal.saveRemoval();
        newCommit.commitSave();
        Branch.setCommitId(HEAD.getHeadBranch(), newCommit.getOwnCommitId());
    }
    public static void rm(String filename) {
        File file = join(CWD, filename);
        Stage addition = Stage.getAddition();
        Stage removal = Stage.getRemoval();
        Commit currentCommit = getCurrentCommit();
        if (!addition.isNewFilename(filename)) {
            addition.getFilenamToBlobId().remove(filename);
            addition.saveAddition();
            return;
        }
        if (currentCommit.getFilenameToBlobsId().containsKey(filename)) {
            String blobId = currentCommit.getFilenameToBlobsId().get(filename);
            Blob blob = readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
            removal.getFilenamToBlobId().put(filename, blob.getBlobId());
            join(CWD, filename).delete();
            removal.saveRemoval();
            return;
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }
    public static void log() {
        String currentCommitId = getCurrentCommitId();
        assert currentCommitId != null;
        while (currentCommitId != null) {
            Commit currentCommit = readObject(join(COMMIT_DIR, currentCommitId), Commit.class);
            currentCommit.logHelper();
            currentCommitId = currentCommit.getParentCommitId();
        }
    }

    public static void globallog() {
        List<String> commitIdList = plainFilenamesIn(COMMIT_DIR);
        assert commitIdList != null;
        for (String commitId:commitIdList) {
            Commit currentCommit = readObject(join(COMMIT_DIR, commitId), Commit.class);
            assert currentCommit != null;
            currentCommit.logHelper();
        }
    }

    public static void find(String message) {
        List<String> commitIdList = plainFilenamesIn(COMMIT_DIR);
        assert commitIdList != null;
        List<String> findList = new ArrayList<>();
        for (String commitId:commitIdList) {
            try {
                Commit commit = readObject(join(COMMIT_DIR, commitId), Commit.class);
                assert commit != null;
                if (commit.getMessage().equals(message)) {
                    findList.add(commitId);
                }
            } catch (Exception ignore) {
            }
        }
        if (findList.isEmpty()) {
            System.out.println("Found no commit with that message.");
        } else {
            for (String commitId : findList) {
                System.out.println(commitId);
            }
        }
    }

    public static void statusPlus() {
        System.out.println("=== Branches ===");
        List<String> branchList = plainFilenamesIn(BRANCH_DIR);
        assert branchList != null;
        String currentHead = HEAD.getHeadBranch();
        System.out.println("*" + currentHead);
        Collections.sort(branchList);
        for (String branchString : branchList) {
            if (branchString.equals(currentHead)) {
                continue;
            }
            System.out.println(branchString);
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        Stage addition = Stage.getAddition();
        if (addition.getFilenamToBlobId().isEmpty()) {
            System.out.println();
        } else {
            for (Blob blob : addition.getBlobList()) {
                System.out.println(blob.getFilename());
            }
            System.out.println();
        }
        System.out.println("=== Removed Files ===");
        Stage removal = Stage.getRemoval();
        if (removal.getFilenamToBlobId().isEmpty()) {
            System.out.println();
        } else {
            for (Blob blob : removal.getBlobList()) {
                System.out.println(blob.getFilename());
            }
            System.out.println();
        }
        List<String> CWDFilename = plainFilenamesIn(CWD);
        assert CWDFilename != null;
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> modifiedList = additionHelper(getCurrentCommit(), addition, CWDFilename, removal);
        for (String filename : modifiedList) {
            System.out.println(filename);
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        List<String> untrackedList = untrackedHelper(getCurrentCommit(), CWDFilename, addition);
        for (String filename : untrackedList) {
            System.out.println(filename);
        }
        System.out.println();
    }

    public static void status() {
        System.out.println("=== Branches ===");
        List<String> branchList = plainFilenamesIn(BRANCH_DIR);
        assert branchList != null;
        String currentHead = HEAD.getHeadBranch();
        System.out.println("*" + currentHead);
        Collections.sort(branchList);
        for (String branchString : branchList) {
            if (branchString.equals(currentHead)) {
                continue;
            }
            System.out.println(branchString);
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        Stage addition = Stage.getAddition();
        if (addition.getFilenamToBlobId().isEmpty()) {
            System.out.println();
        } else {
            for (Blob blob : addition.getBlobList()) {
                System.out.println(blob.getFilename());
            }
            System.out.println();
        }
        System.out.println("=== Removed Files ===");
        Stage removal = Stage.getRemoval();
        if (removal.getFilenamToBlobId().isEmpty()) {
            System.out.println();
        } else {
            for (Blob blob : removal.getBlobList()) {
                System.out.println(blob.getFilename());
            }
            System.out.println();
        }
        List<String> CWDFilename = plainFilenamesIn(CWD);
        assert CWDFilename != null;
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }
    private static List<String> additionHelper(Commit currentCommit, Stage addition, List<String> CWDFilanme, Stage removal) {
        List<String> commitFilename = currentCommit.getCommitFilenamesList();
        List<String> modifiedList = new ArrayList<>();
        for (String everyFilename : commitFilename) {
            String filenameBlobId = currentCommit.getBlobIdByFilename(everyFilename);
            Blob filenameBlob = readObject(join(Repository.BLOB_DIR, filenameBlobId), Blob.class);
            if (getCurrentCommit().getFilenameToBlobsId().containsKey(everyFilename)
                    && !addition.getFilenamToBlobId().containsKey(everyFilename)
                    && (!filenameBlob.getBlobId().equals(getCurrentCommit().getFilenameToBlobsId().get(everyFilename)))) {
                String modified = everyFilename + " (modified)";
                modifiedList.add(modified);
                continue;
            }
            if (addition.getFilenamToBlobId().containsKey(everyFilename)) {
                if (!filenameBlob.getBlobId().equals(addition.getFilenamToBlobId().get(everyFilename))) {
                    String modified = everyFilename + " (modified)";
                    modifiedList.add(modified);
                } else if (!CWDFilanme.contains(everyFilename)) {
                    String deleted = everyFilename + " (deleted)";
                    modifiedList.add(deleted);
                }
            }
            if (getCurrentCommit().getFilenameToBlobsId().containsKey(everyFilename) && (!removal.getFilenamToBlobId().containsKey(everyFilename))
                    && (!CWDFilanme.contains(everyFilename))) {
                String deleted = everyFilename + " (deleted)";
                modifiedList.add(deleted);
            }
        }
        Collections.sort(modifiedList);
        return modifiedList;
    }

    private static List<String> untrackedHelper(Commit commit, List<String> CWDFilename, Stage addition) {
        List<String> untrackedList = new ArrayList<>();
        for (String filename : CWDFilename) {
            if (addition.getFilenamToBlobId().containsKey(filename) && commit.getFilenameToBlobsId().containsKey(filename)) {
                untrackedList.add(filename);
            }
        }
        Collections.sort(untrackedList);
        return untrackedList;
    }

    public static void checkout(String filename) {
        Commit currentCommit = getCurrentCommit();
        List<String> currentCommitFilenameList = currentCommit.getCommitFilenamesList();
        if (currentCommitFilenameList.contains(filename)) {
            String blobId = currentCommit.getBlobIdByFilename(filename);
            Blob blob = readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
            File file = join(CWD, blob.getFilename());
            writeContents(file, new String(blob.getContents(), StandardCharsets.UTF_8));
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    public static void checkout(String commitId, String filename) {
        Commit targetCommit = Commit.getCommitByCommitId(commitId);
        if (targetCommit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        List<String> targetCommitFilenameList = targetCommit.getCommitFilenamesList();
        if (targetCommitFilenameList.contains(filename)) {
            String blobId = targetCommit.getBlobIdByFilename(filename);
            Blob blob = readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
            File file = join(CWD, blob.getFilename());
            writeContents(file, new String(blob.getContents(), StandardCharsets.UTF_8));
        } else {
            System.out.println("File does not exists in that commit");
            System.exit(0);
        }
    }

    public static void checkoutBranchName(String branchName) {
        List<String> branchList = plainFilenamesIn(BRANCH_DIR);
        if (!branchList.contains(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        String currentBranch = getCurrentBranch();
        if (currentBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
        }
        String commitId = Branch.getCommitId(branchName);
        Commit targetBranchCommit = readObject(join(Repository.COMMIT_DIR, commitId), Commit.class);
        List<String> currentCommitDependent = currentCommitDependentHelper(targetBranchCommit);
        List<String> targetCommitDependent = targetCommitDependentHelper(targetBranchCommit);
        List<String> bothCommitHave = bothCommitHaveHelper(targetBranchCommit);
        if (!currentCommitDependent.isEmpty()) {
            for (String filename : currentCommitDependent) {
                File file = join(CWD, filename);
                restrictedDelete(file);
            }
        }
        if (!bothCommitHave.isEmpty()) {
            for (String filename : bothCommitHave) {
                String blobId = targetBranchCommit.getBlobIdByFilename(filename);
                Blob blob = readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
                File file = join(CWD, blob.getFilename());
                writeContents(file, new String(blob.getContents(), StandardCharsets.UTF_8));
            }
        }
        if (!targetCommitDependent.isEmpty()) {
            for (String filename : targetCommitDependent) {
                File file = join(CWD, filename);
                if (file.exists()) {
                    System.out.println("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
                String blobId = targetBranchCommit.getBlobIdByFilename(filename);
                Blob blob = readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
                File fileTarget = join(CWD, blob.getFilename());
                writeContents(fileTarget, new String(blob.getContents(), StandardCharsets.UTF_8));
            }
        }
        Stage addition = Stage.getAddition();
        addition.clear();
        addition.saveAddition();
        Stage removal = Stage.getRemoval();
        removal.clear();
        removal.saveRemoval();
        HEAD.setHeadBranch(branchName);
    }

    public static void branch(String branch) {
        if (join(BRANCH_DIR, branch).exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String currentCommitId = getCurrentCommitId();
        Branch.setCommitId(branch, currentCommitId);
    }

    public static void rmbranch(String branch) {
        List<String> allBranch = plainFilenamesIn(BRANCH_DIR);
        assert allBranch != null;
        if (!allBranch.contains(branch)) {
            System.out.println("A branch with that name does not exists.");
            System.exit(0);
        }
        if (HEAD.getHeadBranch().equals(branch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File fileBranch = join(BRANCH_DIR, branch);
        if (!fileBranch.isDirectory()) {
            fileBranch.delete();
        }
    }

    public static void reset(String commitId) {
        Commit commit = Commit.getCommitByCommitId(commitId);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        List<String> currentCommitDependent = currentCommitDependentHelper(commit);
        List<String> targetCommitDependent = targetCommitDependentHelper(commit);
        List<String> bothCommitHave = bothCommitHaveHelper(commit);
        if (!currentCommitDependent.isEmpty()) {
            for (String filename : currentCommitDependent) {
                File file = join(CWD, filename);
                restrictedDelete(file);
            }
        }
        if (!bothCommitHave.isEmpty()) {
            for (String filename : bothCommitHave) {
                String blobId = commit.getBlobIdByFilename(filename);
                Blob blob = readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
                File file = join(CWD, blob.getFilename());
                writeContents(file, new String(blob.getContents(), StandardCharsets.UTF_8));
            }
        }
        if (!targetCommitDependent.isEmpty()) {
            for (String filename : targetCommitDependent) {
                File file = join(CWD, filename);
                if (file.exists()) {
                    System.out.println("There is an untracked file in the way; delete it, "
                           + "or add and commit it first.");
                    System.exit(0);
                }
                String blobId = commit.getBlobIdByFilename(filename);
                Blob blob = readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
                File fileTarget = join(CWD, blob.getFilename());
                writeContents(fileTarget, new String(blob.getContents(), StandardCharsets.UTF_8));
            }
        }
        Stage addition = Stage.getAddition();
        addition.clear();
        addition.saveAddition();
        Stage removal = Stage.getRemoval();
        removal.clear();
        removal.saveRemoval();
        String currentBranch = HEAD.getHeadBranch();
        Branch.setCommitId(currentBranch, commitId);
    }

    public static void merge(String mergeBranch) {
        Stage addition = Stage.getAddition();
        Stage removal = Stage.getRemoval();
        if (!addition.isEmpty() || !removal.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        String currentBranch = HEAD.getHeadBranch();
        List<String> branchList = plainFilenamesIn(BRANCH_DIR);
        if (!branchList.contains(mergeBranch)) {
            System.out.println("A branch with that names does not exists.");
            System.exit(0);
            return;
        }
        String mergeCommitId = readContentsAsString(join(BRANCH_DIR, mergeBranch));
        Commit mergeCommit = readObject(join(COMMIT_DIR, mergeCommitId), Commit.class);
        if (currentBranch.equals(mergeBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        String currentCommitId = readContentsAsString(join(BRANCH_DIR, currentBranch));
        Commit currentCommit = readObject(join(COMMIT_DIR, currentCommitId), Commit.class);
        String splitCommitId = findSplitCommitId(mergeCommit, currentCommit);
        Commit splitCommit = readObject(join(COMMIT_DIR, splitCommitId), Commit.class);
        if (splitCommitId.equals(mergeCommitId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitCommitId.equals(currentCommitId)) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranchName(mergeBranch);
        }
        String splitMessage = "Merged " + mergeBranch + " into " + currentBranch + ".";
        Commit newCommit = new Commit(splitMessage, currentCommitId, mergeCommitId);
        mergeHelper(splitCommit, newCommit, mergeCommit);
        newCommit.commitSave();
        addition.clear();
        addition.saveAddition();
        removal.clear();
        removal.saveRemoval();
        Branch.setCommitId(HEAD.getHeadBranch(), newCommit.getOwnCommitId());
    }

    private static void mergeHelper
            (Commit splitCommit, Commit newCommit, Commit mergeCommit) {
        HashSet<String> allFilesBlobId = new HashSet<>();
        allFilesBlobId.addAll(splitCommit.getFilenameToBlobsId().values());
        allFilesBlobId.addAll(mergeCommit.getFilenameToBlobsId().values());
        allFilesBlobId.addAll(newCommit.getFilenameToBlobsId().values());
        HashSet<String> splitCommitFileBlobId =
                new HashSet<>(splitCommit.getFilenameToBlobsId().values());
        HashSet<String> mergerCommitFileBlobId =
                new HashSet<>(mergeCommit.getFilenameToBlobsId().values());
        HashSet<String> newtCommitFileBlobId =
                new HashSet<>(newCommit.getFilenameToBlobsId().values());
        boolean conflict = false;
        for (String everyFileBlobId : allFilesBlobId) {
            if (splitCommitFileBlobId.contains(everyFileBlobId)) {
                Blob everyFileBlob = readObject(join(BLOB_DIR, everyFileBlobId), Blob.class);
                String everyFilename = everyFileBlob.getFilename();
                if (newtCommitFileBlobId.contains(everyFileBlobId)
                        && !mergerCommitFileBlobId.contains(everyFileBlobId)) {
                    if (Commit.whetherFilenameExists(everyFilename,mergeCommit)) {
                        File file = join(CWD, everyFilename);

                        overWriteFile(everyFilename, mergeCommit);
                        newCommit.getFilenameToBlobsId().remove(everyFilename);
                        newCommit.getFilenameToBlobsId().
                                put(everyFilename, mergeCommit.getFilenameToBlobsId().get(everyFilename));
                    } else if (!Commit.whetherFilenameExists(everyFilename,mergeCommit)) {        //情况6
                        File file = join(CWD, everyFilename);
                        file.delete();
                        if(Commit.whetherFilenameExists(everyFilename, newCommit)) {
                            newCommit.getFilenameToBlobsId().remove(everyFilename);
                        }
                    }
                }else if (!newtCommitFileBlobId.contains(everyFileBlobId)
                        && !mergerCommitFileBlobId.contains(everyFileBlobId)) {
                    if (Commit.whetherFilenameExists(everyFilename, newCommit)) {
                        if (Commit.whetherFilenameExists(everyFilename, mergeCommit)) {
                            String currentBlobId = newCommit.getFilenameToBlobsId().get(everyFilename);
                            Blob currentBlob = readObject(join(Repository.BLOB_DIR, currentBlobId), Blob.class);
                            String mergeBlobId = mergeCommit.getFilenameToBlobsId().get(everyFilename);       //情况3-1
                            Blob mergeBlob = readObject(join(Repository.BLOB_DIR, mergeBlobId), Blob.class);
                            if ((!currentBlob.getBlobId().equals(mergeBlob.getBlobId()))
                                    && (!currentBlobId.equals(everyFileBlobId) && (!everyFileBlobId.equals(mergeBlobId)))) {             //情况3-2 conflict undone
                                String currentContent = new String(currentBlob.getContents(), StandardCharsets.UTF_8);
                                String mergeContent = new String(mergeBlob.getContents(), StandardCharsets.UTF_8);
                                String conflictMessage = "<<<<<<< HEAD\n" + currentContent
                                        + "=======\n" + mergeContent + ">>>>>>>\n";
                                File file = join(CWD, everyFilename);
                                writeContents(file, conflictMessage);
                                conflict = true;
                            }
                        }else if (!Commit.whetherFilenameExists(everyFilename, mergeCommit)) {
                            String currentBlobId = newCommit.getFilenameToBlobsId().get(everyFilename);
                            Blob currentBlob = readObject(join(Repository.BLOB_DIR, currentBlobId), Blob.class);
                            String currentContent = new String(currentBlob.getContents(), StandardCharsets.UTF_8);
                            if(!currentBlobId.equals(everyFileBlobId)) {
                                String conflictMessage = "<<<<<<< HEAD\n" + currentContent
                                        + "=======\n" + "" + ">>>>>>>\n";
                                File file = join(CWD, everyFilename);
                                writeContents(file, conflictMessage);
                                conflict = true;
                            }
                        }
                    }else if (!Commit.whetherFilenameExists(everyFilename, newCommit)) {
                        if(Commit.whetherFilenameExists(everyFilename, mergeCommit)) {
                            String mergeBlobId = mergeCommit.getFilenameToBlobsId().get(everyFilename);
                            Blob mergeBlob = readObject(join(Repository.BLOB_DIR, mergeBlobId), Blob.class);
                            String mergeContent = new String(mergeBlob.getContents(), StandardCharsets.UTF_8);
                            if (!mergeBlobId.equals(everyFileBlobId)) {
                                String conflictMessage = "<<<<<<< HEAD\n" + ""
                                        + "=======\n" + mergeContent + ">>>>>>>\n";
                                File file = join(CWD, everyFilename);
                                writeContents(file, conflictMessage);
                                conflict = true;
                            }
                        }
                    }
                }
            } else if (!splitCommitFileBlobId.contains(everyFileBlobId)) {
                if (mergerCommitFileBlobId.contains(everyFileBlobId) &&
                        !newtCommitFileBlobId.contains(everyFileBlobId)) {
                    Blob everyFileBlob = readObject(join(BLOB_DIR, everyFileBlobId), Blob.class);
                    String everyFilename = everyFileBlob.getFilename();
                    if (Commit.whetherFilenameExists(everyFilename,newCommit)
                            && !splitCommit.getFilenameToBlobsId().containsKey(everyFilename)) {                //conflict
                        String currentBlobId = newCommit.getFilenameToBlobsId().get(everyFilename);
                        Blob currentBlob = readObject(join(Repository.BLOB_DIR, currentBlobId), Blob.class);
                        String mergeBlobId = mergeCommit.getFilenameToBlobsId().get(everyFilename);
                        Blob mergeBlob = readObject(join(Repository.BLOB_DIR, mergeBlobId), Blob.class);
                        if (!currentBlobId.equals(mergeBlobId)) {
                            String currentContent = new String(currentBlob.getContents(), StandardCharsets.UTF_8);
                            String mergeContent = new String(mergeBlob.getContents(), StandardCharsets.UTF_8);
                            String conflictMessage = "<<<<<<< HEAD\n" + currentContent
                                    + "=======\n" + mergeContent + ">>>>>>>\n";
                            File file = join(CWD, everyFilename);
                            writeContents(file, conflictMessage);
                            conflict= true;
                        }
                    }else if (!Commit.whetherFilenameExists(everyFilename,newCommit)) {
                        File file = join(CWD, everyFilename);
                        if (file.exists()) {
                            System.out.println("There is an untracked file in the way; delete it, " +
                                    "or add and commit it first.");
                        }
                        overWriteFile(everyFilename,mergeCommit);
                        newCommit.getFilenameToBlobsId().remove(everyFilename);
                        newCommit.getFilenameToBlobsId().put(everyFilename, mergeCommit.getFilenameToBlobsId().get(everyFilename));
                    }
                }
            }
        }
        if (conflict){
            System.out.println("Encountered a merge conflict.");
        }
    }
    //help function following
    public static void overWriteFile(String filename,Commit commit) {
        String blobId = commit.getFilenameToBlobsId().get(filename);
        Blob blob = readObject(join(Repository.BLOB_DIR, blobId), Blob.class);
        writeBlobToCWD(blob);
    }

    public static void writeBlobToCWD(Blob blob) {
        File filename = join(CWD, blob.getFilename());
        byte[] contents = blob.getContents();
        String content = new String(contents, StandardCharsets.UTF_8);
        writeContents(filename,content);
    }

    public static String findSplitCommitId(Commit merCommit, Commit currentCommit) {
        HashSet<String> merCommitId = new HashSet<>();
        Queue<String> bfs = new ArrayDeque<>();
        bfs.add(currentCommit.getOwnCommitId());
        while (!bfs.isEmpty()) {
            String commitId = bfs.remove();
            Commit commit = Commit.getCommitByCommitId(commitId);merCommitId.add(commitId);
            if (commit.getParentCommitId() != null) {
                bfs.add(commit.getParentCommitId());
            }
            if (commit.getSecondCommitId() != null){
                bfs.add(commit.getSecondCommitId());
            }
        }
        bfs.add(merCommit.getOwnCommitId());
        while (!bfs.isEmpty()) {
            String commtId = bfs.remove();
            Commit commit = Commit.getCommitByCommitId(commtId);
            if (merCommitId.contains(commtId)) {
                return commtId;
            }
            if (commit.getParentCommitId() != null) {
                bfs.add(commit.getParentCommitId());
            }
            if (commit.getSecondCommitId() != null) {
                bfs.add(commit.getSecondCommitId());
            }
        }
        return null;
    }

    public static List<String> currentCommitDependentHelper(Commit targetCommit) {
        List<String> targetCommitFiles = targetCommit.getCommitFilenamesList();
        List<String> currentCommitFiles = getCurrentCommit().getCommitFilenamesList();
        for (String file : targetCommitFiles) {
            if (currentCommitFiles.contains(file)) {
                currentCommitFiles.remove(file);
            }
        }
        return currentCommitFiles;
    }


    public static List<String> targetCommitDependentHelper(Commit targetCommit) {
        List<String> targetCommitFiles = targetCommit.getCommitFilenamesList();
        List<String> currentCommitFiles = getCurrentCommit().getCommitFilenamesList();
        for (String file : currentCommitFiles) {
            if (targetCommitFiles.contains(file)) {
                targetCommitFiles.remove(file);
            }
        }
        return targetCommitFiles;
    }

    public static List<String> bothCommitHaveHelper(Commit targetCommit) {
        List<String> targetCommitFiles = targetCommit.getCommitFilenamesList();
        List<String> currentCommitFile = getCurrentCommit().getCommitFilenamesList();
        List<String> bothCommitFile = new ArrayList<>();
        for (String file : targetCommitFiles) {
            if (currentCommitFile.contains(file)) {
                bothCommitFile.add(file);
            }
        }
        return bothCommitFile;
    }

    private static Commit getCurrentCommit() {
        String currentCommitId = getCurrentCommitId();
        File f = join(COMMIT_DIR, currentCommitId);
        return readObject(f,Commit.class);
    }

    private static String getCurrentBranch() {
        return HEAD.getHeadBranch();
    }

    private static String getCurrentCommitId() {
        String branch = getCurrentBranch();
        return Branch.getCommitId(branch);
    }
}
