package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Singleton;

public class StorageModule extends AbstractModule {

  private static final StorageModule instance = new StorageModule();

  private ExecutorService executorService;
  private Storage storage;

  private StorageModule() {
  }

  public static StorageModule getInstance() {
    return instance;
  }

  @Provides
  @StorageNumThreads
  int provideStorageNumThreads() {
    return 4;
  }

  @Provides
  @Singleton
  @StorageExecutorService
  ExecutorService provideExecutorService(@StorageNumThreads int numThreads) {
    if (executorService == null) {
      executorService = Executors.newFixedThreadPool(numThreads);
    }
    return executorService;
  }

  @Provides
  @Singleton
  Storage provideStorage(StorageSegmentManager storageSegmentManager) {
    if (storage == null) {
      storage = new StorageImpl(storageSegmentManager);
    }
    return storage;
  }

}
