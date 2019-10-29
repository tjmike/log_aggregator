package tjmike.datastax.datadecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

//interface Collector<T, A, R> {...}
//
//    T – the type of objects that will be available for collection,
//    A – the type of a mutable accumulator object,
//    R – the type of a final result

/**
 * This collector aggregates FileName objects by name,then sessionID (this is the start time of the session)
 * and then the sequence. This means that you can iterate this data structure in order to reassemble the
 * log file. You can also pull out different log files or logfile/session versions.
 */
class FileNameCollector implements Collector<
	FileName,
	TreeMap<String,TreeMap<Long, TreeMap<Long, FileName>>>,
	TreeMap<String,TreeMap<Long,TreeMap<Long, FileName>>>
	>
	{
		private static Logger s_log = LoggerFactory.getLogger(FileNameCollector.class);

		@Override
		public Supplier<TreeMap<String, TreeMap<Long, TreeMap<Long, FileName>>>> supplier() {
			return TreeMap::new;
		}


		/**
		 * This map structure is:
		 * LogFileName
		 *     Session (ordered)
		 *         Sequence (ordered)
		 *
		 * We are adding FileName components to this map
		 * @return
		 */
		@Override
		public BiConsumer<TreeMap<String, TreeMap<Long, TreeMap<Long, FileName>>>, FileName> accumulator() {
			return (acc,fName) -> {
				String logFileName = fName.getLogFileName();
				long session = fName.getSession();
				long seq = fName.getSequence();


				TreeMap<Long,TreeMap<Long, FileName>> forName = acc.computeIfAbsent(
					logFileName,  (f) -> new TreeMap<>()
				);


				TreeMap<Long, FileName> forSession = forName.computeIfAbsent(
					session,  (f) -> new TreeMap<>()
				);

				FileName removed = forSession.put(seq, fName);
				if( removed != null ) {
					s_log.error("FileName Found Multiple Times: " + fName);
				}
			};
		}

		@Override
		public BinaryOperator<TreeMap<String, TreeMap<Long, TreeMap<Long, FileName>>>> combiner() {

			return (a,b) -> {

				for( Map.Entry<String, TreeMap<Long, TreeMap<Long, FileName>>> entriesB : b.entrySet()) {
					String logFileNameB = entriesB.getKey();
					if( !a.containsKey(logFileNameB)) {
						a.put(logFileNameB, entriesB.getValue());
					} else {
						// We have some entries for this logfile
						TreeMap<Long, TreeMap<Long, FileName>> forLogFileA = a.get(logFileNameB);


						TreeMap<Long, TreeMap<Long, FileName>>  bySessionB = entriesB.getValue();
						for( Map.Entry<Long, TreeMap<Long, FileName>> entriesSessionB : bySessionB.entrySet()) {
							Long session = entriesSessionB.getKey();
							if( !forLogFileA.containsKey(session)) {
								forLogFileA.put(session, entriesSessionB.getValue());
							} else {
								// log file a has this session - we should be able to just put ALL the B
								// values into the a one
								TreeMap<Long, FileName> bySequenceIDA = forLogFileA.get(session);
								bySequenceIDA.putAll(entriesSessionB.getValue());
							}
						}
					}
				}
				return a;
			};

//			throw new UnsupportedOperationException("Parallel accumulation not supported");
		}

		@Override
		public Function<TreeMap<String, TreeMap<Long, TreeMap<Long, FileName>>>, TreeMap<String, TreeMap<Long, TreeMap<Long, FileName>>>> finisher() {
			return Function.identity();
		}

		@Override
		public Set<Characteristics> characteristics() {
			HashSet<Characteristics> ret = new HashSet<>();
			ret.add(Characteristics.UNORDERED);
			return ret;
		}
	}
