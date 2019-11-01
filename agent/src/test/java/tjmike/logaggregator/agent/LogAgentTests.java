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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

@SpringBootTest
public class LogAgentTests {


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

		long sequence = 1;
		int bytes = 100;
		byte [] dataBytes = new byte[bytes];
		LogTail.STATUS status = LogTail.STATUS.NEWDATA;


		Files.write(logFilePath, dataBytes, StandardOpenOption.CREATE);

		LogAgent la = new LogAgent(pp,dp);
		la.pollLogs(buff,true);


		LogTailResult ltr = new LogTailResult(
			pp.getSessionID(), logFilePath.getFileName().toString(), sequence, bytes, status
		);

		String resultFile = DataPumpImpl.generateFileName(ltr);

		Path resultPath = cacheDir.resolve(resultFile);

		boolean exists  = Files.exists(resultPath);
		System.out.println("resultPAth = " + resultPath.toString());

		Assert.assertTrue("File exists", exists);


		byte[] all = Files.readAllBytes(resultPath);

		LoggerProtos.LogPart lp = LoggerProtos.LogPart.parseFrom(all);


		Assert.assertEquals("File Size", 100, lp.getPayload().size());
		Assert.assertEquals("FileName/ID",lp.getId(),logFilePath.getFileName().toString());
		Assert.assertEquals("Session",lp.getSession(),pp.getSessionID() );
		Assert.assertEquals("Sequence",lp.getSeq(),sequence );
	}




}
