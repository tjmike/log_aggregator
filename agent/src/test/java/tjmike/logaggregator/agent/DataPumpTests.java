package tjmike.logaggregator.agent;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.context.SpringBootTest;
import tjmike.logaggregator.agent.dataPump.DataPumpImpl;
import tjmike.logaggregator.agent.dataPump.DataPumpInterface;
import tjmike.logaggregator.proto.LoggerProtos;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

// This seems to hang forever
//@RunWith(SpringRunner.class)
@SpringBootTest
public class DataPumpTests {


	@Rule
	public TemporaryFolder d_testFolder = new TemporaryFolder();

	@Test
	public void invalidCacheDirTest() throws Exception {

		File testRoot = d_testFolder.getRoot();
		Path testParent = testRoot.toPath();
		Path cacheDir  = testParent.resolve("cacheDirTest");
		Path logFilePath  = testParent.resolve("logTest.log");
		ArrayList<String> logs = new ArrayList<>(1);
		logs.add(logFilePath.toString());
		PathProvider pp = new PathProvider(
			logs,
			cacheDir.toString()
		);

		pp.init();

		DataPumpInterface dp = new DataPumpImpl(
			pp
		);

		byte [] buff = new byte[1024];

		String fName = "test.log";
		long sessionID = 123L;
		long sequence = 1;
		int bytes = 100;
		LogTail.STATUS status = LogTail.STATUS.NEWDATA;

		LogTailResult ltr = new LogTailResult(
			sessionID, fName, sequence, bytes, status
		);
		dp.process(ltr,buff);

		String resultFile = DataPumpImpl.generateFileName(ltr);

		Path resultPath = cacheDir.resolve(resultFile);

		boolean exists  = Files.exists(resultPath);

		Assert.assertTrue("File exists", exists);


		byte[] all = Files.readAllBytes(resultPath);

		LoggerProtos.LogPart lp = LoggerProtos.LogPart.parseFrom(all);


		Assert.assertEquals("File Size", 100, lp.getPayload().size());

		Assert.assertEquals("FileName/ID",lp.getId(),fName );
		Assert.assertEquals("Session",lp.getSession(),sessionID );
		Assert.assertEquals("Sequence",lp.getSeq(),sequence );
	}




}
