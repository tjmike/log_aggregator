package tjmike.datastax.agent;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manage the tailing of a specific file including the input stream.
 * The data is read via a poll with a supplied byte buffer. This class is not thread safe. The
 * expected usages is for some worker thread to poll at a regular interval.
 *
 *
 */
class LogTail {

	private static Logger s_log = LoggerFactory.getLogger(LogTail.class);

	/**
	 * The log file we are tailing
	 */
	private Path d_path;

	/**
	 * Session id to be associated with log data packets
	 */
	private long d_sessionID;

	/**
	 * Sequence id of data packets
	 */
	private long d_sequence = 0;

	/**
	 * Last set of file attributes we know about
	 */
	private BasicFileAttributes d_attrs;

	/**
	 * Input stream to the file we are tailing. An assumption is being made that the file
	 * is active and maintaining an open stream is reasonable. Also if the file is rotated we
	 * can finish off the old file before switching to the new one if we have an open stream
	 * to the file.
	 */
	private FileInputStream d_inputStream;

	/**
	 * Total number of bytes read from this file. The can be reset if the file is rotated or truncated.
	 */
	private long d_totalRead = 0;

	/**
	 * Total number of times poll was called but there was no change
	 */
	private long d_totalMisses = 0;


	private static Logger logger = LoggerFactory.getLogger(LogAgent.class);


	/**
	 * Poll result status
	 */
	enum STATUS {
		/**
		 * The file was moved. This may happen in the case of a log rotation
		 */
		MOVED,
		/**
		 * The file was deleted.
		 */
		DELETED,
		/**
		 * New data was detected (size > total bytes read
		 */
		NEWDATA,
		/**
		 * The file is unchanged
		 */
		UNCHANGED,
		/**
		 * The file was truncated current size < bytes read. This is really an error condition,
		 * buy we make an effort to deal with it.
		 */
		TRUNCATED
	}
	LogTail(Path path, long sessionID)  {
		d_path = path;
		d_sessionID = sessionID;
		if( Files.exists(d_path)) {
			try {
				d_attrs = Files.readAttributes(d_path, BasicFileAttributes.class);
				resetInputStream(true);
			} catch(Exception ex ) {
				s_log.error(ex.getMessage(),ex);
			}
		}
	}

//	private long getTotalRead() {
//		return d_totalRead;
//	}

	/**
	 * Determine the status of the file for tail
	 * @return
	 * @throws IOException
	 */
	private STATUS status() throws IOException {
		STATUS status;

		if( Files.exists(d_path) ) {

			// my last known device/inode

			BasicFileAttributes attrs = Files.readAttributes(d_path, BasicFileAttributes.class);
			if( d_attrs == null ) {
				if( logger.isInfoEnabled()) {
					logger.info(d_path.getFileName().toString() + ": NEW FILE : RESET INPUT");
				}
				d_attrs = attrs;
				resetInputStream(false);
			}

			Object myKey = d_attrs != null ? d_attrs.fileKey() : null;

			// current device/inode
			Object newKey = attrs.fileKey();

			// assume that if device/inode  (key) is the same then we have the same file
			if( (myKey!=null&&myKey.equals(newKey)) ) {
				long latestSize = attrs.size();


				if( logger.isInfoEnabled()) {
					logger.info(d_path.getFileName().toString() + ": lastestSize = " + latestSize
						+ " totalRead= " + d_totalRead);
				}

				// if the file is at least as large as what we have read we assume
				// its unchanged
				if( latestSize > d_totalRead) {

					status = STATUS.NEWDATA;
				} else if ( latestSize == d_totalRead ) {
					status = STATUS.UNCHANGED;
				} else {
					// If the file is smaller than what we already read then assume it's
					// truncated. NOTE: truncated files can have race condition issues.
					// If the file is truncated after our test then we may attempt to read
					// before resetting the read pointer.
					status = STATUS.TRUNCATED;
				}

			} else {
				status = STATUS.MOVED;
			}

		} else {

			status =  STATUS.DELETED;
		}

		logger.info(d_path.getFileName().toString() + ": current status =  " + status.name() );


		return status;
	}
	private long size() throws IOException {

		return Files.readAttributes(d_path, BasicFileAttributes.class).size();
	}
//	private Path getPath() {
//		return d_path;
//	}

//	private  BasicFileAttributes getAttrs() {
//		return d_attrs;
//	}

//	private FileInputStream getInputStream() {
//		return d_inputStream;
//	}

	private void setAttrs() throws IOException {
		d_attrs = Files.readAttributes(d_path, BasicFileAttributes.class);
	}

	private void resetInputStream(boolean shouldSeek) throws IOException {
		long seek = 0;
		if( d_inputStream != null ) {
			d_inputStream.close();
			d_totalRead = 0;
		} else {
			if( shouldSeek ) {
				seek = size();
				d_totalRead = seek;
			}
		}
		d_inputStream = new FileInputStream(d_path.toFile());
		if( seek > 0 ) {
			long skipped = d_inputStream.skip(seek);
			if( skipped != seek ) {
				s_log.warn(String.format("Tried to seek %d but seeked %d", seek, skipped));
			}
		}
	}
	private int read(byte[] data) throws IOException {
		logger.info("READ BEGIN : total read = " + d_totalRead);
		int nread = d_inputStream.read(data);
		if (nread > 0) {
			d_totalRead += nread;

			++d_sequence;
		}
		logger.info("READ END : total read = " + d_totalRead);

		return nread;
	}

//	public long getSequence() {
//		return d_sequence;
//	}

	/**
	 * Poll the log file, populating the supplied buffer with as much data as a single read
	 * will provide. The LogTailResult is immutable and thread safe.
	 *
	 * NOTE we assume the file is not truncated.
	 *
	 * @param data - bye buffer for reading data.
	 * @return @{@link LogTailResult}
	 * @throws IOException
	 */
	 LogTailResult poll(byte [] data) throws IOException {
		// check size to see if the was truncated
		STATUS status = status();

		int nRead = 0;

		switch (status ) {
			case NEWDATA:
				nRead = read(data);
				logger.info(d_path.getFileName().toString() + " : NEW DATA : nread=" + nRead );
				break;
			case UNCHANGED:
				logger.info(d_path.getFileName().toString() + " : UNCHANGED : nread=" + nRead );
				break;
			case MOVED:
				//
				// keep reading to the end of the file
				// once we get past the end of the file
				// reset to the new file
				// data will be lost if we restart in the middle of this
				//
				nRead = read(data);
				logger.info(d_path.getFileName().toString() + " : READ OLD FILE: nread=" + nRead );

				if( nRead < 0 ) {
					setAttrs();
					resetInputStream(false);
					nRead = read(data);

					logger.info(d_path.getFileName().toString() + " : READ NEW FILE: nread=" + nRead );

				}
				logger.info(d_path.getFileName().toString() + " : MOVED FILE: nread=" + nRead );
				break;
			case DELETED:
				nRead = 0;
				logger.info(d_path.getFileName().toString() + " : DELETED FILE: nread=" + nRead );
				break;
			case TRUNCATED:
				resetInputStream(false);
				nRead = read(data);
				logger.info(d_path.getFileName().toString() + " : TRUNCATED FILE: nread=" + nRead );
				break;
			default:
				logger.error("Error unexpected status: " + status.name());
		}


		if( nRead > 0 ) {
			d_totalMisses = 0;
		} else {
			++d_totalMisses;
		}
		return new LogTailResult(d_sessionID,d_path.getFileName().toString(),d_sequence, nRead, status);
	}

//	public long getTotalMisses() {
//		return d_totalMisses;
//	}
}
