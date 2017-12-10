package gitlet;

import java.io.File;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Pancham Yadav
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     * Implemented with a lot of assistance from TAs in office hours.
     * Strategy/design/algorithmic overview credits to Srividhya Shanker,
     * Urvi Guglani, Varda Shrivastava and Batool Naqvi.
     */
    public static void main(String... args) {

        if ((args.length == 0) || (args.equals(""))) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        if (args[0].equals("init") && new File(".gitlet").exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        }

        Gitlet interpreter = new Gitlet(args);

        while (true) {
            try {
                if  (!interpreter.statement()) {
                    break;
                }
            } catch (GitletException e) {
                System.exit(0);
            }
        }
    }
}
