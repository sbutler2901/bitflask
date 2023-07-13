package dev.sbutler.bitflask.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.google.inject.Provides;
import dev.sbutler.bitflask.common.guice.RootModule;
import dev.sbutler.bitflask.common.io.FilesHelper;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.lsm.LSMTreeModule;
import jakarta.inject.Singleton;
import java.util.concurrent.ThreadFactory;

/** The root Guice module for executing the StorageService */
public class StorageServiceModule extends RootModule {

  private final StorageConfigurations storageConfigurations;

  public StorageServiceModule(StorageConfigurations storageConfigurations) {
    this.storageConfigurations = storageConfigurations;
  }

  @Override
  protected void configure() {
    install(new LSMTreeModule());
  }

  public ImmutableSet<Service> getServices(Injector injector) {
    return ImmutableSet.of(injector.getInstance(StorageService.class));
  }

  @Provides
  StorageConfigurations provideStorageConfiguration() {
    return storageConfigurations;
  }

  @Provides
  @Singleton
  StorageCommandDispatcher provideStorageCommandDispatcher(
      StorageConfigurations storageConfigurations) {
    return new StorageCommandDispatcher(storageConfigurations.getDispatcherCapacity());
  }

  @Provides
  FilesHelper provideFilesHelper(ThreadFactory threadFactory) {
    return new FilesHelper(threadFactory);
  }
}
