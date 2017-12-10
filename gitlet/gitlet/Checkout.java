package gitlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static gitlet.Utils.commitFinder;
import static gitlet.Utils.writeContents;
import static gitlet.Utils.restrictedDelete;
import static gitlet.Utils.readContentsAsString;


/**
 * @author Pancham Yadav
 */
public class Checkout {

    /** Case A.
     * @param filename  the file being checked out*/
    public Checkout(String filename) {
        File headCommitFile = new File(Utils.HEAD_PATH);
        Commit headCommit = commitFinder(readContentsAsString(headCommitFile));
        if (!headCommit.getBlobs().keySet().contains(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        } else {
            File modified = new File(filename);
            String filePath = headCommit.getBlobs().get(filename);
            File blobContent =
                    new File(Utils.BLOBS_PATH
                            + Utils.SEPARATOR
                            + filePath);
            writeContents(modified, readContentsAsString(blobContent));
        }
    }
    /**Case C.
     * @param trigger triggers the function
     * @param  branchName the given branch */
    public Checkout(boolean trigger, String branchName) {
        if (!new File(".gitlet").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (!new File(Utils.BRANCH_PATH + Utils.SEPARATOR
                + branchName).exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        Commit headCommit = commitFinder(readContentsAsString(
                new File(Utils.HEAD_PATH)));
        Commit branchCommit = commitFinder(readContentsAsString(
                new File(Utils.BRANCH_PATH + Utils.SEPARATOR + branchName)));
        if (branchName.equals(
                readContentsAsString
                (new File(Utils.CURRENT_BRANCH_PATH)))) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        List<String> listOfFiles = new ArrayList<>();
        for (File f : new File(".").listFiles()) {
            listOfFiles.add(f.getName());
        }
        for (String name : listOfFiles) {
            if (branchCommit.getBlobs().containsKey(name)) {
                if (!headCommit.getBlobs().containsKey(name)) {
                    System.out.println("There is an untracked file in"
                            + " the way; delete it or add it first.");
                    System.exit(0);
                }
            }
        }
        for (String tracked : branchCommit.getBlobs().keySet()) {
            String secretContent = branchCommit.getBlobs().get(tracked);
            String path = Utils.BLOBS_PATH + Utils.SEPARATOR + secretContent;
            File f = new File(path);
            File overwrite = new File(tracked);
            writeContents(overwrite, readContentsAsString(f));
        }
        Set<String> setOfBranchAndHeadFiles = new HashSet<>();
        setOfBranchAndHeadFiles.addAll(branchCommit.getBlobs().keySet());
        setOfBranchAndHeadFiles.addAll(headCommit.getBlobs().keySet());
        for (String fileName : setOfBranchAndHeadFiles) {
            if (headCommit.getBlobs().keySet().contains(fileName)) {
                if (!branchCommit.getBlobs().keySet().contains(fileName)) {
                    restrictedDelete(fileName);
                }
            }
        }
        for (File file : new File(Utils.STAGE_PATH).listFiles()) {
            restrictedDelete(file);
        }

        writeContents(new File(Utils.CURRENT_BRANCH_PATH), branchName);
        writeContents(new File(Utils.HEAD_PATH),
                readContentsAsString(new File(Utils.BRANCH_PATH
                        + Utils.SEPARATOR + branchName)));
    }

    /** Case B.
     * @param identifiedCommit the desired commit
     * @param  filename the filename being checked out */
    public Checkout(Commit identifiedCommit, String filename) {
        try {
            secondCheckout(identifiedCommit, filename);
        } catch (FileNotFoundException fe) {
            System.out.println("File doesn't exist.");
            return;
        }
    }
    /**Case B.
     * @param commit input commit
     * @param  filename filename */
    private void secondCheckout(Commit commit, String filename)
            throws FileNotFoundException {
        if (!commit.getBlobs().keySet()
                .contains(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
            String inside = commit.getBlobs().get(filename);
            String path = Utils.BLOBS_PATH + Utils.SEPARATOR + inside;
            File newFile = new File(path);
            File needsToBeOverwritten = new File(filename);
            writeContents(needsToBeOverwritten, readContentsAsString(newFile));

        }
    }

}
