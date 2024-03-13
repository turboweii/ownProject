package gitlet;

import static gitlet.Utils.*;

public class HEAD {
    public static void setHeadBranch(String branch) {
        writeContents(Repository.HEAD_FILE, branch);
    }
    public static String getHeadBranch() {
        return readContentsAsString(Repository.HEAD_FILE);
    }
}
