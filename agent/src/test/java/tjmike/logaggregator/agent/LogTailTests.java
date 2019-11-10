package tjmike.logaggregator.agent;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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



	@Test
	public void rotateLogTest() throws Exception {

		File testRoot = d_testFolder.getRoot();
		Path testParent = testRoot.toPath();
		Path tailFile = testParent.resolve("test.log");
		Path tailFileRotated = testParent.resolve("test.log.1");
		Files.createFile(tailFile);
		LogTail lt = new LogTail(tailFile,1L);

		byte data [] = new byte[2048];
		Files.write(tailFile, data);



		byte [] buff = new byte[1024];

		// read first 1024 bytes
		LogTailResult res = lt.poll(buff);
		Assert.assertEquals("Read full buffer", buff.length, res.getNumberRead() );

		// move the file
		Files.move(tailFile, tailFileRotated, StandardCopyOption.ATOMIC_MOVE);

		// Create a new file
		Files.createFile(tailFile);
		Files.write(tailFile, data);


		// read next 1024 bytes from OLD File
		res = lt.poll(buff);
		Assert.assertEquals("Read full buffer", res.getNumberRead(), buff.length);

		// read next 1024 bytes from NEW file
		res = lt.poll(buff);
		Assert.assertEquals("Read full buffer", res.getLastStatus(), LogTail.STATUS.MOVED);

		// read next 1024 bytes from NEW file
		res = lt.poll(buff);
		Assert.assertEquals("Read full buffer", res.getLastStatus(), LogTail.STATUS.NEWDATA);

		// read no bytes
		res = lt.poll(buff);
		Assert.assertEquals("Read full buffer", res.getLastStatus(), LogTail.STATUS.UNCHANGED);


	}



}
