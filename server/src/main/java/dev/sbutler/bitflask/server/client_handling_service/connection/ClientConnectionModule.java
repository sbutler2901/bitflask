package dev.sbutler.bitflask.server.client_handling_service.connection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ClientConnectionModule extends AbstractModule {

  @Provides
  InputStream provideInputStream(ClientConnectionManagerImpl connectionManager) throws IOException {
    return connectionManager.getInputStream();
  }

  @Provides
  OutputStream provideOutputStream(ClientConnectionManagerImpl connectionManager)
      throws IOException {
    return connectionManager.getOutputStream();
  }
}
