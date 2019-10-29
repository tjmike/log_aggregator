package tjmike.datastax.datapump;


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
	private static Logger s_log = LoggerFactory.getLogger(AsyncPusher.class);

	@Value("${Agent.LogCacheDir}")
	private String d_cacheDir;

	@Override
	public void configure() {

		File cacheWatcher = new File(d_cacheDir).getAbsoluteFile();
		s_log.info(
			String.format("Watching: %s as %s", d_cacheDir, cacheWatcher.getAbsolutePath()));

		String camelFrom = String.format("file-watch:%s?events=CREATE", cacheWatcher.getAbsolutePath());

		from(camelFrom)
			.to("bean:DataPumpPusher?method=processDirectory")
		;

	}
}
