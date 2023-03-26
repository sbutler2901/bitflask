package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * Handles compacting all {@link Segment}s in a level.
 */
public final class SegmentLevelCompactor {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ThreadFactory threadFactory;
  private final SegmentFactory segmentFactory;

  @Inject
  SegmentLevelCompactor(ThreadFactory threadFactory, SegmentFactory segmentFactory) {
    this.threadFactory = threadFactory;
    this.segmentFactory = segmentFactory;
  }

  /**
   * Compacts the provided segment level returning the updated
   * {@link dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap}.
   */
  public SegmentLevelMultiMap compactSegmentLevel(
      SegmentLevelMultiMap segmentLevelMultiMap, int segmentLevel) {
    // TODO: evaluate performance
    ImmutableList<Segment> segmentsInLevel = segmentLevelMultiMap.getSegmentsInLevel(segmentLevel);
    ImmutableList<Entry> entriesInLevel = getAllEntriesInLevel(segmentsInLevel);
    ImmutableSortedMap<String, Entry> keyEntryMap =
        EntryUtils.buildImmutableKeyEntryMap(entriesInLevel);

    Segment newSegment;
    try {
      newSegment = segmentFactory.create(keyEntryMap, segmentLevel + 1);
    } catch (IOException e) {
      throw new StorageCompactionException("Failed creating new segment", e);
    }

    logger.atInfo().log("Compacted segment level [%d] removing [%d] duplicate Entries",
        segmentLevel,
        entriesInLevel.size() - keyEntryMap.size());

    return segmentLevelMultiMap.toBuilder()
        .clearSegmentLevel(segmentLevel)
        .add(newSegment)
        .build();
  }

  private ImmutableList<Entry> getAllEntriesInLevel(
      ImmutableList<Segment> segmentsInLevel) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure("compact-segments-level-scope",
        threadFactory)) {
      List<Future<ImmutableList<Entry>>> segmentEntriesFutures =
          new ArrayList<>(segmentsInLevel.size());
      for (var segment : segmentsInLevel) {
        segmentEntriesFutures.add(scope.fork(segment::readAllEntries));
      }

      try {
        scope.join();
        scope.throwIfFailed(e ->
            new StorageCompactionException("Failed getting all entries in segment level", e));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StorageCompactionException("Interrupted while loading segment entries", e);
      }

      return segmentEntriesFutures.stream()
          .map(Future::resultNow)
          .flatMap(ImmutableList::stream)
          .collect(toImmutableList());
    }
  }
}
