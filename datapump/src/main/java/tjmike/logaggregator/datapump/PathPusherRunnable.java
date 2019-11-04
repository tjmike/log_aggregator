package tjmike.logaggregator.datapump;


class PathPusherRunnable implements Runnable, Comparable<PathPusherRunnable> {
	private final PathWithAttributes d_path;
	private final Runnable d_runnable;
	public PathPusherRunnable(PathWithAttributes path, Runnable runnable) {
		d_path = path;
		d_runnable = runnable;
	}

	@Override
	public int compareTo(PathPusherRunnable o) {
		return d_path.compareTo(o.d_path);
	}

	@Override
	public void run() {
		d_runnable.run();
	}

	public PathWithAttributes getPath() {
		return d_path;
	}
}
