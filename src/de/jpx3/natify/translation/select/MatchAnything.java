package de.jpx3.natify.translation.select;

public final class MatchAnything extends IntegerMatcher {
  @Override
  public boolean matches(int integer) {
    return true;
  }

  @Override
  public String toString() {
    return "expr(*)";
  }
}
