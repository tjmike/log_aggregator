package tjmike.datastax.datadecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Manage the tracking of sequences for each log file/session.
 *
 */
@Component("SequenceTracker")
public class SequenceTracker {


	// Directory for the server log (rebuilt) as a string
	@Value("${Server.LogDir}")
	private String d_rebuiltLogDirName;
	// Directory for rebuilt logs
	private Path d_rebuiltLogDir;

	private static Logger s_log = LoggerFactory.getLogger(SequenceTracker.class);


	// The last index for the given log file session (log file + session)
	// This is populated at startup and updated when we do a write.
	private Map<String, Long> d_lastIndexes = new HashMap<>();



	/**
	 * Initialize directories and last sequence map.
	 *
	 */
	@PostConstruct
	private void init() {

		// Build the reconstituted log path from the properties string
		try {
			d_rebuiltLogDir = new File(d_rebuiltLogDirName).getCanonicalFile().toPath();
			if( !Files.exists(d_rebuiltLogDir)) {
				Files.createDirectories(d_rebuiltLogDir);
			}

		} catch(Exception ex ) {
			s_log.error(ex.getMessage(), ex);
		}


		// init the last processed map that keeps track of the last sequence number processed.
		// This allows the decoder to be stopped and started without missing any data
		try {
			// Lets init our last processed info
			Files.list(d_rebuiltLogDir)
				.filter((p) -> p.getFileName().toString().endsWith(".lastSeq"))
				.forEach((p) -> initFromLastSequence(p.getFileName().toString()));
		} catch(IOException ex ) {
			s_log.error(ex.getMessage(), ex);
		}

	}



	/**
	 * Read the last index we processed from the directory.
	 * Should be called only at startup.
	 * @param fileName
	 */
	private void initFromLastSequence(String fileName) {
		try {
			Path toRead = d_rebuiltLogDir.resolve(fileName);
			if( Files.exists(toRead)) {
				try (BufferedReader br = Files.newBufferedReader(toRead)) {

					String line = br.readLine();
					try {
						d_lastIndexes.put(fileName, Long.parseLong(line));
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
	long getLastIndex(FileName fName ) {
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
	private String generateLastSequenceFileName(FileName fName) {
		return fName.getLogFileName() + "_" + fName.getSession() + ".lastSeq";
	}



	/**
	 * Write the last index we processed to a file and the last indexes map.
	 *
	 * @param fName
	 */
	void writeLastIndex(FileName fName) {
		try {

			// NOTE: we could, in theory, call this from a shutdown hook so we don't need to perform
			// an open/write/close so often
			String fileName = generateLastSequenceFileName(fName);
			Path toWrite = d_rebuiltLogDir.resolve(fileName);
			s_log.warn("WRITE IDX: " + toWrite.toString() + " idx: " + fName.getSequence() );
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
