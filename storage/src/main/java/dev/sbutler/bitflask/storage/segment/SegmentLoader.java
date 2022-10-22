package dev.sbutler.bitflask.storage.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.mu.util.stream.GuavaCollectors.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.mu.util.stream.BiStream;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageThreadFactory;
import dev.sbutler.bitflask.storage.segment.SegmentFile.Header;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService.ManagedSegments;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Future.State;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

final class SegmentLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageThreadFactory storageThreadFactory;
  private final SegmentFile.Factory segmentFileFactory;
  private final SegmentFactory segmentFactory;
  private final Path storeDirectoryPath;
  private final SegmentLoaderHelper segmentLoaderHelper;

  @Inject
  SegmentLoader(
      StorageThreadFactory storageThreadFactory, SegmentLoaderHelper segmentLoaderHelper,
      SegmentFactory segmentFactory, SegmentFile.Factory segmentFileFactory,
      StorageConfiguration storageConfiguration) {
    this.storageThreadFactory = storageThreadFactory;
    this.segmentLoaderHelper = segmentLoaderHelper;
    this.segmentFactory = segmentFactory;
    this.segmentFileFactory = segmentFileFactory;
    this.storeDirectoryPath = storageConfiguration.getStorageStoreDirectoryPath();
  }

  /**
   * Loads preexisting segments from the filesystem and initializes them for usage. Assumes the
   * directory for storing segments exists.
   *
   * @return the loaded segments
   * @throws SegmentLoaderException if an error occurs while loading the segments
   */
  public ManagedSegments loadExistingSegments() throws SegmentLoaderException {
    logger.atInfo().log("Loading any pre-existing Segments");

    try {
      boolean segmentStoreDirCreated = segmentFactory.createSegmentStoreDir();
      if (segmentStoreDirCreated) {
        logger.atInfo().log("Segment store directory created");
        return createManagedSegments(ImmutableList.of());
      }

      ImmutableList<Path> segmentFilePaths =
          segmentLoaderHelper.getFilePathsInDirectory(storeDirectoryPath);
      if (segmentFilePaths.isEmpty()) {
        logger.atInfo().log("No existing files found in segment store directory");
        return createManagedSegments(ImmutableList.of());
      }

      ImmutableList<Path> sortedSegmentFilePaths = sortFilePathsByLatestModifiedDatesFirst(
          segmentFilePaths);
      ImmutableMap<Path, FileChannel> pathFileChannelMap = openSegmentFileChannels(
          sortedSegmentFilePaths);
      ImmutableList<SegmentFile> segmentFiles = loadSegmentFiles(pathFileChannelMap);
      ImmutableList<Segment> createdSegments = createSegmentsFromSegmentFiles(segmentFiles);
      logger.atInfo().log("Loaded [%d] preexisting segments", createdSegments.size());
      return createManagedSegments(createdSegments);
    } catch (SegmentLoaderException e) {
      throw e;
    } catch (Exception e) {
      throw new SegmentLoaderException("Failed to load existing segments", e);
    }
  }

  private ManagedSegments createManagedSegments(
      ImmutableList<Segment> loadedSegments) throws IOException {
    Segment writableSegment;
    if (loadedSegments.isEmpty()) {
      writableSegment = segmentFactory.createSegment();
    } else {
      writableSegment = loadedSegments.get(0);
      loadedSegments = loadedSegments.subList(1, loadedSegments.size());
    }

    return new ManagedSegments(writableSegment, loadedSegments);
  }

  ImmutableList<Path> sortFilePathsByLatestModifiedDatesFirst(
      ImmutableList<Path> segmentFilePaths) {
    ImmutableMap<Path, Future<FileTime>> pathFileTimeFutures;
    try {
      pathFileTimeFutures = segmentLoaderHelper.getLastModifiedTimeOfFiles(segmentFilePaths);
    } catch (InterruptedException e) {
      throw new SegmentLoaderException("Interrupted while get file modified times", e);
    }

    ImmutableMap<Path, Throwable> failedFileTimePaths =
        getFailedFutureThrowablesFromMap(pathFileTimeFutures);

    if (!failedFileTimePaths.isEmpty()) {
      failedFileTimePaths.forEach((key, value) ->
          logger.atSevere().withCause(value)
              .log("Getting last modified time for file at path [%s] failed.", key));
      throw new SegmentLoaderException("File last modified times had failures");
    }

    ImmutableMap<Path, FileTime> successFileTimePaths =
        getSuccessfulFutureValuesFromMap(pathFileTimeFutures);

    // More recent modified first
    return BiStream.from(successFileTimePaths)
        .sortedByValues(Comparator.reverseOrder())
        .keys()
        .collect(toImmutableList());
  }

  private ImmutableMap<Path, FileChannel> openSegmentFileChannels(ImmutableList<Path> filePaths) {
    ImmutableMap<Path, Future<FileChannel>> pathFutureFileChannelMap;
    try {
      pathFutureFileChannelMap = segmentLoaderHelper.openFileChannels(filePaths,
          segmentFactory.getFileChannelOptions());
    } catch (InterruptedException e) {
      throw new SegmentLoaderException("Interrupted while opening segment file channels", e);
    }

    ImmutableMap<Path, FileChannel> openFileChannels =
        getSuccessfulFutureValuesFromMap(pathFutureFileChannelMap);

    ImmutableMap<Path, Throwable> failedFileChannels =
        getFailedFutureThrowablesFromMap(pathFutureFileChannelMap);

    if (!failedFileChannels.isEmpty()) {
      failedFileChannels.forEach((key, value) -> logger.atSevere().withCause(value)
          .log("Opening FileChannel for [%s] failed", key));
      segmentLoaderHelper.closeFileChannelsBestEffort(openFileChannels.values().asList());
      throw new SegmentLoaderException("Failed opening segment file channels");
    }

    return openFileChannels;
  }

  private ImmutableList<SegmentFile> loadSegmentFiles(
      ImmutableMap<Path, FileChannel> pathFileChannelMap) {
    ImmutableList.Builder<SegmentFile> segmentFiles = new ImmutableList.Builder<>();
    for (Entry<Path, FileChannel> entry : pathFileChannelMap.entrySet()) {
      Path path = entry.getKey();
      FileChannel fileChannel = entry.getValue();
      Header header = getHeaderFromFileChannel(fileChannel);
      SegmentFile segmentFile =
          segmentFileFactory.create(fileChannel, path, header);
      segmentFiles.add(segmentFile);
    }
    return segmentFiles.build();
  }

  private ImmutableList<Segment> createSegmentsFromSegmentFiles(
      ImmutableList<SegmentFile> segmentFiles) {
    ImmutableMap<SegmentFile, Future<Segment>> segmentFileFutureSegmentMap;
    try {
      segmentFileFutureSegmentMap = createSegmentFutures(segmentFiles);
    } catch (InterruptedException e) {
      throw new SegmentLoaderException("Interrupted will creating segments from segment files", e);
    }

    ImmutableMap<SegmentFile, Throwable> failedSegments =
        getFailedFutureThrowablesFromMap(segmentFileFutureSegmentMap);

    if (!failedSegments.isEmpty()) {
      failedSegments.forEach((key, value) -> logger.atSevere().withCause(value)
          .log("Creating segment from segment file with key [%d] failed", key.getSegmentFileKey()));
      segmentFiles.forEach(SegmentFile::close);
      throw new SegmentLoaderException("Failed creating segments from segment files");
    }

    ImmutableMap<SegmentFile, Segment> createdSegments =
        getSuccessfulFutureValuesFromMap(segmentFileFutureSegmentMap);

    return BiStream.from(createdSegments)
        .values()
        .collect(toImmutableList());
  }

  /**
   * Creates {@link Segment}s from the provided {@link SegmentFile}s.
   *
   * <p>The futures in the returned map are guaranteed to be done, i.e., {@link Future#isDone()}
   * will return {@code true}. Handling the state of the future ({@link Future#state()}) is the
   * responsibility of the caller.
   */
  private ImmutableMap<SegmentFile, Future<Segment>> createSegmentFutures(
      ImmutableList<SegmentFile> segmentFiles) throws InterruptedException {
    ImmutableMap.Builder<SegmentFile, Future<Segment>> segmentFileFutureSegmentMap = new ImmutableMap.Builder<>();
    try (var scope = new StructuredTaskScope.ShutdownOnFailure("create-segments",
        storageThreadFactory)) {
      ImmutableMap.Builder<SegmentFile, Future<Segment>> builder = new ImmutableMap.Builder<>();
      for (SegmentFile segmentFile : segmentFiles) {
        Callable<Segment> segmentCallable = () -> segmentFactory.createSegmentFromFile(segmentFile);
        builder.put(segmentFile, scope.fork(segmentCallable));
      }
      scope.join();
    }
    return segmentFileFutureSegmentMap.build();
  }

  private static <K, V> ImmutableMap<K, V> getSuccessfulFutureValuesFromMap(
      ImmutableMap<K, Future<V>> futureMap) {
    return BiStream.from(futureMap)
        .filterValues(f -> f.state() == State.SUCCESS)
        .mapValues(Future::resultNow)
        .collect(toImmutableMap());
  }

  private static <K, V> ImmutableMap<K, Throwable> getFailedFutureThrowablesFromMap(
      ImmutableMap<K, Future<V>> futureMap) {
    return BiStream.from(futureMap)
        .filterValues(f -> f.state() != State.SUCCESS)
        .mapValues(Future::exceptionNow)
        .collect(toImmutableMap());
  }

  private static Header getHeaderFromFileChannel(FileChannel fileChannel) {
    try {
      return Header.readHeaderFromFileChannel(fileChannel);
    } catch (IOException e) {
      throw new SegmentLoaderException("Failed getting SegmentFile.Header from the FileChannel", e);
    }
  }
}
