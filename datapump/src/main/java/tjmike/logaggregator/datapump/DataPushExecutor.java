package tjmike.logaggregator.datapump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
@Service("DPumpPusher")
class DataPushExecutor implements Executor {

	private final int d_maxSize = 16;
	private final int d_coreThreadSize = 5;
	private final int d_maxThreadsSize = 5;


	// Since this queue is unbounded it doesn't make sense for maxThreads > coreThreads. The threadpool
	// will never grow because the queue will never become full.
	private final PriorityBlockingQueue<Runnable> d_queue = new PriorityBlockingQueue<>(d_maxSize);

	// These  classes are used to help manage the priority queue.
	// We track what's in queue (waiting) and whats running
	private final ReentrantLock d_lock = new ReentrantLock();
	private final TreeSet<PathPusherRunnable> d_waiting = new TreeSet<>();
	private final TreeSet<PathPusherRunnable> d_running = new TreeSet<>();

	private final ThreadPoolExecutor d_threadPoolExecutor;

	private static final Logger s_log = LoggerFactory.getLogger(DataPushExecutor.class);

	public DataPushExecutor() {

		CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("DPumpPusher-");
		d_threadPoolExecutor= new ThreadPoolExecutor(
			d_coreThreadSize, d_maxThreadsSize,1, TimeUnit.MINUTES,d_queue,threadFactory,

			((r, executor) -> {
				// ignore it
			})
		) {

			@Override
			protected void beforeExecute(Thread t, Runnable r) {
				moveFromWaitingToRunning((PathPusherRunnable)r);
				super.beforeExecute(t, r);
			}

			@Override
			protected void afterExecute(Runnable r, Throwable t) {
				PathPusherRunnable ppr = (PathPusherRunnable) r;
				boolean removed  = removeFromRunning(ppr);
				if( !removed ) {
					s_log.warn(String.format("Expected to remove %s but was not found", ppr.getPath().getPath().getFileName().toString()));
				}
				super.afterExecute(r, t);
			}

		};

	}



	private void moveFromWaitingToRunning(PathPusherRunnable ppr) {
		d_lock.lock();
		try {
			d_waiting.remove(ppr);
			d_running.add(ppr);
		} finally {
			d_lock.unlock();
		}
	}
	private boolean removeFromRunning(PathPusherRunnable ppr) {
		boolean removed;
		d_lock.lock();
		try {
			removed = d_running.remove(ppr);
		} finally {
			d_lock.unlock();
		}
		return removed;
	}


	/**
	 * Add the path to the waiting set. If necessary, remove the oldest item from the waiting set
	 * AND from the actual  queue.
	 * @param ppr
	 * @return true if we are now tracking this and it should be submitted to the pool
	 */
	private boolean addToWaiting(PathPusherRunnable ppr) {
		boolean added ;
		d_lock.lock();
		try {
			// If we're already working on it then say skip
			if( d_waiting.contains(ppr) || d_running.contains(ppr)) {
				if( s_log.isDebugEnabled()) {
					s_log.debug("Already working on: " + ppr.getPath().getPath().getFileName().toString());
				}
				return false;
			}
			if( d_waiting.size() >= d_maxSize) {
				// We need to remove one before we add one
				PathPusherRunnable last = d_waiting.last();

				// only remove if ours is newer
				if( ppr.compareTo(last ) < 0 ) {
					// we already know it's not there and that last exists and needs to be removed
					d_waiting.add(ppr);
					d_waiting.remove(last);
					added = true;
					// mirror the remove to the queue, note that it's possible that the
					// item was already removed from the queue. If was not in our
					// waiting or running sets then it must be that it completed running already.
					// This is ok.
					if( d_queue.remove(last) ) {
						s_log.info(String.format(
							"Remove from queue - newer item has priority: added: %s removed: %s",
								ppr.getPath().getPath().getFileName().toString(),
								last.getPath().getPath().getFileName().toString()
							)
						);
					} else {
						s_log.info("Remove from queue - item already gone: " + last.getPath().getPath().getFileName().toString());
					}
				} else {
					if( s_log.isDebugEnabled() ) {
						s_log.info("IGNORE queue full of higher priority items: " + last.getPath().getPath().getFileName().toString());
					}
					added = false;
				}
			} else {
				added = d_waiting.add(ppr);
			}
		} finally {
			d_lock.unlock();
		}
		return added;
	}


	public void execute(Runnable command) {

		// Only accept PathPusherRunnable
		if( !(command instanceof PathPusherRunnable)) {
			throw new RejectedExecutionException("Not a PathPusherRunnable");
		}
		PathPusherRunnable r = (PathPusherRunnable)command;

		if( addToWaiting(r)) {
			if( s_log.isDebugEnabled( ) ) {
				s_log.debug("DataPushExecutor : ADDING : " + r.getPath().getPath().getFileName().toString());
			}
			d_threadPoolExecutor.execute(r);
		} else {
			if( s_log.isDebugEnabled()) {
				s_log.info("DataPushExecutor :  ignored: " + r.getPath().getPath().getFileName().toString());
			}
		}
	}
}
