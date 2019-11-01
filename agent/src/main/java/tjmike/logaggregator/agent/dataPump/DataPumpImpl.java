package tjmike.logaggregator.agent.dataPump;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tjmike.logaggregator.agent.LogTailResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import tjmike.logaggregator.agent.PathProvider;
import tjmike.logaggregator.proto.LoggerProtos;



/**
 * Accept data associated metadata files and write them to a durable store.
 */
@Service
public class DataPumpImpl implements DataPumpInterface {

	private static final Logger s_log = LoggerFactory.getLogger(DataPumpImpl.class);

	private final PathProvider d_pathProvider;

	@Autowired
	public DataPumpImpl(PathProvider pathProvider) {
		d_pathProvider = pathProvider;
	}



	public void process(LogTailResult ltr, byte[] data) throws IOException {

		writeFile(ltr, data);
	}


	private void writeFile(LogTailResult ltr, byte[] data) throws IOException  {
		LoggerProtos.LogPart lp = generateLogPart(ltr, data);
		String  name = generateFileName(ltr);
		String  nameTMP = name + ".tmp";

		Path poutTMP = d_pathProvider.getLogCacheDir().resolve(nameTMP);
		Path pout= d_pathProvider.getLogCacheDir().resolve(name);



		// Write the file

		try(OutputStream fos = Files.newOutputStream(poutTMP) ) {
			fos.write(lp.toByteArray());
			fos.flush();
		}

		// perform atomic move
		Files.move(poutTMP, pout, StandardCopyOption.ATOMIC_MOVE);

		s_log.info("Created: " + pout.toString());
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
		lp.setPayload( ByteString.copyFrom(buff,0,res.getNumberRead()));
		return lp.build();
	}

	/**
	 * Create a unique file name that can includes enough metadata to re-assemble the logs in order.
	 *
	 * @param ltr
	 * @return
	 */
	public static String generateFileName( LogTailResult ltr) {
		return String.format("%s_%d_%d.pbData", ltr.getId(), ltr.getSessionID(), ltr.getSeqNum());
	}
}
