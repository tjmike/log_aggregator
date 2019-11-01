package tjmike.logaggregator.datapump;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

@SpringBootTest
public class PathForLogChunkWorkListTest {


	@Test
	public void pathForLogChunkWorkListTest() {


		PathForLogChunkWorkList pwl = new PathForLogChunkWorkList();
		Path p = Path.of("0");
		for(int i=0;i<100;i++) {
			pwl.addPath(p);
			p = Path.of("0");
		}

		Assert.assertEquals("No dupe paths", pwl.getWorkListSize(), 1);
		for(int i=0;i<(pwl.getMaxPaths()+10);i++) {
			pwl.addPath(p);
			p = Path.of(String.format("%d",i));
		}
		Assert.assertEquals("Max Path Size" , pwl.getWorkListSize(), pwl.getMaxPaths());

		Assert.assertEquals("Work Data Sync", pwl.checkWorkListVsSetSync(),0);

		p = pwl.beginWork();
		int expect = 0;
		while( p != null ) {
			Assert.assertEquals("Ordering", Integer.parseInt(p.toString()),expect);
			pwl.endWork(p);
			p = pwl.beginWork();
			expect++;
		}

		for(int i=0;i<(pwl.getMaxPaths()+10);i++) {
			pwl.addPath(p);
			p = Path.of(String.format("%d",i));
		}

		Path pp  = pwl.beginWork();
		pwl.handlePathFailed(pp);
		p = pwl.beginWork();

		Assert.assertEquals("Test Failed To Head", pp, p);

	}

}
