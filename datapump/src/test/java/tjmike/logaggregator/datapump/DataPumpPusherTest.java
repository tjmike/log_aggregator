package tjmike.logaggregator.datapump;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


@SpringBootTest
public class DataPumpPusherTest {

	@Rule
	public TemporaryFolder d_testFolder = new TemporaryFolder();

	@Test
	public void pathForLogChunkWorkListTest() throws IOException  {

		final PathWithAttributes[] requestedPush = {null,null};

		AsyncPusherIF mock = new AsyncPusherIF() {
			@Override
			public void uploadPath(PathWithAttributes path) {
				if( requestedPush[0] == null ) requestedPush[0] = path;
				else requestedPush[1] = path;
			}
		};

		Path cacheDir = d_testFolder.newFolder("cacheDir").toPath();
		Path testFile = cacheDir.resolve("data" + DataPumpPusher.s_ProtocolBufferExtension);
		PathWithAttributes pwaTestFile = new PathWithAttributes(testFile);

		Files.createFile(testFile);

		DataPumpPusher pp = new DataPumpPusher(
			cacheDir.toString(),
			mock
		);

		pp.processDirectory();

		Assert.assertNotNull("Path requested", requestedPush[0]);
		Assert.assertTrue("Path matches", requestedPush[0].equals(pwaTestFile));



		requestedPush[0] = null;
		requestedPush[1] = null;
		Path testFile2 = cacheDir.resolve("data2" + DataPumpPusher.s_ProtocolBufferExtension);
		Files.createFile(testFile2);
		List<Path> files = Files.list(cacheDir).collect(Collectors.toList());


		PathWithAttributes pwaTestFile2 = new PathWithAttributes(testFile2);
		pp.processDirectory();

		Assert.assertNotNull("Path requested 2", requestedPush[1]);
		Assert.assertEquals("Path matches 2", requestedPush[1], pwaTestFile2);





	}

}
