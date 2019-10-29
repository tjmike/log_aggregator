package tjmike.datastax.datadecoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tjmike.datastax.proto.LoggerProtos;

import javax.annotation.PostConstruct;

/**
 * Service that accepts requests to process a directory. It will process the directory at least once.
 */
@Component("DataPumpDecoder")
public class DataPumpDecoder {


	// Directory for the server log cache as a string
	@Value("${Server.LogCacheDir}")
	private String d_logCacheDirName;


	// Directory for the server log (rebuilt) as a string
	@Value("${Server.LogDir}")
	private String d_logDirName;

	// Directory for the agent log cache as a path - created from string
	private Path d_logCacheDir;


	// Directory for rebuilt logs
	private Path d_rebuiltLogDir;


	private static Logger s_log = LoggerFactory.getLogger(DataPumpDecoder.class);




	// This is not static because this is a component and there should only be one instance
	// in the app
	private Semaphore d_lastIndexLock = new Semaphore(1);

	// The last index for the given log file session (log file + session)
	// This is populated at startup and updated when we do a write.
	private Map<String, Long> d_lastIndexes = new HashMap<>();



	// List the directory
	// log
	//   session
	//      sequence
	//
	// For a given Log/Session
	//

	/**
	 * Append the cached protobuff chunk to the rebuilt log.
	 * We maintain a different log per agent session.
	 * @param fName
	 */
	private void appendChunkToLog(FileName fName) {
		String reconstitutedName  = generateReconstitutedFileName(fName);
		Path target = d_rebuiltLogDir.resolve(reconstitutedName);
		Path dbgTarget = d_rebuiltLogDir.resolve(reconstitutedName + "." + fName.getSequence() + "_" + System.currentTimeMillis());
		Path src = fName.getOriginalPath();
		try (
			InputStream is = Files.newInputStream(src);
			OutputStream os  = Files.newOutputStream(target, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			OutputStream os2  = Files.newOutputStream(dbgTarget, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
		) {

			if(notYetProcessed(fName)) {


				// decode  the log part and append the data too the reconstructed log file
				LoggerProtos.LogPart lp = LoggerProtos.LogPart.parseFrom(is);
 				os.write(lp.getPayload().toByteArray());
				os.flush();

				// TODO remove this - this is for debugging
				os2.write(lp.getPayload().toByteArray());
				os2.flush();
				s_log.warn("WRITE: " + src.toString() + " --> " + target.toString());

				writeLastIndex(fName);

				s_log.warn("DELETE: " + src.toString());
				Files.delete(src);


			} else {
				s_log.warn("SKIP PROCESS - already processed: " + fName);
			}
		} catch(Exception ex ) {
//			hadError = true;
			s_log.error(ex.getMessage(), ex);
		}

	}

	/**
	 * Check to see if we've already processed this log chunk.
	 * @param fName
	 * @return
	 */
	private boolean notYetProcessed(FileName fName) {
		String lastSeqFileName = generateLastSequenceFileName(fName);
		long lastRead = getLastIndex(lastSeqFileName);
		return fName.getSequence() > lastRead;
//		boolean ret =  lastRead >= fName.getSequence();
//		return !ret;
	}

	/**
	 * Get the last index we read for each session
	 * @param lastSeqFileName
	 * @return
	 */
	private long getLastIndex(String lastSeqFileName ) {
		long read = 0;
		try {
			d_lastIndexLock.acquire();

			try {
				Long max = d_lastIndexes.get(lastSeqFileName);
				if( max != null) {
					read = max;
				}
			} finally {
				d_lastIndexLock.release();
			}

		} catch( InterruptedException ex) {
			s_log.error(ex.getMessage(), ex);
		}
		return read;
	}


	/**
	 * Read the last index we processed from the directory
	 * @param fileName
	 */
	private void initFromLastSequence(String fileName) {
		long ret; // = 0
		try {
			d_lastIndexLock.acquire();

			try {
				Path toRead = d_rebuiltLogDir.resolve(fileName);
				if( Files.exists(toRead)) {
					try (BufferedReader br = Files.newBufferedReader(toRead)) {

						String line = br.readLine();
						try {
							ret = Long.parseLong(line);
							d_lastIndexes.put(fileName, ret);
						} catch(NumberFormatException ex) {
							s_log.error(ex.getMessage(), ex);
						}
					}
				}

			} catch(IOException ex ) {
				s_log.error(ex.getMessage(),ex);

			} finally {
				d_lastIndexLock.release();
			}

		} catch (InterruptedException ex) {
			s_log.error(ex.getMessage(),ex);
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
	 * Generate the file name for the file that contains the last sequence parsed for a given
	 * file/session
	 * @param fname
	 * @return
	 */
	private String generateLastSequenceFileName(FileName fname) {
		return fname.getLogFileName() + "_" + fname.getSession() + ".lastSeq";
	}


	// write the last index we processed to a file and the lastIndexs map
	//
	private void writeLastIndex(FileName fName) {
		try {
			d_lastIndexLock.acquire();

			try {
				String fileName = generateLastSequenceFileName(fName);
				Path toWrite = d_rebuiltLogDir.resolve(fileName);
				s_log.warn("WRITE IDX: " + toWrite.toString() + " idx: " + fName.getSequence() );
				try (BufferedWriter wOut = Files.newBufferedWriter(toWrite,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING) ) {
					wOut.write( fName.getSequence() + System.lineSeparator());
				}
				d_lastIndexes.put(fileName, fName.getSequence());
			} catch(IOException ex ) {
				s_log.error(ex.getMessage(),ex);

			} finally {
				d_lastIndexLock.release();
			}

		} catch (InterruptedException ex) {
			s_log.error(ex.getMessage(),ex);
		}

	}



	//	@Autowired
	public DataPumpDecoder() {
		super();
	}


	/**
	 * Perform some init after the constructor and any @Value items are set.
	 *
	 */
	@PostConstruct
	private void getCacheDir() {
		// Generate a Path from the properties string;
		try {
			d_logCacheDir = new File(d_logCacheDirName).getCanonicalFile().toPath();
			if( !Files.exists(d_logCacheDir)) {
				Files.createDirectories(d_logCacheDir);
			}
		} catch(Exception ex ) {
			s_log.error(ex.getMessage(), ex);
		}

		// Build the reconstituted Path from the properties string
		try {
			d_rebuiltLogDir = new File(d_logDirName).getCanonicalFile().toPath();
			if( !Files.exists(d_rebuiltLogDir)) {
				Files.createDirectories(d_rebuiltLogDir);
			}

		} catch(Exception ex ) {
			s_log.error(ex.getMessage(), ex);
		}


		// init the last processed map
		try {
			// Lets init our last processed info
			Files.list(d_rebuiltLogDir)
				.filter((p) -> {

					boolean ret =  p.getFileName().toString().endsWith(".lastSeq");
					s_log.info("TEST: " + p.toString() + " Result = " + ret);
					return ret;

				})
				.forEach((p) -> initFromLastSequence(p.getFileName().toString()));
		} catch(IOException ex ) {
			s_log.error(ex.getMessage(), ex);
		}

	}


	/**
	 *
	 * The method is called when a new serialized protobuf file appears in the
	 * cache directory. We then marshall the request onto a single thread for processing.
	 * Even though this is marked as async we depend on the fact that all the work is being
	 * done on a single processing thread
	 * @throws IOException
	 */
	@Async("LogDecode")
	@SuppressWarnings("unused") // this will get called via Camelrouting
	public void processDirectory() {

		File[] filesToConsider = getPBDataFiles();

		if( filesToConsider != null ) {



			// create ordered data structure of files for processing
			TreeMap<String, TreeMap<Long, TreeMap<Long, FileName>>> byName =
				Arrays.stream(filesToConsider)
					.map((f) -> new FileName(f.toPath()))
					.collect(new FileNameCollector() )
			;
			processCandidates(byName);


		}
	}


	// key is the log file name , next key is the session, next key is the sequence number
	private void processCandidates(TreeMap<String, TreeMap<Long, TreeMap<Long, FileName>>> byName) {

		for( Map.Entry<String, TreeMap<Long, TreeMap<Long, FileName>>> entriesByName : byName.entrySet() ) {

			String logFileName = entriesByName.getKey();
			TreeMap<Long, TreeMap<Long, FileName>> bySession = entriesByName.getValue();

			for( Map.Entry<Long, TreeMap<Long, FileName>> entriesBySession : bySession.entrySet() ) {
				long session = entriesBySession.getKey();

				s_log.info("******************************************************************");

				TreeMap<Long, FileName> logSessionBySequence = entriesBySession.getValue();

				for( Map.Entry<Long,FileName>  entriesBySeq : logSessionBySequence.entrySet() ) {
					long seq = entriesBySeq.getKey();
					FileName fName = entriesBySeq.getValue();

					long prevSeq = getLastIndex(generateLastSequenceFileName(fName));

					{
						String msg = String.format("CONSIDERx: Name:%s Session:%d Seq:%d  PREVSeq: %d "
							, logFileName, session, seq, prevSeq);
						s_log.info(msg);
					}


					// Special case we just accept the first entry and start from there
					// This is because the delta could be > 1
					if( prevSeq == 0 ) {

						if(notYetProcessed(fName)) {
							String msg = String.format("PROCESSa: %s %d %d ", logFileName, session, seq);
							s_log.info(msg);
							appendChunkToLog(fName);
						} else {
							String msg = String.format("SKIP: %s %d %d ", logFileName, session, seq);
							s_log.info(msg);
						}

					} else {
						long delta = seq - prevSeq;
						// only process the next expected sequence
						// there's no facility for skipping missing chunks
						if( delta == 1 ) {
							String msg = String.format("PROCESSb: %s %d %d ", logFileName, session, seq);
							s_log.info(msg);
							appendChunkToLog(fName);
						} else {
							s_log.warn(String.format("Missing sequence have %d and %d", prevSeq, seq) );
						}




					}
				}
				s_log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
			}
		}
	}

	private File[] getPBDataFiles() {
		// get a filtered list of all files in the directory files
		File dir = d_logCacheDir.toFile();
		return dir.listFiles(
			pathname -> pathname.getName().endsWith(".pbData")
		);
	}



}
