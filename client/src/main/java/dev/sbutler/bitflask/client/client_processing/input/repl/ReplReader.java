package dev.sbutler.bitflask.client.client_processing.input.repl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.primitives.Longs;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplDoubleQuotedString;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplInteger;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplSingleQuotedString;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplString;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Supplier;

public final class ReplReader implements AutoCloseable {

  /**
   * Various special chars used by the Repl parser for easy reuse.
   */
  private static class SpecialChars {

    static final String SINGLE_QUOTE = "'";
    static final String DOUBLE_QUOTE = "\"";
    static final String BACK_SLASH = "\\";
    static final String NEW_LINE = "\n";
  }

  private final Reader reader;

  private String peeked = "";
  private ReplToken peekedAsToken = ReplToken.START_DOCUMENT;

  public ReplReader(Reader reader) {
    this.reader = reader;
  }

  private void peek() throws IOException {
    if (isPeekedEndDocument()) {
      return;
    }
    int read = reader.read();
    peekedAsToken = mapToToken(read);
    if (isPeekedEndDocument()) {
      peeked = "";
      return;
    }
    peeked = Character.toString(read);
  }

  ReplString readReplString() throws IOException, ReplSyntaxException {
    if (isPeekedEndDocument()) {
      return null;
    }
    readAllWhiteSpace();
    return switch (peekedAsToken) {
      case CHARACTER, NUMBER -> readRawString();
      case SINGLE_QUOTE, DOUBLE_QUOTE -> readQuotedString();
      case default -> throw new ReplSyntaxException(String.format(
          "The current element cannot be read as a ReplString. First character: [%s]", peeked));
    };
  }

  private ReplString readRawString() throws IOException {
    StringBuilder builder = new StringBuilder();
    while (peekedIsNotSpace() && peekedIsNotEndLine() && peekedIsNotEndDocument()) {
      builder.append(peeked);
      peek();
    }
    return new ReplString(builder.toString());
  }

  private ReplString readQuotedString() throws IOException, ReplSyntaxException {
    String quotedString = parseQuotedString();
    return new ReplString(quotedString);
  }

  ReplInteger readReplInteger() throws IOException, ReplSyntaxException {
    // pre-parse responsibilities delegated to attemptReadingReplInteger()
    ReplElement readElement = attemptReadingReplInteger();
    if (readElement instanceof ReplString replString) {
      throw new ReplSyntaxException(
          String.format("A ReplInteger could not be read: [%s]", replString.getAsString()));
    }
    return (ReplInteger) readElement;
  }

  /**
   * Attempts to read the next element as a ReplInteger, returning a ReplString of the value read if
   * not.
   */
  @SuppressWarnings("UnstableApiUsage")
  private ReplElement attemptReadingReplInteger() throws IOException {
    if (isPeekedEndDocument()) {
      return null;
    }
    readAllWhiteSpace();
    // Try to read as number
    ReplString replString = readRawString();
    Long parsed = Longs.tryParse(replString.getAsString());
    if (parsed == null) {
      // was not a number
      return replString;
    }
    return new ReplInteger(parsed);
  }

  ReplSingleQuotedString readReplSingleQuotedString() throws IOException, ReplSyntaxException {
    if (isPeekedEndDocument()) {
      return null;
    }
    readAllWhiteSpace();
    String quotedString = parseQuotedString();
    return new ReplSingleQuotedString(quotedString);
  }

  ReplDoubleQuotedString readReplDoubleQuotedString() throws IOException, ReplSyntaxException {
    if (isPeekedEndDocument()) {
      return null;
    }
    readAllWhiteSpace();
    String quotedString = parseQuotedString();
    return new ReplDoubleQuotedString(quotedString);
  }

  ReplElement readNextElement() throws IOException, ReplSyntaxException {
    if (isPeekedEndDocument()) {
      return null;
    }
    readAllWhiteSpace();
    return switch (peekedAsToken) {
      case NUMBER -> attemptReadingReplInteger();
      case CHARACTER, SINGLE_QUOTE, DOUBLE_QUOTE -> readReplString();
      default -> throw new ReplParseException(String.format("Invalid token found: [%s]", peeked));
    };
  }

  ImmutableList<ReplElement> readToEndLine() throws IOException, ReplSyntaxException {
    if (isPeekedEndDocument()) {
      return null;
    }
    readAllWhiteSpace();
    ImmutableList.Builder<ReplElement> builder = new Builder<>();
    while (peekedIsNotEndLine() && peekedIsNotEndDocument()) {
      ReplElement readReplElement = readNextElement();
      if (readReplElement == null) {
        break;
      }
      builder.add(readReplElement);
    }
    return builder.build();
  }

  private void readAllWhiteSpace() throws IOException {
    while (isPeekedStartDocument() || isPeekedEndLine() || isPeekedWhitespace()) {
      peek();
    }
  }

  private String parseQuotedString() throws IOException, ReplSyntaxException {
    ReplToken startQuote = peekedAsToken;
    Supplier<String> escapeHandler = startQuote == ReplToken.DOUBLE_QUOTE
        ? this::doubleQuoteEscapeHandler
        : this::singleQuoteEscapeHandler;

    // consume start quote
    peek();

    StringBuilder builder = new StringBuilder();
    boolean escapeActive = false;
    for (; peekedIsNotEndLine() && peekedIsNotEndDocument(); peek()) {
      if (escapeActive) {
        String result = escapeHandler.get();
        builder.append(result);
        escapeActive = false;
      } else if (peekedAsToken == startQuote) {
        // quote complete
        break;
      } else if (isPeekedBackSlash()) {
        // start escape for next char
        escapeActive = true;
      } else {
        builder.append(peeked);
      }
    }
    if (peekedAsToken != startQuote) {
      throw new ReplSyntaxException("Quoted element was not properly terminated");
    }

    // Consume end quote
    peek();
    if (peekedIsNotEndDocument() && peekedIsNotSpace()) {
      throw new ReplSyntaxException("Quoted elements must be followed with a space");
    }
    return builder.toString();
  }

  private String doubleQuoteEscapeHandler() {
    if (isPeekedBackSlash() || isPeekedDoubleQuote()) {
      return peeked;
    }
    if (peeked.equals("n")) {
      return SpecialChars.NEW_LINE;
    }
    // Unsupported escape, include backslash
    return SpecialChars.BACK_SLASH + peeked;
  }

  private String singleQuoteEscapeHandler() {
    if (isPeekedBackSlash() || isPeekedSingleQuote()) {
      return peeked;
    }
    // Unsupported escape, include backslash
    return SpecialChars.BACK_SLASH + peeked;
  }

  private static ReplToken mapToToken(int read) {
    if (read == -1) {
      return ReplToken.END_DOCUMENT;
    }
    if (Character.isAlphabetic(read)) {
      return ReplToken.CHARACTER;
    }
    if (Character.isDigit(read)) {
      return ReplToken.NUMBER;
    }
    if (Character.isSpaceChar(read)) {
      return ReplToken.SPACE;
    }
    String asString = Character.toString(read);
    return switch (asString) {
      case SpecialChars.SINGLE_QUOTE -> ReplToken.SINGLE_QUOTE;
      case SpecialChars.DOUBLE_QUOTE -> ReplToken.DOUBLE_QUOTE;
      case SpecialChars.BACK_SLASH -> ReplToken.BACK_SLASH;
      case SpecialChars.NEW_LINE -> ReplToken.END_LINE;
      // TODO: improve handling of unexpected values
      default -> throw new ReplParseException(
          String.format("Could not map to ReplToken: int [%d], string [%s]", read, asString));
    };
  }

  private boolean isPeekedSingleQuote() {
    return peekedAsToken == ReplToken.SINGLE_QUOTE;
  }

  private boolean isPeekedDoubleQuote() {
    return peekedAsToken == ReplToken.DOUBLE_QUOTE;
  }

  private boolean isPeekedBackSlash() {
    return peekedAsToken == ReplToken.BACK_SLASH;
  }

  private boolean isPeekedSpace() {
    return peekedAsToken == ReplToken.SPACE;
  }

  private boolean peekedIsNotSpace() {
    return !isPeekedSpace();
  }

  private boolean isPeekedStartDocument() {
    return peekedAsToken == ReplToken.START_DOCUMENT;
  }

  private boolean isPeekedEndDocument() {
    return peekedAsToken == ReplToken.END_DOCUMENT;
  }

  private boolean peekedIsNotEndDocument() {
    return !isPeekedEndDocument();
  }

  private boolean isPeekedEndLine() {
    return peekedAsToken == ReplToken.END_LINE;
  }

  private boolean peekedIsNotEndLine() {
    return !isPeekedEndLine();
  }

  private boolean isPeekedWhitespace() {
    return peeked.trim().length() == 0;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
