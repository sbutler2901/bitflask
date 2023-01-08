package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;

/**
 * Asynchronously submits a delete request to the storage engine and processes the results.
 */
public class DeleteCommand implements ServerCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final StorageCommandDispatcher storageCommandDispatcher;
  private final String key;

  public DeleteCommand(
      ListeningExecutorService listeningExecutorService,
      StorageCommandDispatcher storageCommandDispatcher,
      String key) {
    this.listeningExecutorService = listeningExecutorService;
    this.storageCommandDispatcher = storageCommandDispatcher;
    this.key = key;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public ListenableFuture<String> execute() {
    StorageCommandDTO storageCommandDTO = new DeleteDTO(key);
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
        logger.atWarning().log("Storage failed deleting [%s]: %s", key, failed.message());
        yield String.format("Failed to delete [%s]", key);
      }
    };
  }

  private String catchStorageFailure(Throwable t) {
    logger.atWarning().withCause(t)
        .log("StorageService response threw an unexpected error while deleting [%s]", key);
    return String.format("Unexpected failure deleting [%s]", key);
  }
}
