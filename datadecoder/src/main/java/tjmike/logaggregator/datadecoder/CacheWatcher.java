package tjmike.logaggregator.datadecoder;


import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Watch the cache directory and fire events to the
 * DataPumpDecoder when we see a change.
 *
 */
@Component
public class CacheWatcher extends RouteBuilder {
	private static final Logger s_log = LoggerFactory.getLogger(CacheWatcher.class);

	private final PathProvider d_pathProvider;

	@Autowired
	public CacheWatcher(CamelContext context, PathProvider pathProvider) {
		super(context);
		d_pathProvider = pathProvider;
	}

//	@Override
	public void configure() {

		String cachePathString = d_pathProvider.getLogCacheDir().toString();

		s_log.info(
			String.format("Watching: %s",  cachePathString));

		String camelFrom = String.format("file-watch:%s?events=CREATE", cachePathString);


		from(camelFrom)
			.to("bean:DataPumpDecoderSort?method=processDirectory")
		;


	}
}
