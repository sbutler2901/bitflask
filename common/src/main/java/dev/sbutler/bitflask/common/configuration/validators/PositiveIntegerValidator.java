package dev.sbutler.bitflask.common.configuration.validators;

import com.beust.jcommander.IParameterValidator;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;

public class PositiveIntegerValidator implements IParameterValidator {

  public void validate(String name, String value) throws IllegalConfigurationException {
    int n = Integer.parseInt(value);
    if (Integer.signum(n) < 1) {
      throw new IllegalConfigurationException("Parameter " + name
          + " should be positive (found " + value + ")");
    }
  }
}
