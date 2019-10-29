package tjmike.datastax.agent.dataPump;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tjmike.datastax.agent.LogTailResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import tjmike.datastax.proto.LoggerProtos;


/**
 * Accept data associated metadata files and write them to a durable store.
 */
@Service
public class DataPumpImpl implements  DataPumper {

	private static final Logger s_log = LoggerFactory.getLogger(DataPumpImpl.class);

	@Value("${Agent.LogCacheDir}")
	private String d_logCacheDirName;

	private File d_logCacheDir;

	public DataPumpImpl() {
//		d_cacheDir = cacheDir;
	}

	private File getCacheDir() {
		if( d_logCacheDir == null ) d_logCacheDir = new File(d_logCacheDirName);

		// Try to make the dirs
		if ( !d_logCacheDir.exists() ) {
			boolean created = d_logCacheDir.mkdirs();
			s_log.warn("Create log dir: " + d_logCacheDir.getAbsolutePath()  + " Result= " + created);

		}

		return d_logCacheDir;
	}

	public void process(LogTailResult ltr, byte[] data) throws IOException {

		writeFile(ltr, data);
	}


	private void writeFile(LogTailResult ltr, byte[] data) throws IOException  {
		LoggerProtos.LogPart lp = generateLogPart(ltr, data);
		String  name = generateFileName(ltr);
		String  nameTMP = name + ".tmp";

		File foutTMP = new File(getCacheDir(), nameTMP);
		File fout = new File(getCacheDir(), name);

		// Write the file

		try(FileOutputStream fos = new FileOutputStream(foutTMP) ) {
			fos.write(lp.toByteArray());
			fos.flush();
		}


		// perform atomic move
		Files.move(foutTMP.toPath(), fout.toPath(), StandardCopyOption.ATOMIC_MOVE);

	}

	/**
	 * Generate the protobuf object
	 * @param res
	 * @param buff
	 * @return
	 */
	private static LoggerProtos.LogPart generateLogPart(LogTailResult res,  byte[] buff) {
		LoggerProtos.LogPart.Builder lp = LoggerProtos.LogPart.newBuilder();
		lp.setId(res.getId());
		lp.setSeq(res.getSeqNum());
		lp.setSession(res.getSessionID());

		lp.setPayload( ByteString.copyFrom(buff,0,res.getnRead()));
		return lp.build();
	}

	/**
	 * Create a unique file name that can includes enough metadata to re-assemble the logs in order.
	 *
	 * @param ltr
	 * @return
	 */
	private static String generateFileName( LogTailResult ltr) {
		return String.format("%s_%d_%d.pbData", ltr.getId(), ltr.getSessionID(), ltr.getSeqNum());
	}
}
