package tjmike.logaggregator.datapump;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;


@SpringBootTest
public class PathPusherRunnableTest {

	@Rule
	public TemporaryFolder d_testFolder = new TemporaryFolder();

	@Test
	public void pathForLogChunkWorkListTest() throws IOException  {

		Path cacheDir = d_testFolder.newFolder("cacheDir").toPath();
		Path testFile = cacheDir.resolve("data" + DataPumpPusher.s_ProtocolBufferExtension);
		Path testFileA = cacheDir.resolve("data" + DataPumpPusher.s_ProtocolBufferExtension);
		PathWithAttributes pwaTestFile = new PathWithAttributes(testFile);
		PathWithAttributes pwaTestFileA = new PathWithAttributes(testFileA);
		try {
			Thread.sleep(1);
		} catch (Exception ex) {
			// ;
		}
		Path testFile2 = cacheDir.resolve("data2" + DataPumpPusher.s_ProtocolBufferExtension);
		PathWithAttributes pwaTestFile2 = new PathWithAttributes(testFile2);

		Runnable r = () -> { };

		PathPusherRunnable pwaTestFileR = new PathPusherRunnable(pwaTestFile, r);
		PathPusherRunnable pwaTestFileRA = new PathPusherRunnable(pwaTestFileA, r);
		PathPusherRunnable pwaTestFileR2 = new PathPusherRunnable(pwaTestFile2, r);

		int cmp = pwaTestFileR.compareTo(pwaTestFileR2);
		Assert.assertTrue("Compare Test", cmp< 0);

		cmp = pwaTestFileR2.compareTo(pwaTestFileR);
		Assert.assertTrue("Compare Test", cmp >  0);

		cmp = pwaTestFileR.compareTo(pwaTestFileRA);
		Assert.assertEquals("Compare Test", 0, cmp);


	}

}
