package dev.sbutler.bitflask.server.configuration.logging;

import com.google.inject.MembersInjector;
import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jMembersInjector<T> implements MembersInjector<T> {

  private final Field field;
  private final Logger logger;

  Slf4jMembersInjector(Field aField) {
    field = aField;
    logger = LoggerFactory.getLogger(field.getDeclaringClass());
    field.setAccessible(true);
  }

  public void injectMembers(T anArg0) {
    try {
      field.set(anArg0, logger);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
