package tjmike.logaggregator.datapump;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Manage the work queue related to pushing log chunks to the server
 */
class PathForLogChunkWorkList {

	// max # of paths we will manage.
	private static final int s_maxPaths = 1000;

	// ordered list of Paths to process. We try to process them in the order sent
	private final LinkedList<Path> d_workList = new LinkedList<>();
	// This is just a set that is used to track all the Paths in the list
	private final HashSet<Path> d_workListSet = new HashSet<>();

	// The set of paths that we are working on
	private final HashSet<Path> d_processingSet = new HashSet<>();

	synchronized  int getWorkListSize() {
		return d_workList.size();
	}

	synchronized  int checkWorkListVsSetSync() {
		return d_workList.size() - d_workListSet.size();
	}


	/**
	 * Add a path to the work queue. If the path is already in the work list or being processed
	 * then it is ignored. It will also be ignored if the queue size is too big.
	 * If a path is ignored it will get picked up on the next directory scan.
	 *
	 * @param p  - path too add
	 */
	synchronized void addPath(Path p ) {
		if( d_workListSet.size() <  s_maxPaths && !d_workListSet.contains(p) && !d_processingSet.contains(p)) {
			d_workListSet.add(p);
			d_workList.add(p);
		}
	}

	/**
	 * Take the next task to be processed. If there are no tasks null is returned.
	 * @return task or null
	 */
	synchronized Path beginWork( ) {
		if (d_workList.size() > 0) {
			Path next = d_workList.removeFirst();
			d_workListSet.remove(next);

			d_processingSet.add(next);
			return next;
		}
		return null;
	}

	/**
	 * Declare the processing successful. Remove the Path from management.
	 * @param p
	 */
	synchronized void endWork(Path p) {
		d_processingSet.remove(p);
	}

	/**
	 * Declare the path processing as failed. Requeue the path to be worked on again.
	 * @param p
	 */
	synchronized void handlePathFailed(Path p) {
		if( d_processingSet.remove(p) ) {
			d_workList.addFirst(p);
			d_workListSet.add(p);
		}
	}

	int getMaxPaths() {
		return s_maxPaths;
	}
}
