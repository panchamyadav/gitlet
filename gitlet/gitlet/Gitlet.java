package gitlet;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Consumer;
import java.io.File;

import static gitlet.Utils.*;

/**
 * @author Pancham Yadav
 */
public class Gitlet implements Serializable {

    /**
     * Input from the user.
     *
     * @param args - input args from the user.
     */
    Gitlet(String[] args) {
        this._input = args;
    }

    /**
     * Parse and execute one statement from the token stream.
     * Return true iff the command is something other than quit or exit.
     */
    boolean statement() {
        String command = _input[0];
        if (command == null) {
            return false;
        }
        if (command.equals("rm-branch")) {
            doRMBranch(command);
        } else if (command.equals("global-log")) {
            doGlobal(command);
        } else {
            try {
                Command.Type key = Command.Type.valueOf(command.toUpperCase());
                if (_commands.containsKey(key)) {
                    _commands.get(key).accept(command);
                }
            } catch (IllegalArgumentException np) {
                System.out.println("No command with that name exists.");
                System.exit(0);
            }
        }
        return false;
    }

    /**
     * Commits the staged changes.
     * @param incoming the trigger string
     * Strategy/discussion credits to TAs in office hours
     * and Srividhya Shanker and Urvi Guglani.
     **/
    void doCommit(String incoming) {
        if (!operandCheckOne()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        if (_input[1].equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        File stage = new File(STAGE_PATH);
        File remove = new File(REMOVE_FILES);
        String message = _input[1];
        if ((stage.listFiles().length == 0)
                && (remove.listFiles().length == 0)) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        File headFile = new File(HEAD_PATH);
        Commit head = commitFinder(readContentsAsString(headFile));
        Commit addedCommit = new Commit(message, new Date(), head.getID());
        HashMap<String, String> parents = new HashMap<>(head.getBlobs());
        addedCommit.setBlobs(parents);
        removeChecker(remove, headFile, addedCommit);
        stageChecker(stage, headFile, addedCommit);
        clearStage();
        clearRemove();
    }

    /**
     * Processes staged changes.
     * @param stage the stage directory
     * @param addedCommit the commit being added
     * @param headFile the head file
     **/
    private void stageChecker(File stage, File headFile, Commit addedCommit) {
        if (stage.listFiles().length != 0) {
            for (File fileToAdd : stage.listFiles()) {
                HashMap<String, String> trackedFiles = addedCommit.getBlobs();
                if (!trackedFiles.containsKey(fileToAdd.getName())) {
                    addBlobs(headFile, addedCommit, fileToAdd);

                } else if (trackedFiles
                        .containsKey(fileToAdd.getName())) {
                    if (!trackedFiles.get(fileToAdd.getName())
                            .equals(addedCommit.getID())) {
                        addBlobs(headFile, addedCommit, fileToAdd);
                    }
                }
            }
        }
    }
    /** Add functionality for the given blobs.
     * @param commit the commit to be added
     * @param  fileToAdd the file to be added
     * @param headFile the file contains head commit ID. **/
    private void addBlobs(File headFile,
                          Commit commit,
                          File fileToAdd) {
        String blobsUniqueID = sha1(fileToAdd.getName()
                + readContentsAsString(fileToAdd));
        commit.getBlobs().put(fileToAdd.getName(), blobsUniqueID);

        writeContents(fileToAdd,
                readContentsAsString(fileToAdd));
        File blobCreate =
                new File(BLOBS_PATH
                        + SEPARATOR + blobsUniqueID);
        writeContents(blobCreate,
                readContentsAsString(fileToAdd));
        String newCommitPath =
                COMMITS_PATH + SEPARATOR + commit.getID();
        File commitFile = new File(newCommitPath);
        writeObject(commitFile, commit);

        File branchPath = new File(BRANCH_PATH
                + SEPARATOR
                + readContentsAsString
                (new File(CURRENT_BRANCH_PATH)));
        writeContents(branchPath,
                commit.getID());

        writeContents(headFile, commit.getID());
    }

    /**
     * Processes staged changes.
     * @param remove the remove directory
     * @param commit the commit being added
     * @param headFile the head file
     * Strategy and implementation credits for
     *
     **/
    private void removeChecker(File remove,
                               File headFile,
                               Commit commit) {
        if (remove.listFiles().length != 0) {
            for (File fileToRemove : remove.listFiles()) {
                HashMap<String, String> trackedFiles = commit.getBlobs();
                trackedFiles.remove(fileToRemove.getName());
                if (!trackedFiles
                        .containsKey(fileToRemove.getName())) {
                    trackedFiles.remove(fileToRemove.getName());

                    File newBlob = new File(REMOVE_FILES
                            + SEPARATOR
                            + fileToRemove.getName());

                    writeContents(newBlob,
                            readContentsAsString(fileToRemove));
                    String commitPath =
                            COMMITS_PATH + SEPARATOR + commit.getID();

                    writeObject(new File(commitPath), commit);
                    writeContents(new File(BRANCH_PATH
                                    + SEPARATOR
                                    + readContentsAsString(
                                            new File(CURRENT_BRANCH_PATH))),
                            commit.getID());

                    writeContents(headFile, commit.getID());
                }
            }
        }
    }

    /**
     * Adds files to the stage.
     * @param trigger the trigger
     **/
    private void doAdd(String trigger) {
        if (operandCheckTwo()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        try {
            String fileToBeAdded = _input[1];
            adder(fileToBeAdded);
        } catch (IOException e) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
    }
    /**
     * Helper for add.
     * @param fileName the trigger
     **/
    public void adder(String fileName) throws IOException {
        File modified = new File(fileName);
        File inFile = new File(STAGE_PATH
                + SEPARATOR + fileName);
        File inRemove = new File(REMOVE_FILES
                + SEPARATOR + fileName);

        if (!modified.exists()) {
            System.out.println("File does not exist.");
        } else {
            if (inRemove.exists()) {
                inRemove.delete();
            }
            String head = readContentsAsString(new File(HEAD_PATH));
            Commit currentCommit = commitFinder(head);
            if (currentCommit == null) {
                writeContents(inFile, readContentsAsString(modified));
                return;
            }
            if (currentCommit.getBlobs().containsKey(fileName)) {
                String secret = sha1(fileName
                        + readContentsAsString(modified));
                if (currentCommit.getBlobs().get(fileName).equals(secret)) {
                    inFile.delete();
                } else {
                    writeContents(inFile, readContentsAsString(modified));
                }
            } else {
                writeContents(inFile, readContentsAsString(modified));
            }
        }
    }
    /**
     * The init command.
     *
     * @param trigger to trigger the function.
     **/
    private void doInit(String trigger) {
        if (operandCheckTwo()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        File gitletDir = new File(GITLET_PATH);
        if (!gitletDir.exists()) {
            gitletDir.mkdir();

            File stage = new File(STAGE_PATH);
            stage.mkdir();

            File commitsDir = new File(COMMITS_PATH);
            commitsDir.mkdir();

            DateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            Date date = new Date(0);
            sdf.format(date);

            String secret = sha1(date.toString() + "initial commit");
            String initCommitPath = (COMMITS_PATH + SEPARATOR + secret);
            File initCommitFile = new File(initCommitPath);
            Commit initialCommit = new Commit("initial commit", date, "");
            writeObject(initCommitFile, initialCommit);

            File branchDir = new File(BRANCH_PATH);
            branchDir.mkdir();

            File blobsDir = new File(BLOBS_PATH);
            blobsDir.mkdir();

            File removeFilesDir = new File(REMOVE_FILES);
            removeFilesDir.mkdir();
            removeFilesDir.mkdir();

            writeContents(new File(HEAD_PATH), initialCommit.getID());

            File masterFile =
                    new File(BRANCH_PATH + SEPARATOR + "master");
            writeContents(masterFile, initialCommit.getID());

            File currentBranch =
                    new File(BRANCH_PATH + SEPARATOR + "currentBranch");
            writeContents(currentBranch, "master");
        }
    }

    /**
     * The remove command.
     *
     * @param trigger to trigger the function.
     **/
    private void doRemove(String trigger) {
        String removeFileName = _input[1];
        File fileInStagingToBeRemoved =
                new File(STAGE_PATH
                        + SEPARATOR + removeFileName);
        Commit curr = commitFinder(readContentsAsString(new File(HEAD_PATH)));

        if (fileInStagingToBeRemoved.exists()) {
            fileInStagingToBeRemoved.delete();
        } else if (curr.getBlobs().keySet().contains(removeFileName)) {
            String contents =
                    readContentsAsString(
                            new File(BLOBS_PATH
                                    + SEPARATOR
                                    + curr.getBlobs().get(removeFileName)));
            writeContents(new File(
                    REMOVE_FILES
                            + SEPARATOR
                            + removeFileName),
                    contents);
            restrictedDelete(removeFileName);
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /**
     * The three checkout conditions - in the command.
     * java gitlet.Main checkout -- [file name]
     * java gitlet.Main checkout [commit id] -- [file name]
     * java gitlet.Main checkout [branch name]
     *
     * @param trigger unused.
     **/
    private void doCheckout(String trigger) {
        String identifier = _input[1];
        if (identifier.equals("--")) {
            if (operandCheckThree()) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            identifier = _input[2];
            new Checkout(identifier);
        } else if (_input.length == 4) {
            if (_input[2].equals("--")) {
                String commitID = "";
                String shortenedString = identifier;
                for (File f : new File(COMMITS_PATH).listFiles()) {
                    if (f.getName().substring(0,
                            shortenedString.length())
                            .equals(shortenedString)) {
                        commitID = f.getName();
                    }
                }
                if (!commitExists(commitID) || commitID.equals("")) {
                    System.out.println("No commit with that id exists.");
                    System.exit(0);
                }
                String filename = _input[3];
                String id = commitID;
                Commit target = commitFinder(id);
                new Checkout(target, filename);
            } else {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        } else {
            if (operandCheckTwo()) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            File branchFile =
                    new File(BRANCH_PATH
                            + SEPARATOR + identifier);
            if (!branchFile.exists()) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
            new Checkout(true, identifier);
        }
    }

    /**
     * The status command.
     *
     * @param trigger to trigger the function.
     **/
    private void doStatus(String trigger) {
        if (!new File(".gitlet").exists()) {
            System.out.println(
                    "Not in an initialized Gitlet directory.");
            return;
        }
        if (operandCheckOne()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        branchStatus();
        stagedStatus();
        removeStatus();
        otherStatus();
    }
    /**
     * The other status. Extra credit.
     *     **/
    private void otherStatus() {
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    /**
     * The removed files status.
     */
    private void removeStatus() {
        ArrayList<String> removedFilesList = new ArrayList<>();
        for (File f : new File(REMOVE_FILES).listFiles()) {
            removedFilesList.add(f.getName());
        }
        Collections.sort(removedFilesList);
        System.out.println("=== Removed Files ===");
        for (String removedFile : removedFilesList) {
            System.out.println(removedFile);
        }
        System.out.println();
    }
    /**
     * The staged files status.
     */
    private void stagedStatus() {
        File stage = new File(STAGE_PATH);
        ArrayList<String> stagedList = new ArrayList<>();
        for (File f : stage.listFiles()) {
            stagedList.add(f.getName());
        }
        Collections.sort(stagedList);
        System.out.println("=== Staged Files ===");
        for (String stagedFile : stagedList) {
            System.out.println(stagedFile);
        }
        System.out.println();
    }
    /**
     * The branches status.
     */
    private void branchStatus() {
        File currentBranchFile = new File(CURRENT_BRANCH_PATH);
        File branchesDir = new File(BRANCH_PATH);
        String currentBranch =
                readContentsAsString(currentBranchFile);
        ArrayList<String> branchList = new ArrayList<>();
        for (File each : branchesDir.listFiles()) {
            branchList.add(each.getName());
        }
        Collections.sort(branchList);
        System.out.println("=== Branches ===");
        for (String branch : branchList) {
            if (branch.equals(currentBranch)
                    && !branch.equals("currentBranch")) {
                System.out.println("*" + currentBranch);
            } else if (!branch.equals("currentBranch")) {
                System.out.println(branch);
            }
        }
        System.out.println();
    }

    /**
     * The log command.
     *
     * @param trigger to trigger the function.
     **/
    private void doLog(String trigger) {
        if (operandCheckOne()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        File headFile = new File(HEAD_PATH);
        Commit head = commitFinder(readContentsAsString(headFile));
        while (head != null) {
            System.out.println(head.toString());
            System.out.println();
            head = commitFinder(head.getParent());
        }
    }

    /**
     * @param trigger initiates function.
     */
    private void doFind(String trigger) {
        if (operandCheckTwo()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String message = _input[1];
        commitSearcher(message);
    }

    /**
     * Used in branch, reset, add, rm branch, find, remove.
     * @return true or false based on conditions met.
     **/
    private boolean operandCheckOne() {
        return (_input.length > 1);
    }

    /**
     * Used in init, status.
     * @return true or false based on conditions met.
     **/
    private boolean operandCheckTwo() {
        return (_input.length > 2);
    }

    /**
     * Used in branch, reset, add, rm branch, find, remove.
     * @return true or false based on conditions met.
     **/
    private boolean operandCheckThree() {
        return (_input.length > 3);
    }

    /**
     * Used in init, status.
     * @return true or false based on conditions met.
     **/
    private boolean operandCheckFour() {
        return (_input.length > 4);
    }

    /**
     * The branch command.
     *
     * @param trigger to trigger the function.
     **/
    private void doBranch(String trigger) {
        if (operandCheckTwo()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String branchName = _input[1];
        if (branchExists(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        } else {
            String headCommitID =
                    readContentsAsString(new File(HEAD_PATH));
            File branchFile =
                    new File(BRANCH_PATH + SEPARATOR + branchName);
            writeContents(branchFile, headCommitID);
        }
    }

    /**
     * The reset command.
     *
     * @param trigger to trigger the function.
     **/
    private void doReset(String trigger) {
        resetChecks();
        String shortenedString = _input[1];
        int part = shortenedString.length();
        String commitID = null;
        commitID = shortener(commitID, shortenedString, part);
        if (commitID == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        } else {
            Commit localHeadCommit = commitFinder(commitID);
            File globalHead = new File(HEAD_PATH);
            Commit headCommit =
                    commitFinder(readContentsAsString(globalHead));
            List<String> untrackedFiles = new ArrayList<>();
            File workingDir = new File(WORKING_DIR);
            for (File each : workingDir.listFiles()) {
                untrackedFiles.add(each.getName());
            }
            for (String untrackedName : untrackedFiles) {
                if (localHeadCommit.getBlobs().containsKey(untrackedName)) {
                    if (!headCommit.getBlobs().containsKey(untrackedName)) {
                        System.out.println("There is an untracked file in"
                                + " the way; delete it or add it first.");
                        System.exit(0);
                    }
                }
            }
            Commit commie = commitFinder(commitID);
            if (commie.getBlobs() != null) {
                for (String name
                        : commie.getBlobs().keySet()) {
                    String value = commie.getBlobs().get(name);
                    File testing =
                            new File(WORKING_DIR
                                    + SEPARATOR + name);
                    writeContents(testing,
                            readContentsAsString(
                                    new File(BLOBS_PATH
                                            + SEPARATOR + value)));
                }
            }
            if (workingDir.listFiles().length != 0) {
                for (String workingFile
                        : plainFilenamesIn(workingDir)) {
                    if (!commie.getBlobs().keySet()
                            .contains(workingFile)) {
                        restrictedDelete(workingFile);
                    }
                }
            }
        }
        writeContents(new File(BRANCH_PATH + SEPARATOR
                + readContentsAsString(
                        new File(CURRENT_BRANCH_PATH))), commitID);
        writeContents(new File(HEAD_PATH), commitID);
        clearStage();
    }

    /**Test for bad resets. */
    private void resetChecks() {
        if (operandCheckTwo()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (!new File(".gitlet").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    /** Returns the actual ID of the shortened commit.
     * @param actualCommitID the actual commit ID
     * @param extent the length.
     * @param shortened the shortened ID.
     * Idea credits to Srividhya Shanker. **/
    private String shortener(String actualCommitID,
                             String shortened,
                             int extent) {
        int shortLimiter = 4 * (5 + 5);
        if (shortened.length() < shortLimiter
                || extent > 6) {
            for (File eachCommitFine
                    : new File(COMMITS_PATH).listFiles()) {
                if (eachCommitFine.getName()
                        .substring(0, extent).equals(shortened)) {
                    actualCommitID = eachCommitFine.getName();
                }
            }
        }
        return actualCommitID;
    }

    /**
     * The merge command.
     *
     * @param trigger to trigger the function.
     *
     *  Implementation discussion collaboratively
     *  with Srividhya Shanker, Urvi Guglani, Varda
     *  Srivastava and Batool Naqvi.*/
    private void doMerge(String trigger) {
        if (operandCheckTwo()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String givenBranchName = _input[1];
        doMergeTwo(givenBranchName);
    }

    /**
     * The merge command for branches.
     *
     * @param given to trigger the function.
     **/
    private void doMergeTwo(String given) {
        File givenBranchFile =
                new File(BRANCH_PATH + SEPARATOR + given);
        if (!(givenBranchFile).exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        boolean conflict = false;
        File currentBranchNameFile = new File(CURRENT_BRANCH_PATH);
        File currentBranchFile =
                new File(BRANCH_PATH
                        + SEPARATOR
                        + readContentsAsString(currentBranchNameFile));
        Commit currentBranch =
                commitFinder(readContentsAsString(currentBranchFile));
        Commit givenBranch =
                commitFinder(readContentsAsString(givenBranchFile));

        badMergeChecks(given, currentBranchNameFile);
        getUntracked(currentBranch, givenBranch);

        Commit splitpoint =
                findSplitPoint(given,
                        readContentsAsString(
                                currentBranchNameFile));

        mergeErrors(given,
                givenBranchFile, currentBranch, givenBranch, splitpoint);

        HashMap<String, String> givenBranchValues = givenBranch.getBlobs();
        HashMap<String, String> splitPointValues = splitpoint.getBlobs();
        HashMap<String, String> currentBranchValues = currentBranch.getBlobs();

        conflict = goThroughSplitPointFiles(conflict,
                currentBranch, givenBranch, splitpoint,
                givenBranchValues, splitPointValues, currentBranchValues);

        conflict = goThroughCurrentBranchFiles(conflict,
                givenBranch, splitpoint,
                givenBranchValues, currentBranchValues);

        goThroughGivenBranchFiles(givenBranch,
                givenBranchValues, splitPointValues, currentBranchValues);

        String currenBranchName =
                readContentsAsString(new File(CURRENT_BRANCH_PATH));
        String message =
                "Merged "
                + given
                + " into "
                + currenBranchName
                + ".";

        specialCommitMerge(message, given, conflict);

        mergeConflictPrint(conflict, "Encountered a merge conflict.");
    }

    /** Error checking for merge. Helper.
     * @param currentBranch the current branch file
     * @param splitpoint the split point
     * @param given the given branch name.
     * @param givenBranch the given branch file.
     * @param givenBranchFile **/
    private void mergeErrors(String given,
                             File givenBranchFile,
                             Commit currentBranch,
                             Commit givenBranch,
                             Commit splitpoint) {
        if (splitpoint.getID().equals(givenBranch.getID())) {
            System.out.println(
                    "Given branch is an ancestor of the current branch."
            );
            System.exit(0);
        }

        if (splitpoint.getID().equals(currentBranch.getID())) {
            System.out.println("Current branch fast-forwarded.");
            writeContents(new File(currentBranch.getID()), given);
            writeContents(new File(HEAD_PATH),
                    readContentsAsString(givenBranchFile));
            System.exit(0);
        }
    }

    /** Will print if there's a merge conflict.
     * @param conflict if a conflict exists.
     * @param x the message to be printed. Standard for all.**/
    private void mergeConflictPrint(boolean conflict, String x) {
        if (conflict) {
            System.out.println(x);
            System.exit(0);
        }
    }

    /**
     * Helper for merge.
     * @param given the name of the given merge.
     * @param currentBranchNameFile the name of the current branch
     * **/
    private void badMergeChecks(String given,
                                File currentBranchNameFile) {
        if (given.equals(readContentsAsString(currentBranchNameFile))) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        if (new File(STAGE_PATH).listFiles().length != 0
                || new File(REMOVE_FILES).listFiles().length != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
    }

    /**
     * Helper for merge.
     * @param givenBranchValues the values
     * @param splitPointValues split point hashmap
     * @param currentBranchValues current branch's values
     * @param givenBranch the given branch
     **/
    private void goThroughGivenBranchFiles(Commit givenBranch,
                                           HashMap<String, String>
                                                   givenBranchValues,
                                           HashMap<String, String>
                                                   splitPointValues,
                                           HashMap<String, String>
                                                   currentBranchValues) {
        for (String givenKey : givenBranchValues.keySet()) {
            boolean sIsThere = splitPointValues.containsKey(givenKey);
            boolean cIsThere = currentBranchValues.containsKey(givenKey);
            if (!cIsThere && !sIsThere) {
                mergeHelperA(givenKey, givenBranch);
            }
        }
    }

    /**
     * Helper for merge.
     * @param givenBranchValues split point hashmap
     * @param currentBranchValues current branch's values
     * @param givenBranch the given branch
     * @param conflict if there's a conflict
     * @return if there's a conflict.
     * @param splitpoint the splitting point
     **/
    private boolean goThroughCurrentBranchFiles(boolean conflict,
                                                Commit givenBranch,
                                                Commit splitpoint,
                                                HashMap<String, String>
                                                        givenBranchValues,
                                                HashMap<String, String>
                                                        currentBranchValues) {
        for (String currKey : currentBranchValues.keySet()) {
            boolean gIsThere = givenBranch.getBlobs().containsKey(currKey);
            if (gIsThere && !splitpoint.getBlobs().containsKey(currKey)
                    && !givenBranch.getBlobs()
                    .get(currKey)
                    .equals(currentBranchValues.get(currKey))) {
                mergeHelperC(currKey, currentBranchValues.get(currKey),
                        givenBranchValues.get(currKey));
                conflict = true;
            }
        }
        return conflict;
    }

    /**
     * Helper for merge.
     * @param givenBranchValues the values
     * @param splitPointValues split point hashmap
     * @param currentBranchValues current branch's values
     * @param givenBranch the given branch
     * @param splitpoint the actual splitpoint
     *                   @param conflict if there's a conflict.
     *                   @param currentBranch the current branch
     * @return t/f.
     * */
    private boolean goThroughSplitPointFiles(boolean conflict,
                                             Commit currentBranch,
                                             Commit givenBranch,
                                             Commit splitpoint,
                                             HashMap<String, String>
                                                     givenBranchValues,
                                             HashMap<String, String>
                                                     splitPointValues,
                                             HashMap<String, String>
                                                     currentBranchValues) {
        for (String filename : splitPointValues.keySet()) {
            boolean existsInGiven =
                    givenBranchValues.containsKey(filename);
            boolean existsInCurrent =
                    currentBranchValues.containsKey(filename);

            if (existsInGiven && existsInCurrent
                    && !splitPointValues
                    .get(filename).equals(givenBranchValues.get(filename))
                    && splitPointValues
                    .get(filename).equals(currentBranchValues.get(filename))) {
                mergeHelperA(filename, givenBranch);
            } else if (!existsInGiven && existsInCurrent
                    && splitPointValues
                    .get(filename).equals(currentBranchValues.get(filename))) {
                mergeHelperH(filename);
            }
            if (existsInGiven && !existsInCurrent
                    && splitPointValues
                    .get(filename).equals(givenBranchValues.get(filename))) {
                mergeHelperH(filename);
            }
            if (existsInGiven && existsInCurrent
                    && !splitPointValues
                    .get(filename).equals(currentBranchValues.get(filename))
                    && !splitPointValues
                    .get(filename).equals(givenBranchValues.get(filename))
                    && !currentBranchValues
                    .get(filename).equals(givenBranchValues.get(filename))) {
                mergeHelperC(filename, currentBranchValues
                        .get(filename), givenBranchValues.get(filename));
                conflict = true;
            }
            if ((!existsInGiven && existsInCurrent
                    && (!splitPointValues
                    .get(filename).equals(currentBranchValues.get(filename))))
                    || (!existsInCurrent && existsInGiven
                    && (!splitPointValues
                    .get(filename).equals(givenBranchValues.get(filename))))) {
                if (existsInGiven) {
                    mergeHelperC(filename, "",
                            givenBranchValues.get(filename));
                } else {
                    mergeHelperC(filename,
                            currentBranchValues.get(filename), "");
                }
                conflict = true;
            }
        }
        return conflict;
    }

    /**
     * Helper for merge.
     * @param currentBranch the current branch commit
     * @param givenBranch the given branch
     * Discussion credits to Varda Srivastava and Batool
     * Naqvi.
     **/
    private void getUntracked(Commit currentBranch, Commit givenBranch) {
        List<String> listOfFiles = new ArrayList<>();
        for (File f : new File(".").listFiles()) {
            listOfFiles.add(f.getName());
        }

        Set<String> setOfBranchAndHeadFiles = new HashSet<>();
        for (String name : givenBranch.getBlobs().keySet()) {
            setOfBranchAndHeadFiles.add(name);
        }
        for (String name : currentBranch.getBlobs().keySet()) {
            setOfBranchAndHeadFiles.add(name);
        }

        for (String name : givenBranch.getBlobs().keySet()) {
            if (listOfFiles.contains(name)) {
                if (!currentBranch.getBlobs().containsKey(name)) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it or add it first.");
                    System.exit(0);
                }
            }
        }
    }

    /**
     * Helper for merge.
     * @param filename the current branch name
     * @param givenBranch the given branch
     **/
    public void mergeHelperA(String filename, Commit givenBranch) {
        File workingDirFile = new File("." + SEPARATOR + filename);
        File savedFile = (new File(
                BLOBS_PATH
                        + SEPARATOR
                        + givenBranch.getBlobs().get(filename)));
        writeContents(workingDirFile,
                readContentsAsString(savedFile));
        writeContents(new File(STAGE_PATH + SEPARATOR + filename),
                readContentsAsString(new File("." + SEPARATOR + filename)));

    }

    /**
     * Helper for merge.
     * @param key the current branch name
     **/
    private void mergeHelperH(String key) {
        writeContents(new File(
                REMOVE_FILES + SEPARATOR + key), key);
        restrictedDelete(key);
    }

    /**
     * Helper for merge.
     * @param key the current branch name
     * @param currentContentsUniqueSha the ID of the blob contents
     * @param givenContentsUniqueSha the given contents SHA.
     **/
    private void mergeHelperC(String key,
                     String currentContentsUniqueSha,
                     String givenContentsUniqueSha) {
        String replace;
        if (currentContentsUniqueSha.equals("")) {
            replace = "<<<<<<< HEAD" + "\n"
                    + "=======" + "\n"
                    + readContentsAsString(new File(
                            BLOBS_PATH
                                    + SEPARATOR
                                    + givenContentsUniqueSha))
                    + ">>>>>>>" + "\n";
        } else if (givenContentsUniqueSha.equals("")) {
            replace = "<<<<<<< HEAD" + "\n"
                    + readContentsAsString(
                            new File(BLOBS_PATH
                    + SEPARATOR + currentContentsUniqueSha))
                    + "=======" + "\n"
                    + ">>>>>>>" + "\n";
        } else {
            replace = "<<<<<<< HEAD" + "\n"
                    + readContentsAsString(new File(BLOBS_PATH
                    + SEPARATOR + currentContentsUniqueSha))
                    + "=======" + "\n"
                    + readContentsAsString(new File(BLOBS_PATH
                    + SEPARATOR + givenContentsUniqueSha))
                    + ">>>>>>>" + "\n";
        }
        writeContents(new File(key), replace);
    }

    /**
     * Helper for merge.
     * @param branchOne the first branch
     * @param branchTwo the second branch
     * @return split point commit
     * Discussion credits to Stephen Chu and Eli Lipsitz in office
     * hours. Implementation
     **/
    private Commit findSplitPoint(String branchOne, String branchTwo) {
        ArrayList<String> firstBranchIDs = new ArrayList<>();
        ArrayList<String> secondBranchIDs = new ArrayList<>();

        branchPathTracer(branchOne, firstBranchIDs);
        branchPathTracer(branchTwo, secondBranchIDs);

        List<String> common = new ArrayList<>(firstBranchIDs);
        common.retainAll(secondBranchIDs);
        String splitID = common.get(0);
        Commit splitPoint = commitFinder(splitID);
        return splitPoint;
    }

    /** Traces the branch path and adds to an array list.
     * @param branch the branch name.
     * @param branchIDs the given arraylist of IDs.
     *              */
    private void branchPathTracer(String branch, ArrayList<String> branchIDs) {
        Commit commit = commitFinder(readContentsAsString(
                new File(BRANCH_PATH + SEPARATOR + branch)));
        branchIDs.add(commit.getID());
        while (commit != null
                && !commit.getParent().equals("")) {
            branchIDs.add(commit.getParent());
            commit = commitFinder(commit.getParent());
        }
    }

    /** Executes the special merge case commit.
     * @param conflict the trigger for the helper.
     * @param given the given branch.
     * @param message the message for the merge commit. **/
    private void specialCommitMerge(String message,
                                    String given,
                                    boolean conflict) {
        if (!new File(".gitlet").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
            return;
        }
        Date now = new Date();
        Commit newComm = new Commit(true,
                message,
                now,
                readContentsAsString(new File(HEAD_PATH)),
                readContentsAsString(new File(BRANCH_PATH
                        + SEPARATOR
                        + given)),
                new HashMap<>());

        Commit head = commitFinder(readContentsAsString(new File(HEAD_PATH)));

        newComm.setParent(head.getID());
        newComm.setBlobs(new HashMap<>(head.getBlobs()));

        specialMergeRemove(newComm);
        specialMergeAdder(newComm, head);
        clearRemove();
        clearStage();
    }
    /** Executes the special merge add commit.
     * @param newComm the trigger for the helper.
     * @param head the given branch.*/
    private void specialMergeAdder(Commit newComm, Commit head) {
        if (new File(STAGE_PATH).listFiles().length != 0) {
            for (File filesBeingAdded : new File(STAGE_PATH).listFiles()) {
                if (!newComm.getBlobs().containsKey(filesBeingAdded.getName())
                        || (newComm.getBlobs()
                        .containsKey(filesBeingAdded.getName())
                                && !newComm.getBlobs()
                        .get(filesBeingAdded.getName()).equals(
                                        (sha1(filesBeingAdded
                                                .getName()
                                                + readContentsAsString(
                                                        filesBeingAdded)))))) {

                    newComm.getBlobs().put(filesBeingAdded.getName(), (
                            sha1(filesBeingAdded.getName()
                                    + readContentsAsString(filesBeingAdded))));

                    writeContents(filesBeingAdded,
                            (readContentsAsString(filesBeingAdded)));

                    newComm.setParent(head.getID());

                    File newBlob = new File(
                            BLOBS_PATH
                                    + SEPARATOR
                                    + sha1(filesBeingAdded.getName()
                            + readContentsAsString(filesBeingAdded)));

                    writeContents(newBlob,
                            readContentsAsString(filesBeingAdded));
                    newComm.setID(sha1(newComm.getDate()
                            + newComm.getMessage()));

                    String newCommitPath = (COMMITS_PATH
                            + SEPARATOR + newComm.getID());
                    writeObject(new File(newCommitPath), (newComm));
                    writeContents(new File(
                            BRANCH_PATH + File.separator
                            + readContentsAsString(
                                    new File(CURRENT_BRANCH_PATH))),
                            newComm.getID());

                    writeContents(new File(HEAD_PATH), readContentsAsString(new
                            File(BRANCH_PATH
                            + SEPARATOR
                            + readContentsAsString(
                                    new File(CURRENT_BRANCH_PATH)))));
                }
            }
        }
    }
    /** Executes the special merge case commit.
     * @param newComm the commit to have removed files. **/
    private void specialMergeRemove(Commit newComm) {
        if (new File(REMOVE_FILES).listFiles().length != 0) {
            for (File fileToRemove : new File(REMOVE_FILES).listFiles()) {
                if (!newComm.getBlobs().containsKey(fileToRemove.getName())) {
                    String blobsSha = sha1(fileToRemove.getName()
                            + readContentsAsString(fileToRemove));

                    newComm.getBlobs().remove(
                            fileToRemove.getName(), blobsSha);

                    File newBlob = new File(
                            REMOVE_FILES
                                    + SEPARATOR
                                    + fileToRemove.getName());

                    writeContents(newBlob, readContentsAsString(fileToRemove));
                    String newCommitPath =
                            COMMITS_PATH
                                    + SEPARATOR
                                    + newComm.getID();

                    writeObject(new File(newCommitPath), newComm);
                    writeContents(new File(BRANCH_PATH
                                    + SEPARATOR
                                    + readContentsAsString(
                                            new File(CURRENT_BRANCH_PATH))),
                            readContentsAsString(new File(HEAD_PATH)));

                    writeContents(new File(HEAD_PATH),
                            readContentsAsString(new
                            File(BRANCH_PATH
                            + File.separator
                            + readContentsAsString(
                                    new File(CURRENT_BRANCH_PATH)))));
                }
            }
        }
    }

    /**
     * The rm-branch command.
     *
     * @param trigger to trigger the function.
     **/
    private void doRMBranch(String trigger) {
        if (operandCheckTwo()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String branchToRemove = _input[1];
        String latestBranch =
                readContentsAsString(new File(CURRENT_BRANCH_PATH));
        try {
            if (latestBranch.equals(branchToRemove)) {
                System.out.println("Cannot remove the current branch.");
                System.exit(0);
            } else if (!branchExists(branchToRemove)) {
                System.out.println("A branch with that name does not exist.");
                System.exit(0);
            }
            File rmBranch = new File(BRANCH_PATH + SEPARATOR + branchToRemove);
            rmBranch.delete();
        } catch (GitletException ge) {
            System.exit(0);
        }
    }

    /**
     * The global command.
     *
     * @param trigger to trigger the function.
     **/
    private void doGlobal(String trigger) {
        if (operandCheckTwo()) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        for (File each : new File(COMMITS_PATH).listFiles()) {
            Commit eachCommitEver = commitFinder(each.getName());
            System.out.println(eachCommitEver.toString());
            System.out.println();
        }
    }

    /**
     * The remote command.
     *
     * @param trigger to trigger the function.
     **/
    private void doRemote(String trigger) {

    }
    /**
     * The pull command.
     *
     * @param trigger to trigger the function.
     **/
    private void doPull(String trigger) {

    }
    /**
     * The push command.
     *
     * @param trigger to trigger the function.
     **/
    private void doPush(String trigger) {

    }

    /**
     * Input from the user.
     */
    private String[] _input;

    /**
     * Mapping of command types to methods that process them.
     * Inspired from 61B Project 2, Qirkat.
     */
    private final HashMap<Command.Type,
            Consumer<String>> _commands = new HashMap<>();

    {
        _commands.put(Command.Type.COMMIT, this::doCommit);
        _commands.put(Command.Type.ADD, this::doAdd);
        _commands.put(Command.Type.INIT, this::doInit);
        _commands.put(Command.Type.RM, this::doRemove);
        _commands.put(Command.Type.CHECKOUT, this::doCheckout);
        _commands.put(Command.Type.STATUS, this::doStatus);
        _commands.put(Command.Type.LOG, this::doLog);
        _commands.put(Command.Type.FIND, this::doFind);
        _commands.put(Command.Type.BRANCH, this::doBranch);
        _commands.put(Command.Type.RMBRANCH, this::doRMBranch);
        _commands.put(Command.Type.RESET, this::doReset);
        _commands.put(Command.Type.MERGE, this::doMerge);
        _commands.put(Command.Type.GLOBALLOG, this::doGlobal);
        _commands.put(Command.Type.PUSH, this::doPush);
        _commands.put(Command.Type.PULL, this::doPull);
        _commands.put(Command.Type.REMOTE, this::doRemote);
    }
}
