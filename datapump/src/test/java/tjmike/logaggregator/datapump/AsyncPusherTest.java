package tjmike.logaggregator.datapump;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
public class AsyncPusherTest {

	@Rule
	public TemporaryFolder d_testFolder = new TemporaryFolder();

	@Rule
	public MockServerRule mockServerRule = new MockServerRule(this,8080);


	@Test
	public void asyncWorkerGoodPost() throws IOException  {

		Executor ex = getExecutor();

		Path dataPath = d_testFolder.newFile("data").toPath();
		MockServerClient cl = mockServerRule.getClient();
		int port = mockServerRule.getPort();

		cl
			.when(
				request()
					.withMethod("POST")
					.withPath("/data")
			)
			.respond(
				response()
					.withStatusCode(200)
					.withBody("Throttle: 0")
			);

		AsyncPusher pusher = new AsyncPusher("http://localhost:" + port + "/data",ex);
		PathWithAttributes pwa = new PathWithAttributes(dataPath);
		pusher.uploadPath(pwa);


		Assert.assertEquals("Sleep Millis", pusher.sleepMillis(), 0);
		Assert.assertFalse("File Was Deleted", Files.exists(dataPath));
	}
	@Test
	public void asyncWorkerThrottle() throws IOException  {
		Path dataPath = d_testFolder.newFile("data").toPath();

		Executor ex = getExecutor();

		MockServerClient cl = mockServerRule.getClient();
		int port = mockServerRule.getPort();
		AsyncPusher pusher = new AsyncPusher("http://localhost:" + port + "/data", ex);
		cl.reset();
		cl
			.when(
				request()
					.withMethod("POST")
					.withPath("/data")
			)
			.respond(
				response()
					.withStatusCode(200)
					.withBody("Throttle: 5")
			);

		PathWithAttributes pwa = new PathWithAttributes(dataPath);
		pusher.uploadPath(pwa);
		Assert.assertTrue("Sleep Millis > 0", pusher.sleepMillis()>0);
	}

	@Test
	public void asyncWorkerError() throws IOException  {
		Path dataPath = d_testFolder.newFile("data").toPath();
		Executor ex = getExecutor();
		PathWithAttributes pwa = new PathWithAttributes(dataPath);

		MockServerClient cl = mockServerRule.getClient();
		int port = mockServerRule.getPort();
		AsyncPusher pusher = new AsyncPusher("http://localhost:" + port + "/data", ex);
		cl.reset();
		cl
			.when(
				request()
					.withMethod("POST")
					.withPath("/data")
			)
			.respond(
				response()
					.withStatusCode(503)
					.withBody("Throttle: 5")
			);

		pusher.uploadPath(pwa);
		Assert.assertTrue("Sleep Millis > 0", pusher.sleepMillis()>0);
		Assert.assertTrue("File Was Not Deleted", Files.exists(dataPath));

		pusher.uploadPath(pwa);


	}


	@Test
	public void asyncWorkerReject() throws IOException  {
		Path dataPath = d_testFolder.newFile("data").toPath();

		Executor ex = getExecutor() ;

		PathWithAttributes pwa = new PathWithAttributes(dataPath);

		MockServerClient cl = mockServerRule.getClient();
		int port = mockServerRule.getPort();
		AsyncPusher pusher = new AsyncPusher("http://localhost:" + port + "/data", ex);
		cl.reset();
		cl
			.when(
				request()
					.withMethod("POST")
					.withPath("/data")
			)
			.respond(
				response()
					.withStatusCode(503)
					.withBody("Throttle: 5")
			);

		pusher.uploadPath(pwa);
		Assert.assertTrue("Sleep Millis > 0", pusher.sleepMillis()>0);
		Assert.assertTrue("File Was Not Deleted", Files.exists(dataPath));

		pusher.uploadPath(pwa);


	}

	private Executor getExecutor() {
		return Runnable::run;
	}

}
