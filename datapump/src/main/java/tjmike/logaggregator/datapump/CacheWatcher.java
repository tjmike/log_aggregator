package tjmike.logaggregator.datapump;


import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Watch the cache directory and fire events to the
 * DataPumpPusher when we see a change.
 *
 */
@Component
public class CacheWatcher extends RouteBuilder {
	private static final Logger s_log = LoggerFactory.getLogger(AsyncPusher.class);

	@Value("${Agent.LogCacheDir}")
	private String d_cacheDir;

	@Override
	public void configure() {

		File cacheWatcher = new File(d_cacheDir).getAbsoluteFile();

		if( s_log.isInfoEnabled() ) {
			s_log.info(
				String.format("Watching: %s as %s", d_cacheDir, cacheWatcher.getAbsolutePath()));
		}


		// Set up the directory we're going to watch
		String camelFrom = String.format("file-watch:%s?events=CREATE", cacheWatcher.getAbsolutePath());

		// set up the watch - and fire events to DataPumpPusher.processDirectory()
		from(camelFrom)
			.to("bean:DataPumpPusher?method=processDirectory")
		;

	}
}
