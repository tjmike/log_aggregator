package tjmike.logaggregator.datapump;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@SpringBootTest
public class DataPumpPusherTest {



	@Rule
	public TemporaryFolder d_testFolder = new TemporaryFolder();

	@Test
	public void pathForLogChunkWorkListTest() throws IOException  {

		final Path[] requestedPush = {null};
		final boolean []  pushCalled = {false};

		AsyncPusherIF mock = new AsyncPusherIF() {
			@Override
			public void requestPathPush(Path pathToPush) {
				requestedPush[0] = pathToPush;
			}

			@Override
			public Future<Boolean> push() {
				pushCalled[0] = true;
				return new Future<Boolean>() {
					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}

					@Override
					public boolean isDone() {
						return true;
					}

					@Override
					public Boolean get() throws InterruptedException, ExecutionException {
						return true;
					}

					@Override
					public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
						return true;
					}
				};
			}
		};

		Path cacheDir = d_testFolder.newFolder("cacheDir").toPath();
		Path testFile = cacheDir.resolve("data" + DataPumpPusher.s_ProtocolBufferExtension);
		Files.createFile(testFile);

		DataPumpPusher pp = new DataPumpPusher(
			cacheDir.toString(),
			mock
		);

		pp.processDirectory();

		Assert.assertNotNull("Path requested", requestedPush[0]);
		Assert.assertTrue("Path matches", requestedPush[0].equals(testFile));


	}

}
