package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  Thanks CuiYuxin
 *  @author luohuang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        //  what if args is empty?
        emptyWarning(args);
        String firstArg = args[0];
        Repository repo = new Repository();
        switch(firstArg) {
            case "init":
                //  handle the `init` command
                validateNumArgs(args, 1);
                repo.init();
                break;
            case "add":
                //  handle the `add [filename]` command
                validateNumArgs(args, 2);
                repo.add(args[1]);
                break;
            case "commit":
                //  handle the `commit [message]` command
                validateNumArgs(args, 2);
                repo.commitCommand(args[1]);
                break;
            case "rm":
                //  handle the `rm [filename]` command
                validateNumArgs(args, 2);
                repo.rm(args[1]);
                break;
            case "log":
                validateNumArgs(args, 1);
                repo.log();
                break;
            case "global-log":
                validateNumArgs(args, 1);
                repo.globalLog();
                break;
            case "find":
                // handle the `find [commit message]` command
                validateNumArgs(args, 2);
                repo.find(args[1]);
                break;
            case "status":
                validateNumArgs(args, 1);
                repo.status();
                break;
            case "checkout":
                validateNumArgs(args, 2);
                if (args.length == 2) {
                    // java gitlet.Main checkout [branch name]
                    repo.checkout(args[1]);
                } else if (args.length == 3) {
                    //java gitlet.Main checkout [commit id] -- [file name]
                    repo.checkout("head", args[2]);
                } else if (args.length == 4) {
                    //                    java gitlet.Main checkout -- [file name]
                    repo.checkout(args[1], args[3]);
                   //  repo.checkout(args[0], args[3]); bug!!!!!
                }

                break;
            case "branch":
                validateNumArgs(args, 2);
                repo.creatBranch(args[1]);
                break;
            case "rm-branch":
                validateNumArgs(args, 2);
                repo.removeBranch(args[1]);
                break;
            case "reset":
                validateNumArgs(args, 2);
                repo.reset(args[1]);
                break;
            case "merge":
                validateNumArgs(args, 2);
                repo.merge(args[1]);
                break;
//            case "add-remote":
//                
//                validateNumArgs(args, 3);
//                Repository


            default:
                inExistence(); // instruct nonexistence
        }
    }

    public static void emptyWarning(String[] args) {
        if(args.length == 0) { //
            Utils.errorPrint("Please enter a command.");
        }
    }

    public static void validateNumArgs(String[] args, int index) {
        if (!args[0].equals("init") && !Repository.isRepo()) {
            Utils.errorPrint("Not in an initialized Gitlet directory.");
        }
        if (args[0].equals("checkout")) {
            if (args.length == 2) {
                return;
            } else if (args.length == 3) {
                if (!args[1].equals("--")) {
                    Utils.errorPrint("Incorrect operands.");
                }
            } else if (args.length == 4) {
                if (!args[2].equals("--")) {
                    Utils.errorPrint("Incorrect operands.");
                }
            } else {
                Utils.errorPrint("Incorrect operands.");
            }
            return;
        }
        if (args.length != index) { //
            Utils.errorPrint("Incorrect operands.");
        }
    }

    public static void inExistence() {
        Utils.errorPrint("No command with that name exists.");
    }
}
