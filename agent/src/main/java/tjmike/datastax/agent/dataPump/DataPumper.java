package tjmike.datastax.agent.dataPump;

import tjmike.datastax.agent.LogTailResult;

import java.io.IOException;

public interface DataPumper {
	/**
	 * Process the log tail result. A successful return means the result has been
	 * persisted and queued up to be passed to a remote server. The caller can assume
	 * a successful hand-off if there is no Exception. The caller can also mutate the
	 * buffer after the call returns.
	 * @param ltr
	 * @param data
	 * @throws IOException
	 */
	 void process(LogTailResult ltr, byte[] data) throws IOException ;
}
