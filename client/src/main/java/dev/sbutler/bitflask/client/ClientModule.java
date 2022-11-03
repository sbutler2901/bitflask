package dev.sbutler.bitflask.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.client_processing.ClientProcessingModule;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.resp.network.RespNetworkModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ClientModule extends AbstractModule {

  public static ClientModule create(ClientConfiguration configuration,
      ConnectionManager connectionManager) {
    return new ClientModule(configuration, connectionManager);
  }

  private final ClientConfiguration configuration;
  private final ConnectionManager connectionManager;

  private ClientModule(ClientConfiguration configuration, ConnectionManager connectionManager) {
    this.configuration = configuration;
    this.connectionManager = connectionManager;
  }

  @Override
  protected void configure() {
    install(new RespNetworkModule());
    install(new ClientProcessingModule());
  }

  @Provides
  ClientConfiguration provideClientConfiguration() {
    return configuration;
  }

  @Provides
  InputStream provideInputStream() throws IOException {
    return connectionManager.getInputStream();
  }

  @Provides
  OutputStream provideOutputStream() throws IOException {
    return connectionManager.getOutputStream();
  }
}
