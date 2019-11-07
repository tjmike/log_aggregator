package tjmike.logaggregator.datapump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.concurrent.Executor;

/**
 * This class is responsible for accepting request to push files to a a server using http.
 */
@Service
public class AsyncPusher implements AsyncPusherIF {

	private final HttpClient d_httpClient;

	private static final Logger s_log = LoggerFactory.getLogger(AsyncPusher.class);

	private final String d_webServerPath;

	private long d_sleepUntil = 0;

	// We want too ensure the same directory doesnt get processed multiple times
	private final HashSet<PathWithAttributes> d_working = new HashSet<>();

	private final Executor d_executor;
	public AsyncPusher(
		@Value("${datapump.server.path}") String webServerPath,
		@Qualifier("DPumpPusher") Executor executor
	) {
		d_executor = executor;
		d_webServerPath =webServerPath;
		d_httpClient = HttpClient.newBuilder()
			            .version(HttpClient.Version.HTTP_2)
			            .build();
	}

	private synchronized void throttle(int seconds ) {
		d_sleepUntil = System.currentTimeMillis() + (seconds * 1000);
	}

	synchronized long sleepMillis( ) {
		long ret = d_sleepUntil - System.currentTimeMillis();
		ret = Math.max(0,ret);
		return ret;
	}


	// Rather than use async we use our own executor. This is so we can pass
	// our special runnable in that can be prioritized by the executor
	//
	//	@Async("DPumpPusher")
	public void uploadPath(final PathWithAttributes p) {

		Runnable rr = () -> uploadPathPrivate(p);

		PathPusherRunnable r = new PathPusherRunnable(
			p,
			rr
		);
		d_executor.execute(r);
	}

	private void uploadPathPrivate(PathWithAttributes pathToWorkOn) {

		// ensure that we're not already processing this path
		synchronized (d_working) {
			if( d_working.contains(pathToWorkOn) ) {
				s_log.info("Already working on: " + pathToWorkOn.getPath().getFileName().toString());
				return;
			} else {
				d_working.add(pathToWorkOn);
			}
		}

		try {

			{
				if( Files.exists(pathToWorkOn.getPath())) {
					long sleep = sleepMillis();
					if( sleep > 0 ) {
						try {
							s_log.info(String.format("Throttled: sleeping for %d millis", sleep));
							Thread.sleep(sleep);
						} catch (InterruptedException ex) {
							s_log.error(ex.getMessage(), ex);
						}
					}
					// This was suspected of leaving file descriptors open but it seems to be ok
					HttpRequest request = HttpRequest.newBuilder()
						.POST(HttpRequest.BodyPublishers.ofFile(pathToWorkOn.getPath()))
						.uri(URI.create(d_webServerPath))
						.setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
						.header("Content-Type", "application/application/octet-stream")
						.build();
					HttpResponse<String> response = d_httpClient.send(request, HttpResponse.BodyHandlers.ofString());


					int code = response.statusCode();
					String msg = response.body();

					// Check the message for a delay
					String key = "Throttle: ";
					if( msg.indexOf(key) == 0 ) {
						String amt = msg.substring(key.length()).trim();
						try {
							int seconds = Integer.parseInt(amt);
							throttle(seconds);
						} catch(NumberFormatException ex) {
							s_log.warn("Error parsing throttle message: " + msg);
						}
					}

					s_log.info(
						String.format("PUSH: %s Code: %d Message: %s", pathToWorkOn.getPath().getFileName(), code, msg)
					);

					// only delete the file and send success if we got an OK code from the server
					// TODO consider other 200 series codes
					if( code  == HttpURLConnection.HTTP_OK) {
						// If there were no errors then delete the the file
						Files.delete(pathToWorkOn.getPath());
					}

				} else {
					// if the file doesn't exist then we treat as success
					if( s_log.isDebugEnabled() ) {
						s_log.debug(String.format("PUSH: SKIP NONEXISTENT FILE: %s", pathToWorkOn.getPath().getFileName().toString()));
					}
				}
			}
		} catch (Throwable ex) {
//					 if something fails then we will not delete the file and it will be retried
			s_log.error(ex.getMessage(), ex);
		} finally {
			synchronized (d_working) {
				d_working.remove(pathToWorkOn);
			}
		}
	}

	synchronized int numWorking() {
		return d_working.size();
	}
}
