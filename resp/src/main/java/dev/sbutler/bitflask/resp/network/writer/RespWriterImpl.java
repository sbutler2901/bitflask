package dev.sbutler.bitflask.resp.network.writer;

import dev.sbutler.bitflask.resp.types.RespType;
import java.io.IOException;
import java.io.OutputStream;
import javax.inject.Inject;

class RespWriterImpl implements RespWriter {

  private final OutputStream outputStream;

  @Inject
  RespWriterImpl(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  /**
   * Writes the provided RespType to the underlying output-stream
   *
   * @param respType the data to be written
   * @throws IOException if a general failure occurs while reading
   */
  public void writeRespType(RespType<?> respType) throws IOException {
    outputStream.write(respType.getEncodedBytes());
    outputStream.flush();
  }

}
