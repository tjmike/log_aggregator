package tjmike.datastax.logServer.ServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import tjmike.datastax.proto.LoggerProtos;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


/**
 * A web server that accepts protobuffers via http post and stores them to disc for later processing.
 *
 */

@RestController("tjmike.datastax.logServer.LogServerController")
public class LogServerController {
	private static final Logger s_log = LoggerFactory.getLogger(LogServerController.class);
	private static final String s_AliveMessage = "Log Server Is Alive";
	// some metrics for this instance
	private int counter = 0;

	@Value("${Server.LogCacheDir}")
	private String d_cacheDirectoryString;
	private Path d_cachePath;
	private boolean d_shutItDown = false;

	/**
	 * A simple backoff  strategy - if the delay is set then we tell clients to back off pushing data
	 * for this many seconds
	 */
	private int d_throttle = 0;


 @PostConstruct
 private void initCachePath() {
 	try {
	 	File cacheFile = new File(d_cacheDirectoryString).getCanonicalFile();

		if(!cacheFile.exists() ) {
			boolean created = cacheFile.mkdirs();
			if( !created ) {
				s_log.error(
					String.format("cache file doesn't exist and could not be created. %s",cacheFile.getCanonicalPath())
				);
				d_shutItDown = true;
			}
		}

		if( !cacheFile.isDirectory()  ) {
			s_log.error(
				String.format("cache file exists but is not a directory. %s",cacheFile.getCanonicalPath())
			);
			d_shutItDown = true;
		}

		d_cachePath = cacheFile.toPath();

	} catch (Exception ex) {
 		s_log.error(ex.getMessage(), ex);
		d_shutItDown = true;
	}
 }


	@EventListener
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ApplicationContext ctx = event.getApplicationContext();
		if( d_shutItDown ) {
			SpringApplication.exit(ctx, () -> -1 );
		}
	}

	@RequestMapping("/")
	public String index() {
		return s_AliveMessage;
	}


	/**
	 * Set the log server throttle seconds
	 * @param seconds
	 * @return
	 */
	@RequestMapping("/throttle")
	public String setThrottle(int seconds) {
		if( seconds >= 0 ) {
			synchronized (this) {
				d_throttle = seconds;
			}
		}
		return "Log server throttle = " + d_throttle;
	}


	private void save(LoggerProtos.LogPart lp ) throws IOException  {
 		String id = lp.getId();
 		long seqNum = lp.getSeq();
 		long session = lp.getSession();


 		String fileName = String.format("%s_%s_%s.pbData", id,session, seqNum);

 		File tmpFile = new File(d_cachePath.toFile(),fileName + ".tmp");
 		File outFile = new File(d_cachePath.toFile(),fileName );


		try(FileOutputStream fos = new FileOutputStream(tmpFile) ) {
			fos.write(lp.toByteArray());
			fos.flush();
		}

		// perform atomic move
		Files.move(tmpFile.toPath(), outFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
	}

	/**
	 * Accept a protobuffer payload and stick it into an on disc cache to be re-assembled.
	 * @param dataStream
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/data")
	public String acceptData(InputStream dataStream) throws Exception {

		if( s_log.isDebugEnabled() ) {
			s_log.debug("CACHE DIR = " + d_cachePath.toString());
		}
		LoggerProtos.LogPart logPartRebuilt = LoggerProtos.LogPart.parseFrom(dataStream);

		save(logPartRebuilt);

		synchronized (this) {
 			counter++;
		}

		if( s_log.isInfoEnabled() ) {
			int payloadSize = logPartRebuilt.getPayload().size();
			String id = logPartRebuilt.getId();
			long session = logPartRebuilt.getSession();
			long seq = logPartRebuilt.getSeq();
			
			String msg = String.format("Name: %s Session: %d Seq: %d Payload: %d", id, session, seq, payloadSize);
			s_log.info(msg);
		}

		int throttleSeconds;
		synchronized (this) {
			throttleSeconds = d_throttle;
		}
		return "Throttle: " + throttleSeconds;
	}

	/**
	 *
	 * @return number of buffers processed
	 */
	@RequestMapping("/count")
	public String count() {
		int count;
		synchronized (this) {
			count = counter;
		}
		return String.format("Count=%d", count);
	}
}


