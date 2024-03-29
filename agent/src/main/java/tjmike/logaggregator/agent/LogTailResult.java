package tjmike.logaggregator.agent;


/**
 *  An immutable class representing the result of a log poll.
 */
public class LogTailResult {
	/**
	 * A unique ID for this log file. This can be used to reassemble the log
	 */
	private final String d_id;

	/**
	 * SessionID of this logger
	 */
	private final long d_sessionID;
	/**
	 * Timestamp this result was captured. Can be used for sequencing results.
	 */
	private final long d_timestamp;
	/**
	 * Bytes read can be -1, 0, or N <= size of user supplied buffer
	 */
	private final int d_nRead;

	/**
	 * Sequence number of this entry
	 */
	private final long d_seqNum;
	/**
	 * Status of the poll request.
	 * @see LogTail.STATUS
	 */
	private final LogTail.STATUS lastStatus;

	LogTailResult(long sessionID, String id, long sequence, int nRead, LogTail.STATUS lastStatus) {
		d_sessionID = sessionID;
		d_id = id;
		d_seqNum = sequence;
		d_nRead = nRead;
		this.lastStatus = lastStatus;
		d_timestamp = System.currentTimeMillis();
	}

	public int getNumberRead() {
		return d_nRead;
	}

	LogTail.STATUS getLastStatus() {
		return lastStatus;
	}

	public String getId() {
		return d_id;
	}

	public long getTimestamp() {
		return d_timestamp;
	}

	public long getSeqNum() {
		return d_seqNum;
	}

	public long getSessionID() {
		return d_sessionID;
	}
}
