package tjmike.datastax.datadecoder;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FileName implements Comparable<FileName> {
	private Path d_originalPath;
	private String d_logFileName;
	private long d_session;
	private long d_sequence;

	// TODO centralize .pbData references
	private static final Pattern s_compilePattern = Pattern.compile("(.*)_(\\d*)_(\\d*).pbData");


	FileName(Path pbDataFilePath) {
		d_originalPath = pbDataFilePath;
		Matcher mm = s_compilePattern.matcher(pbDataFilePath.getFileName().toString());
		if( mm.matches()) {
			d_logFileName = mm.group(1);
			d_session = Long.parseLong(mm.group(2));
			d_sequence = Long.parseLong(mm.group(3));
		}
//		LoggerFactory.getLogger(FileName.class).warn("CREATE: " + this);
	}

	Path getOriginalPath() {
		return d_originalPath;
	}

	String getLogFileName() {
		return d_logFileName;
	}

	long getSession() {
		return d_session;
	}

	long getSequence() {
		return d_sequence;
	}

	@Override
	public String toString() {
		return d_logFileName + " :: " + d_session + " :: " + d_sequence;
	}

	@Override
	public int compareTo(FileName o) {
		int ret = d_logFileName.compareTo(o.d_logFileName);
		if( ret != 0 ) { return ret; }

		ret = Long.compare(d_session, o.d_session);
		if( ret != 0 ) { return ret; }

		return Long.compare(d_sequence, o.d_sequence);


	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof FileName)) return false;

		FileName fileName = (FileName) o;

		if (d_session != fileName.d_session) return false;
		if (d_sequence != fileName.d_sequence) return false;
		return d_logFileName.equals(fileName.d_logFileName);
	}

	@Override
	public int hashCode() {
		int result = d_logFileName.hashCode();
		result = 31 * result + (int) (d_session ^ (d_session >>> 32));
		result = 31 * result + (int) (d_sequence ^ (d_sequence >>> 32));
		return result;
	}
}
