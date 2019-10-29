package tjmike.datastax.datadecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tjmike.datastax.proto.LoggerProtos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that accepts requests to process a directory.
 * All request are initiated by a call to processDirectory().
 * This marshals all the work onto a single worker thread where the work is performed.
 *
 */
@Component("DataPumpDecoderSort")
public class DataPumpDecoderSort {

	private static Logger s_log = LoggerFactory.getLogger(DataPumpDecoderSort.class);
	private SequenceTracker d_sequenceTracker;
	private PathProvider    d_pathProvider;


	@Autowired
	public DataPumpDecoderSort(SequenceTracker sequenceTracker, PathProvider pathProvider) {
		d_sequenceTracker = sequenceTracker;
		d_pathProvider = pathProvider;
	}




	/**
	 * Append the cached protobuff chunk to the rebuilt log.
	 * We maintain a different log per agent session.
	 *<p/>
	 * <em>NOTE:</em> We append the data and then write the last index to a file.
	 * If we perform the write, and the app fails to  perform the delete and index write
	 * and exits then we will re-append the last buffer to the log.
	 *
	 *
	 * @param fName
	 */
	private void appendChunkToLog(FileName fName) {
		String reconstitutedName  = generateReconstitutedFileName(fName);


		Path target = d_pathProvider.getRebuiltLogDir().resolve(reconstitutedName);
		Path src = fName.getOriginalPath();
		try (
			InputStream is = Files.newInputStream(src);
			OutputStream os  = Files.newOutputStream(target, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
		) {
			// decode  the log part and append the data to the reconstructed log file
			LoggerProtos.LogPart lp = LoggerProtos.LogPart.parseFrom(is);
			os.write(lp.getPayload().toByteArray());
			os.flush();
			if( s_log.isInfoEnabled() ) {
				s_log.info("Append: " + src.toString() + " --> " + target.toString());
			}
			// delete the buffer
			if( s_log.isDebugEnabled() ) {
				s_log.warn("DELETE: " + src.toString());
			}
			Files.delete(src);

			// track what we've processed so far
			d_sequenceTracker.writeLastIndex(fName);

		} catch(Exception ex ) {
			s_log.error(ex.getMessage(), ex);
		}

	}


	/**
	 * Generate the filename for the rebuilt file
	 * @param fName
	 * @return
	 */
	private String generateReconstitutedFileName(FileName fName) {
		String logFileName = fName.getLogFileName();
		long session = fName.getSession();
		return logFileName + "." + session;
	}

	/**
	 *
	 * The method is called when a new serialized protobuf file appears in the
	 * cache directory. We then marshall the request onto a single thread for processing.
	 * Even though this is marked as async we depend on the fact that all the work is being
	 * done on a single processing thread. To enforce this we specify a threadpook that we control that
	 * has a size of one.
	 *
	 * @throws IOException
	 */
	@Async("LogDecode")
	@SuppressWarnings("unused") // this will get called via Camelrouting
	public void processDirectory()  {

		File[] filesToConsider = getPBDataFiles();

		if( filesToConsider != null ) {

			List<FileName> fileNames = Arrays.stream(filesToConsider)
				.map((f) -> new FileName(f.toPath()))
				.sorted(new FileNameComparator())
				.collect(Collectors.toList())
				;

			processCandidates(fileNames);

		}
	}


	/**
	 *
	 * Attempt to process all the FileNames provided. We assume the files have been sorted as defined in:
	 *
	 * @see FileNameComparator
	 *
	 * @param fileNames - order list of FileNames
	 */
	private void processCandidates(List<FileName> fileNames) {
		for(FileName fName : fileNames ) {
			String logFileName = fName.getLogFileName();
			long session = fName.getSession();
			long seq = fName.getSequence();

			long lastProcessedSeq = d_sequenceTracker.getLastIndex(fName);

			if( s_log.isInfoEnabled() ) {
				String msg = String.format("CONSIDER: Name:%s Session:%d Seq:%d  Last Processed: %d"
					, logFileName, session, seq, lastProcessedSeq);
				s_log.info(msg);
			}

			// this is the expected sequence number
			long expectedSeq = lastProcessedSeq + 1;

			if( seq == expectedSeq ) {
				// process the expected sequence
				if( s_log.isInfoEnabled() ) {
					String msg = String.format("PROCESS FoundNex: %s %d %d ", logFileName, session, seq);
					s_log.info(msg);
				}
				appendChunkToLog(fName);

			} else if (lastProcessedSeq == 0 ) {
				// special case if we have a seq > 1 but prev is zero then just start from there
				if( s_log.isInfoEnabled() ) {
					String msg = String.format("PROCESS INIT: %s %d %d ", logFileName, session, seq);
					s_log.info(msg);
				}
				appendChunkToLog(fName);

			} else {
				s_log.warn(String.format("Missing sequence have %d and %d", lastProcessedSeq, seq) );

			}

		}
	}

	/**
	 *
	 * @return all the pbData files in the cache dir waiting to be processed
	 */
	private File[] getPBDataFiles() {
		// get a filtered list of all files in the directory files
		File dir = d_pathProvider.getLogCacheDir().toFile();
		return dir.listFiles(
			pathname -> pathname.getName().endsWith(d_pathProvider.getPBDataExtension())
		);
	}

}
