package tjmike.datastax.datapump;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Service that accepts requests to process a directory. It will process the directory at least once.
 */
@Component("DataPumpPusher")
public class DataPumpPusher  {


	// Directory for the agent log cache as a string
	@Value("${Agent.LogCacheDir}")
		private String d_logCacheDirName;

	// Directory for the agent log cache as a path - created from string
	private Path d_logCacheDir;

	private AsyncPusher d_asyncPusher;

	private static Logger s_log = LoggerFactory.getLogger(DataPumpPusher.class);

	private static final String s_ProtocolBufferExtension = ".pbData";


	@Autowired
	public DataPumpPusher(AsyncPusher asyncPusher) {
		d_asyncPusher = asyncPusher;
	}





	// Lazy creation of the path - it's not set in the constructor
	private Path getCacheDir() {
		// We need the full path if the passed in one os relative
		if( d_logCacheDir == null ) {
			d_logCacheDir = new File(d_logCacheDirName).getAbsoluteFile().toPath();
			if( s_log.isInfoEnabled()) {
				s_log.info(String.format("Log Cache Dir = %s", d_logCacheDir ));
			}

		}

		return d_logCacheDir;
	}



	// Process the directory. This is fairly fast operation because the push command is asnyc
	// We get an event from the cache watcher and marshall it onto the
	// the single DirectoryProcessor thread. We don't need to list directories
	// in parallel. If we get multiple requests while processing this we need
	// only execute the process one more time. A queue of two allows for this.
	@SuppressWarnings("unused") // this will get called via Spring
	@Async("DirectoryProcessor")
	public  void processDirectory()  {



		File dir = getCacheDir().toFile();
		File [] filesToConsider = dir.listFiles(
			pathname -> pathname.getName().endsWith(s_ProtocolBufferExtension)
		);




		// This should be fast, we just tee up the files to be pushed.
		// If the queue overflows then we ignore the request. It should get picked up
		// on another pass
		if( filesToConsider != null ) {
			Arrays.stream(filesToConsider).map(File::toPath).forEach( (p)->{
				d_asyncPusher.requestPathPush(p); // request the the path be processed
				d_asyncPusher.push(); // request work to get done - sort of like a notify()
			}
			);

		}

	}


}
