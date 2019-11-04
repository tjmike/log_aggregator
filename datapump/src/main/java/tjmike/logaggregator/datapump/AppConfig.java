package tjmike.logaggregator.datapump;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AppConfig {


//	private static class ThreadPool1 {
//
//		// thread pool for pushing data via http
//		@Bean(name = "DPumpPusher")
//		public Executor taskExecutor() {
//			return new DataPushExecutor();
//		}
//
//	}

	private static class ThreadPool2 {
		// thread for directory processing
		// NOTE: must be a single thread - like AWT event thread
		@Bean(name = "DirectoryProcessor")
		public Executor taskExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(1);
			executor.setMaxPoolSize(1);
			executor.setQueueCapacity(2); // ensure that if we get a request that we run again
			executor.setThreadNamePrefix("DirProcess-");
			executor.initialize();
			executor.setRejectedExecutionHandler((r, executor1) -> {
				// do nothing - if the pool is overrun the directory
			});
			return executor;
		}
	}
}
