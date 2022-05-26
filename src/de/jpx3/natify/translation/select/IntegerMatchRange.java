package de.jpx3.natify.translation.select;

public final class IntegerMatchRange extends IntegerMatcher {
  private final int start, end;

  public IntegerMatchRange(int start, int end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public boolean matches(int integer) {
    return start <= integer && integer <= end;
  }

  @Override
  public String toString() {
    return "expr(" + start + " <= {value} <= " + end + ")";
  }
}
