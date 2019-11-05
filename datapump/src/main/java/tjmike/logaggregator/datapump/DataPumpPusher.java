package tjmike.logaggregator.datapump;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	private final String d_logCacheDirName;

	// Directory for the agent log cache as a path - created from string
	private Path d_logCacheDir;

	private final AsyncPusherIF d_asyncPusher;

	private static final Logger s_log = LoggerFactory.getLogger(DataPumpPusher.class);

	static final String s_ProtocolBufferExtension = ".pbData";


	@Autowired
	public DataPumpPusher(
		@Value("${Agent.LogCacheDir}") String logCacheDirName,
		AsyncPusherIF asyncPusher
	) {
		d_logCacheDirName = logCacheDirName;
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

	// Process the directory. This is fairly fast operation because the push command is async
	// We get an event from the cache watcher and marshall it onto the
	// the single DirectoryProcessor thread. We don't need to list directories
	// in parallel. If we get multiple requests while processing this we need
	// only execute the process one more time. A queue of two allows for this.

	@SuppressWarnings("unused") // this will get called via Spring
	@Async("DirectoryProcessor")
	public  void processDirectory()  {


		// Get get a list of paths that all have attributes
		List<PathWithAttributes> paths;
		try {
			try (Stream<Path> ps = Files.list(getCacheDir())) {
				paths = ps
					.filter((p) -> p.getFileName().toString().endsWith(s_ProtocolBufferExtension))
					.map(PathWithAttributes::new)
					.filter((p) -> p.getAttributes() != null)
					.collect(Collectors.toList()
					);

			} catch (IOException ex) {
				s_log.error(ex.getMessage(), ex);
				paths = new ArrayList<>(0);
			}
		} catch(UncheckedIOException ex ) {
			paths = new ArrayList<>(0);
			s_log.error(ex.getMessage(), ex);

		}

		// sort the paths by last modified
		paths.sort((o1, o2) -> {
			FileTime ft1 = o1.getAttributes().lastModifiedTime();
			FileTime ft2 = o2.getAttributes().lastModifiedTime();
			return ft1.compareTo(ft2);
		});


		// This should be fast, we just tee up the files to be pushed.
		// If the queue overflows then we ignore the request. It should get picked up
		// on another pass
		{
			int szBefore = paths.size();
			paths.forEach(
				d_asyncPusher::uploadPath
		);
		}
	}

}
