package gitlet;

import java.util.regex.Pattern;

/**
 * Created by panchamyadav on 11/21/17.
 * Modified from project 2.
 * @author Pancham Yadav
 */
public class Command {
    /** The command enums. */
    enum Type {
         COMMIT("commit"),
         CHECKOUT("checkout"),
         ADD("add"),
         RM("remove"),
         STATUS("status"),
         INIT("init"),
         LOG("log"),
         FIND("find"),
         BRANCH("branch"),
         RMBRANCH(("rm-branch")),
         RESET("reset"),
         MERGE("merge"),
         GLOBALLOG("global-log"),
         PUSH("push"),
         PULL("pull"),
         REMOTE("remote");

        /** Converting pattern.
         * @param pattern is the input**/
        Type(String pattern) {
            _pattern = Pattern.compile(pattern + "$");
        }

        /** The Pattern describing syntactically
         *  correct versions of this type of command. */
        private final Pattern _pattern;

    }
}
