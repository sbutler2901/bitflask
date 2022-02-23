package dev.sbutler.bitflask.client;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sun.jdi.InternalException;
import dev.sbutler.bitflask.client.client_processing.ClientProcessingModule;
import dev.sbutler.bitflask.client.client_processing.ClientProcessor;
import dev.sbutler.bitflask.client.command_processing.CommandProcessingModule;
import dev.sbutler.bitflask.client.connection.ConnectionManager;
import dev.sbutler.bitflask.client.connection.ConnectionModule;
import java.io.IOException;

public class Client {

  private static final String INITIALIZATION_FAILURE = "Failed to initialize the client";
  private static final String TERMINATING_CONNECTION = "Disconnecting server";
  private static final String TERMINATION_FAILURE = "Failed to close the socket";

  private final ConnectionManager connectionManager;
  private final ClientProcessor clientProcessor;

  @Inject
  public Client(ConnectionManager connectionManager, ClientProcessor clientProcessor) {
    this.connectionManager = connectionManager;
    this.clientProcessor = clientProcessor;
  }

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(
        createConnectionModule(),
        new CommandProcessingModule(),
        new ClientProcessingModule()
    );

    Client client = injector.getInstance(Client.class);
    client.start();
    client.close();
  }

  public void start() {
    clientProcessor.start();
  }

  public void close() {
    System.out.println(TERMINATING_CONNECTION);
    clientProcessor.halt();
    try {
      connectionManager.close();
    } catch (IOException e) {
      throw new InternalException(TERMINATION_FAILURE + e.getMessage());
    }
  }

  private static ConnectionModule createConnectionModule() {
    try {
      return new ConnectionModule();
    } catch (IOException e) {
      throw new InternalException(INITIALIZATION_FAILURE + e.getMessage());
    }
  }
}