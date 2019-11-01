package tjmike.logaggregator.agent;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

// This seems to hang forever
//@RunWith(SpringRunner.class)
@SpringBootTest
public class PathProviderTests {


	@Rule
	public TemporaryFolder d_testFolder = new TemporaryFolder();

	@Test
	public void invalidCacheDirTest() throws Exception {

		File testRoot = d_testFolder.getRoot();
		Path testParent = testRoot.toPath();
		Path cacheDir = testParent.resolve("cacheDirTest");
		PathProvider pathProvider = createPathProvider();

		Files.createFile(cacheDir);
		try {
			pathProvider.init();
			Assert.fail("Expected an Exception");
		} catch (IOException ex) {
			// do nothing, we expect an exception
		}
	}

	@Test
	public void createCacheDirTest() throws Exception {

		File testRoot = d_testFolder.getRoot();
		Path testParent = testRoot.toPath();
		Path cacheDir = testParent.resolve("cacheDirTest");
		PathProvider pathProvider = createPathProvider();

		Files.deleteIfExists(cacheDir);
		try {
			pathProvider.init();

			if( !Files.exists(pathProvider.getLogCacheDir())) {
				Assert.fail("Directory not created");
			}
			if( !Files.isDirectory(pathProvider.getLogCacheDir())) {
				Assert.fail("File is not a directory");
			}

		} catch(IOException ex) {
			Assert.fail(ex.getMessage());
		}
	}


	private PathProvider createPathProvider() {
		File testRoot = d_testFolder.getRoot();
		Path testParent = testRoot.toPath();

		Path cacheDir  = testParent.resolve("cacheDirTest");
		Path logFilePath  = testParent.resolve("logTest.log");

		ArrayList<String> logs = new ArrayList<>(1);
		logs.add(logFilePath.toString());


		return new PathProvider(
			logs,
			cacheDir.toString()
		);
	}

}
