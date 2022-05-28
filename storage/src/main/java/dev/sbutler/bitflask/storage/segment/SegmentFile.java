package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class SegmentFile {

  // todo: convert to synchronous FileChannel?
  private final AsynchronousFileChannel segmentFileChannel;
  private final Path segmentFilePath;
  private final String segmentFileKey;

  public SegmentFile(AsynchronousFileChannel segmentFileChannel, Path segmentFilePath,
      String segmentFileKey) {
    this.segmentFileChannel = segmentFileChannel;
    this.segmentFilePath = segmentFilePath;
    this.segmentFileKey = segmentFileKey;
  }

  void write(byte[] data, long fileOffset) throws IOException {
    Future<Integer> writeFuture = segmentFileChannel.write(ByteBuffer.wrap(data), fileOffset);
    try {
      writeFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      String message = String.format("Failed to write the provided date at fileOffset [%d]",
          fileOffset);
      throw new IOException(message, e);
    }
  }

  byte[] read(int readLength, long fileOffset) throws IOException {
    ByteBuffer readBytesBuffer = ByteBuffer.allocate(readLength);
    Future<Integer> readFuture = segmentFileChannel.read(readBytesBuffer, fileOffset);

    try {
      readFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      String message = String.format("Failed to read the date at fileOffset [%d]", fileOffset);
      throw new IOException(message, e);
    }

    return readBytesBuffer.array();
  }

  String readAsString(int readLength, long fileOffset) throws IOException {
    return new String(read(readLength, fileOffset));
  }

  byte readByte(long fileOffset) throws IOException {
    byte[] readBytes = read(1, fileOffset);
    return readBytes[0];
  }

  long size() throws IOException {
    return segmentFileChannel.size();
  }

  Path getSegmentFilePath() {
    return segmentFilePath;
  }

  String getSegmentFileKey() {
    return segmentFileKey;
  }

  void close() throws IOException {
    segmentFileChannel.close();
  }

  boolean isOpen() {
    return segmentFileChannel.isOpen();
  }

}
