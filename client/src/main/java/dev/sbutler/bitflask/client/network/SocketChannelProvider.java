package dev.sbutler.bitflask.client.network;

import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import javax.inject.Inject;
import javax.inject.Provider;

class SocketChannelProvider implements Provider<SocketChannel> {

  private final SocketAddress socketAddress;

  @Inject
  SocketChannelProvider(ClientConfigurations configurations) {
    this.socketAddress =
        new InetSocketAddress(configurations.getHost(), configurations.getPort());
  }

  @Override
  public SocketChannel get() {
    try {
      return SocketChannel.open(socketAddress);
    } catch (IOException e) {
      throw new ProvisionException("Failed to provision SocketChannel", e);
    }
  }
}
