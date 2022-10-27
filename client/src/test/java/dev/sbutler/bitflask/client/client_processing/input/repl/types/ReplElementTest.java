package dev.sbutler.bitflask.client.client_processing.input.repl.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ReplElementTest {

  @Test
  void replString() {
    // Arrange
    ReplElement replElement = new ReplString("value");
    // Assert
    assertThat(replElement.isReplString()).isTrue();
    assertDoesNotThrow(replElement::getAsReplString);
  }

  @Test
  void replSingleQuotedString() {
    // Arrange
    ReplElement replElement = new ReplSingleQuotedString("value");
    // Assert
    assertThat(replElement.isReplSingleQuotedString()).isTrue();
    assertDoesNotThrow(replElement::getAsReplSingleQuotedString);
  }

  @Test
  void replDoubleQuotedString() {
    // Arrange
    ReplElement replElement = new ReplDoubleQuotedString("value");
    // Assert
    assertThat(replElement.isReplDoubleQuotedString()).isTrue();
    assertDoesNotThrow(replElement::getAsReplDoubleQuotedString);
  }

  @Test
  void replInteger() {
    // Arrange
    ReplElement replElement = new ReplInteger(0);
    // Assert
    assertThat(replElement.isReplInteger()).isTrue();
    assertDoesNotThrow(replElement::getAsReplInteger);
  }

  @Test
  void replString_throwsIllegalStateException() {
    // Arrange
    ReplSingleQuotedString replSingleQuotedString = new ReplSingleQuotedString("value");
    ReplDoubleQuotedString replDoubleQuotedString = new ReplDoubleQuotedString("value");
    ReplInteger replInteger = new ReplInteger(0);
    // Assert
    assertThrows(IllegalStateException.class, replSingleQuotedString::getAsReplString);
    assertThrows(IllegalStateException.class, replDoubleQuotedString::getAsReplString);
    assertThrows(IllegalStateException.class, replInteger::getAsReplString);
  }

  @Test
  void replSingleQuotedString_throwsIllegalStateException() {
    // Arrange
    ReplString replString = new ReplString("value");
    ReplDoubleQuotedString replDoubleQuotedString = new ReplDoubleQuotedString("value");
    ReplInteger replInteger = new ReplInteger(0);
    // Assert
    assertThrows(IllegalStateException.class, replString::getAsReplSingleQuotedString);
    assertThrows(IllegalStateException.class, replDoubleQuotedString::getAsReplSingleQuotedString);
    assertThrows(IllegalStateException.class, replInteger::getAsReplSingleQuotedString);
  }

  @Test
  void replDoubleQuotedString_throwsIllegalStateException() {
    // Arrange
    ReplString replString = new ReplString("value");
    ReplSingleQuotedString replSingleQuotedString = new ReplSingleQuotedString("value");
    ReplInteger replInteger = new ReplInteger(0);
    // Assert
    assertThrows(IllegalStateException.class, replString::getAsReplDoubleQuotedString);
    assertThrows(IllegalStateException.class, replSingleQuotedString::getAsReplDoubleQuotedString);
    assertThrows(IllegalStateException.class, replInteger::getAsReplDoubleQuotedString);
  }

  @Test
  void replInteger_throwsIllegalStateException() {
    // Arrange
    ReplString replString = new ReplString("value");
    ReplSingleQuotedString replSingleQuotedString = new ReplSingleQuotedString("value");
    ReplDoubleQuotedString replDoubleQuotedString = new ReplDoubleQuotedString("value");
    // Assert
    assertThrows(IllegalStateException.class, replString::getAsReplInteger);
    assertThrows(IllegalStateException.class, replSingleQuotedString::getAsReplInteger);
    assertThrows(IllegalStateException.class, replDoubleQuotedString::getAsReplInteger);
  }
}
