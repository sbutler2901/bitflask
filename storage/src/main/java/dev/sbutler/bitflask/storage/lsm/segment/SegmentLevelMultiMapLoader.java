package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import javax.inject.Inject;

public final class SegmentLevelMultiMapLoader {

  private final SegmentLoader segmentLoader;
  private final SegmentIndexLoader segmentIndexLoader;

  @Inject
  SegmentLevelMultiMapLoader(SegmentLoader segmentLoader, SegmentIndexLoader segmentIndexLoader) {
    this.segmentLoader = segmentLoader;
    this.segmentIndexLoader = segmentIndexLoader;
  }

  public SegmentLevelMultiMap load() {
    // Load SegmentIndexes
    ImmutableList<SegmentIndex> indexes = segmentIndexLoader.load();
    ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap =
        mapIndexesBySegmentNumber(indexes);

    // Load Segments
    ImmutableList<Segment> segments = segmentLoader.loadWithIndexes(segmentNumberToIndexMap);

    return SegmentLevelMultiMap.create(mapSegmentsBySegmentLevel(segments));
  }

  private ImmutableListMultimap<Integer, Segment> mapSegmentsBySegmentLevel(
      ImmutableList<Segment> segments) {
    return segments.stream().collect(toImmutableListMultimap(Segment::getSegmentLevel, identity()));
  }

  private ImmutableMap<Integer, SegmentIndex> mapIndexesBySegmentNumber(
      ImmutableList<SegmentIndex> indexes) {
    return indexes.stream()
        .collect(toImmutableMap(SegmentIndex::getSegmentNumber, identity(), (i0, i1) -> {
          throw new StorageLoadException(String.format(
              "Duplicate segment number [%d] for SegmentIndexes found", i0.getSegmentNumber()));
        }));
  }
}
