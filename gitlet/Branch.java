package gitlet;
import java.io.File;
import static gitlet.Utils.*;

public class Branch {
    public static void setCommitId(String branch, String commitId) {
        File f = join(Repository.BRANCH_DIR, branch);
        writeContents(f, commitId);
    }
    public static String getCommitId(String branch) {
        File f = join(Repository.BRANCH_DIR, branch);
        if (!f.exists()) {
            return null;
        }
        return readContentsAsString(f);
    }
}
