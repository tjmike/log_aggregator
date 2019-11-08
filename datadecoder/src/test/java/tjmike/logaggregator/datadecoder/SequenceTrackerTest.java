package tjmike.logaggregator.datadecoder;



import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.nio.file.Path;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SequenceTrackerTest {



	@TempDir
	Path d_testFolder;

//	@Rule
//	public TemporaryFolder d_testFolder = new TemporaryFolder();



	String d_name = "test";
	long d_session = 1234;
	long d_sequence = 321;


	@Test
	@Order(1)
	public void testWriteRead() {

		Assertions.assertEquals(1,1);

		Path cache = d_testFolder.resolve("cache").toAbsolutePath();
		Path rebuilt = d_testFolder.resolve("rebuilt").toAbsolutePath();
		Path testLog = cache.resolve(String.format("%s_%d_%d.pbData",d_name, d_session,d_sequence));


		PathProvider pp = new PathProvider(
			cache.toString(),
			rebuilt.toString()
		);
		pp.init();
		PBLogFile logFile = new PBLogFile(testLog);
		SequenceTracker st = new SequenceTracker(pp);
		st.init();
		st.writeLastIndex(logFile);
		long last = st.getLastIndex(logFile);
		Assertions.assertEquals( d_sequence, last, "Write != Read");


		SequenceTracker st2 = new SequenceTracker(pp);
		st2.init();
		last = st2.getLastIndex(logFile);
		Assertions.assertEquals( d_sequence, last, "INIT Failed: Write != Read");

	}
}
