package dev.sbutler.bitflask.client;

import com.beust.jcommander.JCommander;
import com.google.common.base.Joiner;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.client.client_processing.repl.ReplReader;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.client.configuration.ClientConfigurationConstants;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ResourceBundle;

public class Client implements Runnable {

  private final ClientConfiguration configuration;
  private final ConnectionManager connectionManager;

  Client(ClientConfiguration configuration, ConnectionManager connectionManager) {
    this.configuration = configuration;
    this.connectionManager = connectionManager;
  }

  public static void main(String[] args) {
    ClientConfiguration configuration = initializeConfiguration(args);
    try {
      ConnectionManager connectionManager = createConnectionManager(configuration);
      Client client = new Client(configuration, connectionManager);
      client.run();
    } catch (IOException e) {
      System.err.println("Failed to initialize connection to the server" + e);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static ClientConfiguration initializeConfiguration(String[] args) {
    ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    ClientConfiguration configuration = new ClientConfiguration();
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(
            ClientConfigurationConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP,
            resourceBundle);
    JCommander.newBuilder()
        .addObject(configuration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(args);
    return configuration;
  }

  private static ConnectionManager createConnectionManager(ClientConfiguration configuration)
      throws IOException {
    SocketAddress socketAddress = new InetSocketAddress(configuration.getHost(),
        configuration.getPort());
    SocketChannel socketChannel = SocketChannel.open(socketAddress);
    return new ConnectionManager(socketChannel);
  }

  @Override
  public void run() {
    Injector injector = Guice.createInjector(ClientModule.create(configuration, connectionManager));

    Reader userInputReader = createUserInputReader();
    ReplClientProcessorService replClientProcessorService =
        createReplClientProcessorService(injector, userInputReader);

    registerShutdownHook(replClientProcessorService);
    replClientProcessorService.run();
  }

  private ReplClientProcessorService createReplClientProcessorService(Injector injector,
      Reader userInputReader) {
    ReplReader replReader = new ReplReader(userInputReader);
    ReplClientProcessorService.Factory replFactory =
        injector.getInstance(ReplClientProcessorService.Factory.class);
    return replFactory.create(replReader, shouldExecuteWithRepl());
  }

  private Reader createUserInputReader() {
    if (shouldExecuteWithRepl()) {
      return new InputStreamReader(System.in);
    }

    String inlineCmd = Joiner.on(' ').join(configuration.getInlineCmd());
    return new StringReader(inlineCmd);
  }

  private boolean shouldExecuteWithRepl() {
    return configuration.getInlineCmd().size() == 0;
  }

  private void registerShutdownHook(ReplClientProcessorService replClientProcessorService) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Exiting...");
      replClientProcessorService.triggerShutdown();
      closeConnectionManager();
    }));
  }

  private void closeConnectionManager() {
    try {
      connectionManager.close();
    } catch (IOException e) {
      System.err.println("Issues closing connection " + e);
    }
  }
}
