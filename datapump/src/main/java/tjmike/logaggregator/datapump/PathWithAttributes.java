package tjmike.logaggregator.datapump;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

class PathWithAttributes implements Comparable<PathWithAttributes> {
	private final Path d_path;
	private BasicFileAttributes d_attributes;


	@Override
	public int compareTo(PathWithAttributes o) {
		int ret;

		if( d_attributes != null ) {
			if( o.d_attributes != null ) {
				ret =  d_attributes.lastModifiedTime().compareTo(o.d_attributes.lastModifiedTime());
			} else {
				ret = -1;
			}
		} else {
			ret = o.d_attributes != null ? -1 : 0;
		}
		if( ret == 0 ) {
			ret = d_path.compareTo(o.d_path);
		}
		return ret;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PathWithAttributes)) return false;

		PathWithAttributes that = (PathWithAttributes) o;

		return d_path.equals(that.d_path);
	}

	@Override
	public int hashCode() {
		return d_path.hashCode();
	}

	PathWithAttributes(Path path) {
		d_path = path;
		loadAttributes();
	}

	private void loadAttributes() {

		try {
			d_attributes = Files.readAttributes(d_path, BasicFileAttributes.class);
		} catch(Exception ex) {
			// assume an exception is related to the file not being there

		}

	}
	public Path getPath() {
		return d_path;
	}

	BasicFileAttributes getAttributes() {
		return d_attributes;
	}
}
