package tjmike.logaggregator.datadecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 *
 * Manage the tracking of sequences for each log file/session.
 *
 */
@Component("SequenceTracker")
public class SequenceTracker {

	private static final Logger s_log = LoggerFactory.getLogger(SequenceTracker.class);

	// The last index for the given log file session (log file + session)
	// This is populated at startup and updated when we do a write.
	private final Map<String, Long> d_lastIndexes = new HashMap<>();

	private PathProvider d_pathProvider;

	public SequenceTracker(PathProvider pathProvider) {
		d_pathProvider = pathProvider;
	}

	/**
	 * Initialize directories and last sequence map.
	 *
	 */
	@PostConstruct
	void init() {

		// init the last processed map that keeps track of the last sequence number processed.
		// This allows the decoder to be stopped and started without missing any data


		try(Stream<Path>files = Files.list(d_pathProvider.getRebuiltLogDir())) {
				files.filter((p) -> p.getFileName().toString().endsWith(".lastSeq"))
				.forEach(this::initFromLastSequence);
		} catch(IOException ex ) {
			s_log.error(ex.getMessage(), ex);
		}

	}


	/**
	 * Read the last index we processed from the directory.
	 * Should be called only at startup.
	 * @param toRead
	 */
	private void initFromLastSequence(Path toRead) {

		try {
//			Path toRead = d_rebuiltLogDir.resolve(fileName);
			if( Files.exists(toRead)) {
				try (BufferedReader br = Files.newBufferedReader(toRead)) {

					String line = br.readLine();
					try {
						d_lastIndexes.put(toRead.getFileName().toString(), Long.parseLong(line));
					} catch(NumberFormatException ex) {
						s_log.error(ex.getMessage(), ex);
					}
				}
			}

		} catch(IOException ex ) {
			s_log.error(ex.getMessage(),ex);
		}

	}



	/**
	 * Get the last index we read for each session from our in memory map.
	 *
	 * @param fName
	 * @return
	 */
	long getLastIndex(PBLogFile fName ) {
		long read = 0;

		Long max = d_lastIndexes.get(generateLastSequenceFileName(fName));
		if( max != null) {
			read = max;
		}
		return read;
	}


	/**
	 * Generate the file name for the file that contains the last sequence parsed for a given
	 * file/session
	 * @param fName
	 * @return
	 */
	private String generateLastSequenceFileName(PBLogFile fName) {
		return fName.getLogFileName() + "_" + fName.getSession() + ".lastSeq";
	}


	/**
	 * Write the last index we processed to a file and the last indexes map.
	 *
	 * @param fName
	 */
	void writeLastIndex(PBLogFile fName) {
		try {

			// NOTE: we could, in theory, call this from a shutdown hook so we don't need to perform
			// an open/write/close so often
			String fileName = generateLastSequenceFileName(fName);
			Path toWrite = d_pathProvider.getRebuiltLogDir().resolve(fileName);
			if( s_log.isDebugEnabled()) {
				s_log.debug("WRITE IDX: " + toWrite.toString() + " idx: " + fName.getSequence());
			}
			try (BufferedWriter wOut = Files.newBufferedWriter(toWrite,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING) ) {
				wOut.write( fName.getSequence() + System.lineSeparator());
				wOut.flush();
			}

			d_lastIndexes.put(fileName, fName.getSequence());
		} catch(IOException ex ) {
			s_log.error(ex.getMessage(),ex);

		}

	}


}
