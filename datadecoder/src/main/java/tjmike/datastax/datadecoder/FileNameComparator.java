package tjmike.datastax.datadecoder;

import java.util.Comparator;

/**
 * Comparator for sorting FileName objects by
 * <ol>
 * <li>Name (this is assumed unique per log file)</li>
 * <li>Session</li>
 * <li>Sequence</li>
 * </ol>
 */
class FileNameComparator implements Comparator<FileName> {
	@Override
	public int compare(FileName a, FileName b) {
		int ret = a.getLogFileName().compareTo(b.getLogFileName());
		if( ret != 0 ) return ret;

		ret = Long.compare(a.getSession(), b.getSession());
		if( ret != 0 ) return ret;

		ret = Long.compare(a.getSequence(), b.getSequence());

		return ret;

	}
}
