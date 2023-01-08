package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;

/**
 * Asynchronously submits a write request to the storage engine and processes the results.
 */
class SetCommand implements ServerCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final StorageCommandDispatcher storageCommandDispatcher;
  private final String key;
  private final String value;

  public SetCommand(ListeningExecutorService listeningExecutorService,
      StorageCommandDispatcher storageCommandDispatcher,
      String key, String value) {
    this.listeningExecutorService = listeningExecutorService;
    this.storageCommandDispatcher = storageCommandDispatcher;
    this.key = key;
    this.value = value;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public ListenableFuture<String> execute() {
    StorageCommandDTO storageCommandDTO = new WriteDTO(key, value);
    ListenableFuture<StorageResponse> storageResponseFuture =
        storageCommandDispatcher.put(storageCommandDTO);

    return FluentFuture.from(storageResponseFuture)
        .transform(this::transformStorageResponse, listeningExecutorService)
        .catching(Throwable.class, this::catchStorageFailure, listeningExecutorService);
  }

  private String transformStorageResponse(StorageResponse storageResponse) {
    return switch (storageResponse) {
      case StorageResponse.Success success -> success.message();
      case StorageResponse.Failed failed -> {
        logger.atWarning()
            .log("Storage failed writing [%s]:[%s]. %s", key, value, failed.message());
        yield String.format("Failed to write [%s]:[%s]", key, value);
      }
    };
  }

  private String catchStorageFailure(Throwable e) {
    logger.atWarning().withCause(e)
        .log("StorageService response threw an unexpected error while writing [%s]:[%s]", key,
            value);
    return String.format("Unexpected failure writing [%s]:[%s]", key, value);
  }
}
