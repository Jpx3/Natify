package de.jpx3.natify.translation.controller;

import de.jpx3.natify.NatifyControl;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.objectweb.asm.Type.*;

public final class Variables {

  public static final String DEFAULT_LOCAL_VAR_PREFIX = "local";
  public static final String DEFAULT_STACK_VAR_PREFIX = "stack";
  public static final String DEFAULT_TEMP_VAR_PREFIX = "temp";
  private final MethodNode targetMethodNode;

  private final List<String> locals = new ArrayList<>();
  private final List<String> stack = new ArrayList<>();
  private final List<String> temp = new ArrayList<>();
  private final List<String> allNames = new ArrayList<>();

  public Variables(MethodNode targetMethodNode) {
    this.targetMethodNode = targetMethodNode;
  }

  public List<String> locals() {
    return locals;
  }

  public List<String> stack() {
    return stack;
  }

  public List<String> temp() {
    return temp;
  }

  public String acquireLocalVar(int index, BasicValue value) {
    return acquireLocalVar(index, value.getType());
  }

  public String acquireLocalVar(int index, Type type) {
    return acquireLocalVar(index, type.getSort());
  }

  public String acquireLocalVar(int index, char typeSort) {
    return acquireLocalVar(index) + "." + typeSort;
  }

  public String acquireLocalVar(int index, int typeSort) {
    return acquireLocalVar(index) + "." + typeAccessFor(typeSort);
  }

  public String acquireLocalVar(int index) {
    if (NatifyControl.OBFUSCATE_VARIABLE_NAMES) {
      return acquireVar(locals, index);
    } else {
      String variableIdentifier = DEFAULT_LOCAL_VAR_PREFIX + index;
      locals.add(variableIdentifier);
      return variableIdentifier;
    }
  }

  public String acquireStackVar(int index, Frame<BasicValue> frame) {
    return acquireStackVar(index, frame.getStack(index));
  }

  public String acquireStackVar(int index, BasicValue value) {
    return acquireStackVar(index, value.getType());
  }

  public String acquireStackVar(int index, Type type) {
    return acquireStackVar(index, type.getSort());
  }

  public String acquireStackVar(int index, char typeSort) {
    return acquireStackVar(index) + "." + typeSort;
  }

  public String acquireStackVar(int index, int typeSort) {
    return acquireStackVar(index) + "." + typeAccessFor(typeSort);
  }

  public String acquireStackVar(int index) {
    if (NatifyControl.OBFUSCATE_VARIABLE_NAMES) {
      return acquireVar(stack, index);
    } else {
      String variableIdentifier = DEFAULT_STACK_VAR_PREFIX + index;
      stack.add(variableIdentifier);
      return variableIdentifier;
    }
  }

  public String acquireTempVar(int index, BasicValue value) {
    return acquireTempVar(index, value.getType());
  }

  public String acquireTempVar(int index, Type type) {
    return acquireTempVar(index, type.getSort());
  }

  public String acquireTempVar(int index, char typeSort) {
    return acquireTempVar(index) + "." + typeSort;
  }

  public String acquireTempVar(int index, int typeSort) {
    return acquireTempVar(index) + "." + typeAccessFor(typeSort);
  }

  public String acquireTempVar(int index) {
    if (NatifyControl.OBFUSCATE_VARIABLE_NAMES) {
      return acquireVar(temp, index);
    } else {
      String variableIdentifier = DEFAULT_TEMP_VAR_PREFIX + index;
      temp.add(variableIdentifier);
      return variableIdentifier;
    }
  }

  private String typeAccessFor(int typeSort) {
    switch (typeSort) {
      case BOOLEAN:
//        return "z";
      case BYTE:
//        return "b";
      case CHAR:
//        return "c";
      case SHORT:
//        return "s";
      case INT:
        return "i";
      case LONG:
        return "j";
      case FLOAT:
        return "f";
      case DOUBLE:
        return "d";
      default:
        return "l";
    }
  }

  private String acquireVar(List<String> register, int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("Requested register var index is negative");
    }
    for (int i = register.size(); i <= index; i++) {
      register.add(generateNewName());
    }
    return register.get(index);
  }

  private String generateNewName() {
    String name;
    int index = 0;
    do {
      name = newLowercaseNameByIndex(index + ThreadLocalRandom.current().nextInt(0, 25));
      index++;
    } while (nameExists(name));
    allNames.add(name);
    return name;
  }

  private boolean nameExists(String name) {
    return allNames.contains(name);
  }

  private final static String AVAILABLE_DIGITS_LOWERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toLowerCase();

  public String newLowercaseNameByIndex(int index) {
    if (index <= 0) {
      return String.valueOf(AVAILABLE_DIGITS_LOWERCASE.charAt(0));
    }
    int base = AVAILABLE_DIGITS_LOWERCASE.length();
    StringBuilder output = new StringBuilder();
    while (index > 0) {
      int digit = index % base;
      output.insert(0, AVAILABLE_DIGITS_LOWERCASE.charAt(digit));
      index = index / base;
    }
    return output.toString();
  }
}
