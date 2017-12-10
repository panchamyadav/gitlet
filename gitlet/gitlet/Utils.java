package gitlet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;


/** Assorted utilities.
 *  @author P. N. Hilfinger & modified (added methods) by Pancham Yadav
 */
class Utils {
    /** All paths. */
    static final String SEPARATOR = File.separator;
    /** Gitlet path. */
    static final String GITLET_PATH = ".gitlet" + SEPARATOR;
    /** Remove files path. */
    static final String REMOVE_FILES = GITLET_PATH + ".rmFiles";
    /** Blobs path. */
    static final String BLOBS_PATH =  GITLET_PATH + ".blobs";
    /** Commit path. */
    static final String COMMITS_PATH =  GITLET_PATH + ".commits";
    /** Stage. */
    static final String STAGE_PATH =  GITLET_PATH + ".stage";
    /** Branches path. */
    static final String BRANCH_PATH =  GITLET_PATH + ".branches";
    /** Current head commit file. */
    static final String HEAD_PATH = GITLET_PATH + "HEAD";
    /** Current branch. */
    static final String CURRENT_BRANCH_PATH =
            BRANCH_PATH + SEPARATOR + "currentBranch";
    /** Current working directory path. */
    static final String WORKING_DIR = ".";

    /** Returns the SHA-1 hash of the concatenation of VALS, which may
     *  be any mixture of byte arrays and Strings. */
    static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    /** Returns the SHA-1 hash of the concatenation of the strings in
     *  VALS. */
    static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }

    /* FILE DELETION */

    /** Deletes FILE if it exists and is not a directory.  Returns true
     *  if FILE was deleted, and false otherwise.  Refuses to delete FILE
     *  and throws IllegalArgumentException unless the directory designated by
     *  FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(File file) {
        if (!(new File(file.getParentFile(), ".gitlet")).isDirectory()) {
            throw new IllegalArgumentException("not .gitlet working directory");
        }
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /** Deletes the file named FILE if it exists and is not a directory.
     *  Returns true if FILE was deleted, and false otherwise.  Refuses
     *  to delete FILE and throws IllegalArgumentException unless the
     *  directory designated by FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(String file) {
        return restrictedDelete(new File(file));
    }

    /* READING AND WRITING FILE CONTENTS */

    /** Return the entire contents of FILE as a byte array.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Return the entire contents of FILE as a String.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    /** Write the result of concatenating the bytes in CONTENTS to FILE,
     *  creating or overwriting it as needed.  Each object in CONTENTS may be
     *  either a String or a byte array.  Throws IllegalArgumentException
     *  in case of problems. */
    static void writeContents(File file, Object... contents) {
        try {
            if (file.isDirectory()) {
                throw new
                        IllegalArgumentException("cannot overwrite directory");
            }
            BufferedOutputStream str = new
                    BufferedOutputStream(Files.newOutputStream(file.toPath()));
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    str.write((byte[]) obj);
                } else {
                    str.write(((String) obj)
                            .getBytes(StandardCharsets.UTF_8));
                }
            }
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Return an object of type T read from FILE, casting it to EXPECTEDCLASS.
     *  Throws IllegalArgumentException in case of problems. */
    static <T extends Serializable> T readObject(File file,
                                                 Class<T> expectedClass) {
        try {
            ObjectInputStream in =
                    new ObjectInputStream(new FileInputStream(file));
            T result = expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException
                | ClassNotFoundException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Write OBJ to FILE. */
    static void writeObject(File file, Serializable obj) {
        writeContents(file, serialize(obj));
    }

    /* DIRECTORIES */

    /** Filter out all but plain files. */
    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

    /* OTHER FILE UTILITIES */

    /** Return the concatentation of FIRST and OTHERS into a File designator,
     *  analogous to the {@link java.nio.file.Paths(String, String[])}
     *  method. */
    static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }

    /** Return the concatentation of FIRST and OTHERS into a File designator,
     *  analogous to the {@link java.nio.file.Paths(String, String[])}
     *  method. */
    static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }


    /* SERIALIZATION UTILITIES */

    /** Returns a byte array containing the serialized contents of OBJ. */
    static byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException excp) {
            throw error("Internal error serializing commit.");
        }
    }



    /* MESSAGES AND ERROR REPORTING */

    /** Return a GitletException whose message is composed from MSG and ARGS as
     *  for the String.format method. */
    static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    /** Print a message composed from MSG and ARGS as for the String.format
     *  method, followed by a newline. */
    static void message(String msg, Object... args) {
        System.out.printf(msg, args);
        System.out.println();
    }

    /** FUNCTIONS */

    /** Represents a function from T1 -> T2.  The apply method contains the
     *  code of the function.  The 'foreach' method applies the function to all
     *  items of an Iterable.  This is an interim class to allow use of Java 7
     *  with Java 8-like constructs.  */
    abstract static class Function<T1, T2> {
        /** Returns the value of this function on X. */
        abstract T2 apply(T1 x);
    }

    /** Searches for commit.
     * @param message searches for a message. */
    static void commitSearcher(String message) {
        boolean found = false;
        File[] listFiles = new File(COMMITS_PATH).listFiles();
        int i = 0;
        while (i < listFiles.length) {
            File f = listFiles[i];
            String fileNameInCommitDir = f.getName();
            Commit workingCurr = commitFinder(fileNameInCommitDir);
            if (message.equals(workingCurr.getMessage())) {
                found = true;
                System.out.print(fileNameInCommitDir);
                System.out.println();
            }
            i++;
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Clears all staged files. **/
    static void clearStage() {
        File stage = new File(STAGE_PATH);
        for (File stagedFile : stage.listFiles()) {
            stagedFile.delete();
        }
    }

    /** Clears all staged files. **/
    static void clearRemove() {
        File remove = new File(REMOVE_FILES);
        for (File removeFile : remove.listFiles()) {
            removeFile.delete();
        }
    }

    /** Clears a particular staged file.
     * @param thisOne the file being cleared **/
    static void clearStage(File thisOne) {
        File stage = new File(STAGE_PATH);
        for (File stagedFile : stage.listFiles()) {
            if (thisOne.getName().equals(stagedFile.getName())) {
                stagedFile.delete();
            }
        }
    }

    /** Fetches the commit based on the ID.
     * @param commitID string
     * @return commit **/
    static Commit commitFinder(String commitID) {
        if (commitID.equals("")) {
            return null;
        }
        File checker = new File(COMMITS_PATH + SEPARATOR + commitID);
        if (checker.exists()) {
            Commit commit = readObject(checker, Commit.class);
            return commit;
        }
        return null;
    }

    /** Fetches the branch based on the name.
     * @param branchName string
     * @return string of local head commit i.e. the current branch. **/
    static String branchFinder(String branchName) {
        File branchChecker = new File(BRANCH_PATH + SEPARATOR + branchName);
        if (branchChecker.exists()) {
            String commitIDofBranch = readContentsAsString(branchChecker);
            return commitIDofBranch;
        }
        return null;
    }

    /** Determines if a commit exists.
     * @param commitID string
     * @return boolean - true or false **/
    static boolean commitExists(String commitID) {
        File check = new File(COMMITS_PATH + SEPARATOR + commitID);
        return check.exists();
    }

    /** Determines if a branch exists.
     * @param branchName string
     * @return boolean - true or false **/
    static boolean branchExists(String branchName) {
        File check = new File(BRANCH_PATH + SEPARATOR + branchName);
        return check.exists();
    }

    /** Returns the head commit's content.
     * @return true or false
     * @param checkFile the input file
     * @param path the path of the directory **/
    static boolean fileExistsInDirectory(String checkFile, String path) {
        File f = new File(path + SEPARATOR + checkFile);
        return f.exists();
    }
}
