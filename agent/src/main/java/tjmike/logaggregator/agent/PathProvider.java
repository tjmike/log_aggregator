package tjmike.logaggregator.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component("PathProvider")
public class PathProvider {

	private final List<String> d_logFiles;
	private List<LogTail> d_logTailFiles;

	private final String d_logCacheDirName;
	private Path d_logCacheDir;

	public PathProvider(
		@Value("#{'${Agent.LogsToMonitor}'.split(',')}") List<String> logFiles,
		@Value("${Agent.LogCacheDir}") String logCacheDirName
	) {
		d_logCacheDirName = logCacheDirName;
		d_logFiles = logFiles;
	}

	private final long d_SessionID = Instant.now().getEpochSecond();


	private static final Logger s_log = LoggerFactory.getLogger(PathProvider.class);


	/**
	 * Initialize directories.
	 *
	 */
	@PostConstruct
	void init() throws Exception {
		d_logFiles.forEach((f) -> s_log.info("Monitoring Log:'" + f + "'"));

		d_logTailFiles = d_logFiles.stream().map(
			(f) -> new LogTail(Paths.get(f).normalize(), d_SessionID)
		).collect(Collectors.toList());

		Path logCacheDir = Paths.get(d_logCacheDirName).toAbsolutePath();

		// Try to make the dirs
		if (!Files.exists(logCacheDir)) {
				d_logCacheDir = Files.createDirectories(logCacheDir).normalize();
				s_log.warn(
					String.format("Create log dir: %s", d_logCacheDir.toAbsolutePath()));
		}  else if(!Files.isDirectory(logCacheDir)) {
		throw new IOException(
			String.format("Log cache directory exists and is not a file: %s", logCacheDir.toString())
		);
		} else {
			d_logCacheDir = logCacheDir;
		}
	}

	List<LogTail> getLogTailFiles() {
		return d_logTailFiles;
	}

	public Path getLogCacheDir() {
		return d_logCacheDir;
	}

	long getSessionID() {
		return d_SessionID;
	}
}
