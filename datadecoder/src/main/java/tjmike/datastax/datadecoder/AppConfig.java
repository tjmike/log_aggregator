package tjmike.datastax.datadecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AppConfig {

	private static final Logger s_log = LoggerFactory.getLogger(AppConfig.class);

	// we do all our protobuff decoding and appending work
	// on this single thread. The primary reason for this is
	// a) Ensure that the processing is single threaded for now
	//    1) We don't know if the polling events will come from multiple threads so we don't want to
	//       process on the polling thread.
	//    2) A single threaded decoder is easier/faster to implement
	// b) Prevent a long running process from occurring on the caller's thread
	//
	// The capacity is only 2 deep because all we really need to do is to ensure we don't
	// miss a request. If we get 10 requests when processing we only need to ensure that we take
	// one more pass.
	//
	@Bean(name="LogDecode")
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(2);
		executor.setThreadNamePrefix("LogDecode-");
		executor.initialize();
		executor.setRejectedExecutionHandler(
			(r, executor1) -> {

				if( s_log.isDebugEnabled( ) ) {
					s_log.debug("Ignore processDirectory request - work already queued");
				}
				// do nothing - if the pool is overrun the file will not be processed
				// and it will get another shot
			}
		);
		return executor;
	}
}
