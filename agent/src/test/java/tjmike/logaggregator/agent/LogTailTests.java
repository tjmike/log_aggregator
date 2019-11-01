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
public class LogTailTests {


	@Rule
	public TemporaryFolder d_testFolder = new TemporaryFolder();

	@Test
	public void invalidCacheDirTest() throws Exception {

		File testRoot = d_testFolder.getRoot();
		Path testParent = testRoot.toPath();
		Path tailFile = testParent.resolve("test.log");
		Files.createFile(tailFile);

		LogTail lt = new LogTail(tailFile,1L);

		byte [] buff = new byte[1024];
		LogTailResult res = lt.poll(buff);

		Assert.assertEquals("sessionID", res.getSessionID(),1L);

	}




}
