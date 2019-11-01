package tjmike.logaggregator.datapump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

/**
 * This class is responsible for accepting request to push files to a a server using http.
 */
@Service
public class AsyncPusher implements AsyncPusherIF {

	/**
	 * An order list of work items.
	 */
	private final PathForLogChunkWorkList d_workQueue = new PathForLogChunkWorkList();

	private final HttpClient d_httpClient;

	private static final Logger s_log = LoggerFactory.getLogger(AsyncPusher.class);


	private String d_webServerPath;


	private long d_sleepUntil = 0;



	public AsyncPusher(
		@Value("${datapump.server.path}") String webServerPath
	) {
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


	/**
	 *
	 * Request that this path be pushed.
	 *
	 * The use case is to call this followed by push().
	 * This method ensures the the work request is accepted. If all the threads are running then
	 * pus() request will be dropped.
	 *
	 * The push() is sort of like a notify(). It ensures that if there's no thread
	 * processing the requests that one gets started.
	 *
	 * A better approach would be to not have to follow the requestPathPush() with a push()...
	 *
	 * @param pathToPush
	 */
	public void requestPathPush(Path pathToPush) {
		d_workQueue.addPath(pathToPush);
	}

	/**
	 *
	 * Request that a push happen. This will kick off another thread if one is available.
	 * If one is not then one of the running threads should pick u the Path to process.
	 *
	 * The use case is to call requestPathPush and then push().
	 *
	 * @return true
	 */
	@Async("DPumpPusher")
	public Future<Boolean> push()  {

		Path pathToWorkOn = beginWork();
		while( pathToWorkOn != null ) {

			uploadPath(pathToWorkOn);


			// sleep if needed or get the next work item
			// don't sleep while holding a Path to work on.
			long sleepMillis = sleepMillis();
			if( sleepMillis > 0 ) {
				s_log.warn("Will sleep: " + sleepMillis);
				try {
					Thread.sleep(sleepMillis);
				} catch(InterruptedException ex) {
					s_log.error(ex.getMessage() ,ex);
				}
			}

			pathToWorkOn = d_workQueue.beginWork();
		}

		// This value is not checked.
		return new AsyncResult<>(true);
	}

	Path beginWork() {
		return d_workQueue.beginWork();
	}
	 void uploadPath(Path pathToWorkOn) {
		boolean success = false;
		try {
			{
				if( Files.exists(pathToWorkOn)) {

					// This was suspected of leaving file descriptors open but it seems to be ok
					HttpRequest request = HttpRequest.newBuilder()
						.POST(HttpRequest.BodyPublishers.ofFile(pathToWorkOn))
						.uri(URI.create(d_webServerPath))
						.setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
						.header("Content-Type", "application/application/octet-stream")
						.build();
					HttpResponse<String> response = d_httpClient.send(request, HttpResponse.BodyHandlers.ofString());

					// TODO check return code for errors

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
						String.format("PUSH: %s Code: %d Message: %s", pathToWorkOn.getFileName(), code, msg)
					);

					// only delete the file and send success if we got an OK code from the server
					if( code  == HttpURLConnection.HTTP_OK) {
						// If there were no errors then delete the the file
						Files.delete(pathToWorkOn);
						success = true;
					}


				} else {
					// if the file doesn't exist then we treat as success
					success = true;
					s_log.warn(String.format("PUSH: SKIP NONEXISTENT FILE: %s", pathToWorkOn.getFileName().toString()));

				}
			}
		} catch (Throwable ex) {
//					 if something fails then we will not delete the file and it will be retried
			s_log.error(ex.getMessage(), ex);
		} finally {
			if( success ) {
				d_workQueue.endWork(pathToWorkOn);
			} else {
				d_workQueue.handlePathFailed(pathToWorkOn);
			}
		}
	}


}
