package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author turbo
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                //handle init method
                validateNumArgs(args,1 );
                Repository.init();
                break;
            case "add":
                validateGitlet();
                validateNumArgs(args,2);
                Repository.add(args[1]);
                break;
            case "commit":
                validateGitlet();
                validateNumArgs(args,2);
                Repository.commit(args[1]);
                break;
            case "rm":
                validateGitlet();
                validateNumArgs(args,2);
                Repository.rm(args[1]);
                break;
            case "log":
                validateGitlet();
                validateNumArgs(args,1);
                Repository.log();
                break;
            case "global-log":
                validateGitlet();
                validateNumArgs(args,1);
                Repository.globallog();
                break;
            case "find":
                validateGitlet();
                validateNumArgs(args,2);
                Repository.find(args[1]);
                break;
            case "status":
                validateGitlet();
                validateNumArgs(args,1);
                Repository.status();
                break;
            case "checkout":
                if(!Repository.GITLET_DIR.exists()){
                    System.out.println("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateGitlet();
                switch (args.length) {
                    case 3:
                        if(!args[1].equals("--")) {
                            System.out.println("Incorrect operands.");
                            System.exit(0);
                        }
                        Repository.checkout(args[2]);
                        break;
                    case 4:
                        if(!args[2].equals("--")) {
                            System.out.println("Incorrect operands.");
                            System.exit(0);
                        }
                        Repository.checkout(args[1],args[3]);
                        break;
                    case 2:
                        Repository.checkoutBranchName(args[1]);
                        break;
                    default:
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                }
                break;
            case "branch":
                validateGitlet();
                validateNumArgs(args,2);
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                validateGitlet();
                validateNumArgs(args,2);
                Repository.rmbranch(args[1]);
                break;
            case "reset":
                validateGitlet();
                validateNumArgs(args,2);
                Repository.reset(args[1]);
                break;
            case "merge":
                validateGitlet();
                validateNumArgs(args,2);
                Repository.merge(args[1]);
                break;
            default:
                validateGitlet();
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }
    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
    public static void validateGitlet() {
        if (!Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
