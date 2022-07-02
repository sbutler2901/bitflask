package dev.sbutler.bitflask.storage;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

final class StorageImpl implements Storage {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentManager segmentManager;

  @Inject
  public StorageImpl(@StorageExecutorService ListeningExecutorService executorService,
      SegmentManager segmentManager) {
    this.executorService = executorService;
    this.segmentManager = segmentManager;
  }

  public Future<Void> write(String key, String value) {
    validateWriteArgs(key, value);
    Callable<Void> writeTask = () -> {
      segmentManager.write(key, value);
      return null;
    };
    logger.atInfo().log("Submitting write for [%s] : [%s]", key, value);
    return executorService.submit(writeTask);
  }

  private void validateWriteArgs(String key, String value) {
    checkNotNull(key);
    checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
    checkArgument(key.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        key.length());
    checkNotNull(value);
    checkArgument(!value.isBlank(), "Expected non-blank key, but was [%s]", value);
    checkArgument(value.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        value.length());
  }

  public Future<Optional<String>> read(String key) {
    validateReadArgs(key);
    Callable<Optional<String>> readTask = () -> segmentManager.read(key);
    logger.atInfo().log("Submitting read for [%s]", key);
    return executorService.submit(readTask);
  }

  private void validateReadArgs(String key) {
    checkNotNull(key);
    checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
    checkArgument(key.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        key.length());
  }

  @Override
  public void shutdown() throws InterruptedException {
    boolean wasShutdownBeforeTermination;
    try {
      executorService.shutdown();
      wasShutdownBeforeTermination = executorService.awaitTermination(10L, TimeUnit.SECONDS);
    } finally {
      segmentManager.close();
    }

    if (!wasShutdownBeforeTermination) {
      executorService.shutdownNow();
    }
  }

}
