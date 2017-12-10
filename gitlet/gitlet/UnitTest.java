package gitlet;

import ucb.junit.textui;
import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;
import static gitlet.Utils.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Pancham Yadav
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void testInit() {
        String[] args = new String[]{"init"};
        Main test = new Main();
        test.main(args);
        File gitletDir = new File(GITLET_PATH);
        assertEquals(true, gitletDir.exists());
    }

    @Test
    public void testBranch() {
        String[] args;
        String master = "master";
        String newbranch = "newbranch";
        args = new String[]{"branch newbranch"};
        Gitlet gitSession = new Gitlet(args);
        File branchDir = new File(BRANCH_PATH + SEPARATOR + newbranch);
        File masterBranch = new File(BRANCH_PATH + SEPARATOR + master);
    }
}


