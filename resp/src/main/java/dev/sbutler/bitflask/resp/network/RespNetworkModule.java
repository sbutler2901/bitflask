package dev.sbutler.bitflask.resp.network;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.reader.RespReaderImpl;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.resp.network.writer.RespWriterImpl;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RespNetworkModule extends AbstractModule {

  @Provides
  @Singleton
  RespReader provideRespReader(RespReaderImpl respReader) {
    return respReader;
  }

  @Provides
  @Singleton
  BufferedReader provideBufferedReader(InputStream inputStream) {
    return new BufferedReader(new InputStreamReader(inputStream));
  }

  @Provides
  @Singleton
  RespWriter provideRespWriter(RespWriterImpl respWriter) {
    return respWriter;
  }
}
