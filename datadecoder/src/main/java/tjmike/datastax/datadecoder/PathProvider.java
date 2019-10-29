package tjmike.datastax.datadecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * Provide the various paths needed by this application.
 *
 */
@Component("PathProvider")
public class PathProvider {

	static final String s_PBDataExtension = ".pbData";

	// Directory for the server log cache as a string
	@Value("${Server.LogCacheDir}")
	private String d_logCacheDirName;
	// Directory for the agent log cache as a path - created from string
	private Path d_logCacheDir;


	// Directory for the server log (rebuilt) as a string
	@Value("${Server.LogDir}")
	private String d_rebuiltLogDirName;
	// Directory for rebuilt logs
	private Path d_rebuiltLogDir;

	private static final Logger s_log = LoggerFactory.getLogger(PathProvider.class);




	/**
	 * Initialize directories and last sequence map.
	 *
	 */
	@PostConstruct
	private void init() {
		// Generate a Path from the properties string;
		try {
			d_logCacheDir = new File(d_logCacheDirName).getCanonicalFile().toPath();
			if( !Files.exists(d_logCacheDir)) {
				Files.createDirectories(d_logCacheDir);
			}
		} catch(Exception ex ) {
			s_log.error(ex.getMessage(), ex);
		}

		// Build the reconstituted log path from the properties string
		try {
			d_rebuiltLogDir = new File(d_rebuiltLogDirName).getCanonicalFile().toPath();
			if( !Files.exists(d_rebuiltLogDir)) {
				Files.createDirectories(d_rebuiltLogDir);
			}
		} catch(Exception ex ) {
			s_log.error(ex.getMessage(), ex);
		}

	}

	String getPBDataExtension() {
		return s_PBDataExtension;
	}

	Path getRebuiltLogDir() {
		return d_rebuiltLogDir;
	}

	Path getLogCacheDir() {
		return d_logCacheDir;
	}
}
