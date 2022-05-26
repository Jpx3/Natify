package de.jpx3.natify;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class NatifyLogger {
  private final static PrintStream OUTPUT_STREAM = System.out;
  private final static String DATE_SPACING = "  ";

  public static void printFancyHeader() {
    System.out.println();
    System.out.println(" Natify - Bytecode to C translator");
    System.out.println(" Copyright Richard Strunk 2020, all rights reserved");
    System.out.println();
  }

  public static void printInfo(String info) {
    printMessage(info);
  }

  public static void printError(String error) {
    printMessage(error);
  }

  private static void printMessage(String message) {
    String timeFormatted = currentTimeFormatted();
    OUTPUT_STREAM.println(timeFormatted + DATE_SPACING + message);
  }

  private static String currentTimeFormatted() {
    return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
  }
}
