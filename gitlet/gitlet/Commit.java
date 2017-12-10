package gitlet;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import static gitlet.Utils.sha1;

/**
 * @author Pancham Yadav
 */
public class Commit implements Serializable {

    /**
     * The commit functionality for Gitlet.
     **/
    private static final long serialVersionUID = 8449013511804116808L;
    /**
     * The current commit's ID.
     */
    private String id;
    /**
     * The parent ID.
     */
    private String parent;
    /**
     * The date.
     */
    private Date timestamp;
    /**
     * The hash map of filename to the SHA1 contents.
     */
    private HashMap<String, String> blobs;
    /**
     * The message.
     */
    private String message;
    /**
     * Tells us if the current commit is merged.
     */
    private boolean merged;
    /**
     * The merged parent ID, if any.
     */
    private String secondParent;

    /**
     * The commit merge constructor.
     * @param incomingMessage the message being input
     * @param date the date
     * @param blobz the input blobs
     * @param parenter the first parent
     **/
    public Commit(String incomingMessage,
                  Date date, String parenter,
                  HashMap<String, String> blobz) {
        this.parent = parenter;
        this.timestamp = date;
        this.message = incomingMessage;
        this.blobs = blobz;
        this.id = createSecret();
        this.merged = false;
    }

    /** Creates sha1 -- commit ID.
     * @return the sha1 of the object **/
    private String createSecret() {
        return sha1(timestamp.toString() + message);
    }

    /**
     * The commit merge constructor.
     * @param trigger triggers the function
     * @param incomingMessage the message being input
     * @param date the date
     * @param blobz the input blobs
     * @param parenter the first parent
*      @param parentTwo the second parent
     **/
    public Commit(boolean trigger, String incomingMessage,
                  Date date, String parenter,
                  String parentTwo,
                  HashMap<String, String> blobz) {
        this.parent = parenter;
        this.secondParent = parentTwo;
        this.timestamp = date;
        this.message = incomingMessage;
        this.blobs = blobz;
        this.merged = true;
    }

    /**
     * For the initial commit.
     * @param parenter the parent
     * @param initDate the date being initialized
     * @param messageIncoming the incoming message
     **/
    public Commit(String messageIncoming, Date initDate, String parenter) {
        this(messageIncoming, initDate, parenter, new HashMap<>());
    }

    /**
     * Returns commit ID for the current commit.
     **/
    public HashMap<String, String> getBlobs() {
        return this.blobs;
    }

    /**
     * Returns commit ID for the current commit.
     * @param inputBlobs inputter
     **/
    public void setBlobs(HashMap<String, String> inputBlobs) {
        this.blobs = inputBlobs;
    }

    /**
     * Returns commit ID for the current commit.
     * @return commit ID - string.
     **/
    public String getID() {
        return id;
    }

    /**
     * Returns commit ID for the current commit.
     * @param identity the incoming ID being set.
     ***/
    public void setID(String identity) {
        this.id = identity;
    }

    /**
     * Returns commit ID of parent for the current commit.
     * @return parent ID - string.
     **/
    public String getParent() {
        return parent;
    }

    /**
     * Returns commit ID of parent for the current commit.
     * @param parentIn the parent being set
     **/
    public void setParent(String parentIn) {
        this.parent = parentIn;
    }

    /**
     * Returns commit ID of parent for the current commit.
     * @return parent ID - string.
     **/
    public String getDate() {
        return this.timestamp.toString();
    }

    /**
     * Returns message in the current commit.
     * @return message - string.
     **/
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("===" + "\n");
        output.append("commit " + id + "\n");
        DateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        String timestamper = sdf.format(timestamp);
        if (merged) {
            output.append("Merge: "
                    + parent.substring(0, 7)
                    + " "
                    + secondParent.substring(0, 7)
                    + "\n");
        }
        output.append("Date: " + timestamper + "\n");
        output.append(message);
        return output.toString();
    }
}
