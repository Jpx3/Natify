package de.jpx3.natify.translation.insntrans;

public final class FailedTranslationException extends RuntimeException {

  public FailedTranslationException() {
    super();
  }

  public FailedTranslationException(String message) {
    super(message);
  }

  public FailedTranslationException(Throwable cause) {
    super(cause);
  }
}
