package tjmike.logaggregator.datapump;

import java.nio.file.Path;
import java.util.concurrent.Future;

public interface AsyncPusherIF {
	void requestPathPush(Path pathToPush);
	Future<Boolean> push();
}
