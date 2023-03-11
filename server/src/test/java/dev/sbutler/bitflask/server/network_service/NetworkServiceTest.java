package dev.sbutler.bitflask.server.network_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.server.network_service.NetworkService.Factory;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetworkServiceTest {

  private NetworkService networkService;

  @Spy
  @SuppressWarnings("UnstableApiUsage")
  private ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Mock
  private ServerSocketChannel serverSocketChannel;
  @Mock
  private ClientHandlingService.Factory clientHandlingServiceFactory;

  @BeforeEach
  void beforeEach() {
    NetworkService.Factory factory = new Factory(executorService, clientHandlingServiceFactory);
    networkService = factory.create(serverSocketChannel);
  }

  @Test
  void run() throws Exception {
    // Arrange
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    SocketChannel socketChannel = mock(SocketChannel.class);
    when(serverSocketChannel.accept()).thenReturn(socketChannel);

    ClientHandlingService clientHandlingService = mock(ClientHandlingService.class);
    when(clientHandlingServiceFactory.create(eq(socketChannel)))
        .thenReturn(clientHandlingService);
    when(clientHandlingService.startAsync())
        .thenReturn(clientHandlingService);

    // Act
    networkService.run();
    networkService.triggerShutdown();
    // Assert
    verify(clientHandlingService, times(1)).startAsync();
    verify(clientHandlingService, times(1)).addListener(any(), any());
    verify(serverSocketChannel, atLeastOnce()).close();
    verify(clientHandlingService, atLeastOnce()).stopAsync();
  }

  @Test
  void triggerShutdown_close_throwsIOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(serverSocketChannel).close();
    // Act
    networkService.triggerShutdown();
  }
}
