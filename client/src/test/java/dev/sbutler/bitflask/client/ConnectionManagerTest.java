package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConnectionManagerTest {

  ConnectionManager connectionManager;
  SocketChannel socketChannel;
  Socket socket;

  @BeforeEach
  void beforeEach() {
    socketChannel = mock(SocketChannel.class);
    socket = mock(Socket.class);
    doReturn(socket).when(socketChannel).socket();
    connectionManager = new ConnectionManager(socketChannel);
  }

  @Test
  void close() throws Exception {
    // Act
    connectionManager.close();
    // Asset
    verify(socketChannel, times(1)).close();
  }

  @Test
  void getInputStream() throws Exception {
    // Arrange
    InputStream mockInputStream = mock(InputStream.class);
    doReturn(mockInputStream).when(socket).getInputStream();
    // Act
    InputStream inputStream = connectionManager.getInputStream();
    // Assert
    assertEquals(mockInputStream, inputStream);
    verify(socket, times(1)).getInputStream();
  }

  @Test
  void getOutputStream() throws Exception {
    // Arrange
    OutputStream mockOutputStream = mock(OutputStream.class);
    doReturn(mockOutputStream).when(socket).getOutputStream();
    // Act
    OutputStream outputStream = connectionManager.getOutputStream();
    // Assert
    assertEquals(mockOutputStream, outputStream);
    verify(socket, times(1)).getOutputStream();
  }
}
