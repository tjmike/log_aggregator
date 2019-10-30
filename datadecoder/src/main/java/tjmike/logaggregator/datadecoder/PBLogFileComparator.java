package tjmike.logaggregator.datadecoder;

import java.util.Comparator;

/**
 * Comparator for sorting FileName objects by
 * <ol>
 * <li>Name (this is assumed unique per log file)</li>
 * <li>Session</li>
 * <li>Sequence</li>
 * </ol>
 */
class PBLogFileComparator implements Comparator<PBLogFile> {
	@Override
	public int compare(PBLogFile a, PBLogFile b) {
		int ret = a.getLogFileName().compareTo(b.getLogFileName());
		if( ret != 0 ) return ret;

		ret = Long.compare(a.getSession(), b.getSession());
		if( ret != 0 ) return ret;

		ret = Long.compare(a.getSequence(), b.getSequence());

		return ret;

	}
}
