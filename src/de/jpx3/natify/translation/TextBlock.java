package de.jpx3.natify.translation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.jpx3.natify.NatifyControl.ERASE_FILE_DEBUGS;

public final class TextBlock {
  private final List<String> lines = new ArrayList<>();

  private TextBlock() {
  }

  public void append(TextBlock block) {
    append(block, 0);
  }

  public void append(TextBlock block, int spacing) {
    String spaceAsString = buildSpacing(spacing);
    for (String line : block.lines) {
      lines.add(spaceAsString + line);
    }
  }

  public void appendLine(String line) {
    if (ERASE_FILE_DEBUGS) {
      if (line.startsWith("//")) {
        return;
      }
      if (line.contains("//")) {
        line = line.substring(0, line.indexOf("//"));
      }
      while (line.contains("/*") && line.contains("*/")) {
        int startIndex = line.indexOf("/*");
        int endIndex = line.indexOf("*/") + 2;
        line = line.substring(0, startIndex) + line.substring(endIndex);
      }
    }
    this.lines.add(line);
  }

  public void appendLines(String... lines) {
    this.lines.addAll(Arrays.asList(lines));
  }

  public void appendToLastLine(String appendix) {
    int index = lines.size() - 1;
    String line = lines.get(index);
    if (line == null) {
      throw new IllegalStateException();
    }
    setLineAt(index, line + appendix);
  }

  public void emptyLine() {
    appendLine("");
  }

  public boolean isEmpty() {
    return lines.isEmpty();
  }

  public int size() {
    return lines.size();
  }

  public String lineAt(int index) {
    return lines.get(index);
  }

  public void setLineAt(int index, String string) {
    lines.set(index, string);
  }

  public String deleteLine(int index) {
    return lines.remove(index);
  }

  public String inlineStringify() {
    return lines.stream().map(String::trim).collect(Collectors.joining());
  }

  public String stringifyWith(int spacing) {
    String spaceAsString = buildSpacing(spacing);
    return lines.stream().map(line -> spaceAsString + line + "\n").collect(Collectors.joining());
  }

  private String buildSpacing(int size) {
    StringBuilder space = new StringBuilder();
    IntStream.range(0, size).mapToObj(i -> " ").forEach(space::append);
    return space.toString();
  }

  public boolean contains(CharSequence charSequence) {
    return lines.stream().anyMatch(line -> line.contains(charSequence));
  }

  public static TextBlock ofLines(String... lines) {
    TextBlock textBlock = newEmpty();
    textBlock.appendLines(lines);
    return textBlock;
  }

  public static TextBlock newEmpty() {
    return new TextBlock();
  }
}
