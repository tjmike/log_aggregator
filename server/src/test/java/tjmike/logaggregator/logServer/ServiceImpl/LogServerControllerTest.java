package tjmike.logaggregator.logServer.ServiceImpl;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import tjmike.logaggregator.proto.LoggerProtos;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class LogServerControllerTest {


	@Rule
	public TemporaryFolder d_testFolder = new TemporaryFolder();


	private static String s_testID = "TestID";
	private static long s_testSEQ = 0;
	private static long s_testSessionID = 1234;
	private static byte [] s_testPayload = new byte[1024];

	@Test
	public void pathForLogChunkWorkListTest() throws Exception {
		Path cacheDir =  d_testFolder.getRoot().toPath().resolve("cacheDir"); // newFolder("cacheDir").toPath();

		LogServerController ctrl = new LogServerController(
			cacheDir.toString()
		);

		ctrl.initCachePath();


		LoggerProtos.LogPart.Builder lp = LoggerProtos.LogPart.newBuilder();
				lp.setId(s_testID);
				lp.setSeq(s_testSEQ);
				lp.setSession(s_testSessionID);
		lp.setPayload( ByteString.copyFrom(s_testPayload,0,s_testPayload.length));
		LoggerProtos.LogPart protoBuff =  lp.build();


		ctrl.acceptData(new ByteArrayInputStream(protoBuff.toByteArray()));

		String fileName = ctrl.generateFileName(protoBuff);

		Path expectedFile = cacheDir.resolve(fileName);
		Assert.assertTrue("File created", Files.exists(expectedFile));

		try(InputStream is = Files.newInputStream(expectedFile)) {
			LoggerProtos.LogPart rebuilt = LoggerProtos.LogPart.parseFrom(is);

			Assert.assertEquals("SequenceID", rebuilt.getSeq(), protoBuff.getSeq());
			Assert.assertEquals("SessionID", rebuilt.getSession(), protoBuff.getSession());
			Assert.assertEquals("ID", rebuilt.getId(), protoBuff.getId());

			byte [] sent = protoBuff.getPayload().toByteArray();
			byte [] got = rebuilt.getPayload().toByteArray();

			Assert.assertTrue("Buffers", equals(sent, got));

		}

	}

	private boolean equals(byte[] a , byte[] b) {
		// just do the match if we throw and exception the test will fail
		for( int i=0;i<a.length;i++) {
			if( a[i] != b[i]) {
				return false;
			}
		}
		return true;
	}
	@Test
	public void throttleTest() throws Exception {
		Path cacheDir =  d_testFolder.newFolder("cacheDir").toPath();

		LogServerController ctrl = new LogServerController(
			cacheDir.toString()
		);

		String reply = ctrl.setThrottle(100);
		Assert.assertTrue("Set Throttle", (reply.indexOf("100") > 0) );
		reply = ctrl.setThrottle(-1);
		Assert.assertTrue("Check Throttle", (reply.indexOf("100") > 0) );
	}

	@Test
	public void aliveTest() throws Exception {
		Path cacheDir =  d_testFolder.newFolder("cacheDir").toPath();
		LogServerController ctrl = new LogServerController(
			cacheDir.toString()
		);
		String reply = ctrl.index();
		Assert.assertEquals("Index(Alive>", LogServerController.s_AliveMessage, reply );
	}

	@Test
	public void countTest() throws Exception {
		Path cacheDir =  d_testFolder.newFolder("cacheDir").toPath();
		LogServerController ctrl = new LogServerController(
			cacheDir.toString()
		);

		ctrl.initCachePath();;

		LoggerProtos.LogPart.Builder lp = LoggerProtos.LogPart.newBuilder();
				lp.setId(s_testID);
				lp.setSeq(s_testSEQ);
				lp.setSession(s_testSessionID);
		lp.setPayload( ByteString.copyFrom(s_testPayload,0,s_testPayload.length));
		LoggerProtos.LogPart protoBuff =  lp.build();


		ctrl.acceptData(new ByteArrayInputStream(protoBuff.toByteArray()));
		ctrl.acceptData(new ByteArrayInputStream(protoBuff.toByteArray()));
		ctrl.acceptData(new ByteArrayInputStream(protoBuff.toByteArray()));
		ctrl.acceptData(new ByteArrayInputStream(protoBuff.toByteArray()));


		String reply = ctrl.count();
		Assert.assertTrue("Count", (reply.contains("4")) );
	}

}
