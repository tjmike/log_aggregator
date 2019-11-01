package tjmike.logaggregator.agent;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;


// This seems to hang forever
//@RunWith(SpringRunner.class)
@SpringBootTest
public class LogTailResultTests {

	private static final long session = 123L;
	private static final long sequence = 321L;
	private static final String id = "id";
	private static final int nread = 456;
	private static final long oneSecond = 1000L;

	@Test
	public void logTailTests()  {

		LogTailResult logTailResult = new LogTailResult(
			session, id, sequence, nread, LogTail.STATUS.DELETED
		);
		long now = System.currentTimeMillis();
		Assert.assertEquals("Session", session, logTailResult.getSessionID());
		Assert.assertEquals("ID", id, logTailResult.getId());
		Assert.assertEquals("Sequence",sequence, logTailResult.getSeqNum());
		Assert.assertEquals("Status", LogTail.STATUS.DELETED, logTailResult.getLastStatus());
		long ts = logTailResult.getTimestamp();
		long deltaT = now - ts;
		Assert.assertTrue( "Timestamp", deltaT < oneSecond);


	}

}
