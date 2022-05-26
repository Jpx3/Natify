package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchValue;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static org.objectweb.asm.Opcodes.LDC;

public final class LdcInsnNodeTranslator extends InstructionTranslator<LdcInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchValue(LDC);

  @Override
  public Class<LdcInsnNode>[] targetInstructions() {
    return new Class[]{LdcInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock textBlock = TextBlock.newEmpty();
    LdcInsnNode ldcInsnNode = (LdcInsnNode) instruction;
    int insertionStackIndex = executionFrame.stackSize() + 1 /* one more than stack */ - 1 /* start index 0 translation*/;
    Object constant = ldcInsnNode.cst;
    String value;
    int typeSort;
    if (constant instanceof String) {
      String constantEntryAsString = (String) constant;
      char[] chars = constantEntryAsString.toCharArray();
      StringBuilder arrayEntryCharWrapper = new StringBuilder();
      for (int i = 0; i < chars.length; i++) {
        char aChar = chars[i];
        boolean last = i + 1 == chars.length;
        arrayEntryCharWrapper.append((int) aChar);
        if (!last) {
          arrayEntryCharWrapper.append(", ");
        }
      }
      value = "(*env)->NewString(env, (unsigned short[]) {" + arrayEntryCharWrapper + "}, " + chars.length + ")";
      constant = "string";
      typeSort = Type.OBJECT;
    } else if (constant instanceof Type) {
      // type must be reference (to either array or object)
      Type constantEntryAsType = (Type) constant;
      value = references.classAccess(constantEntryAsType.getInternalName());
      typeSort = Type.OBJECT;
    } else if (constant instanceof Float) {
      float fVal = (Float) constant;
      if (Float.isNaN(fVal)) {
        value = "0.0f / 0.0f /* NaN */";
      } else if (Float.isInfinite(fVal)) {
        boolean negativeInf = fVal == Float.NEGATIVE_INFINITY;
        value = (negativeInf ? "-" : "") + "1.0f / 0.0f /* " + (negativeInf ? "Negative " : "") + "Infinity */";
      } else {
        value = String.valueOf(constant);
        value += "f";
      }
      typeSort = Type.FLOAT;
    } else if (constant instanceof Double) {
      double dVal = (Double) constant;
      if (Double.isNaN(dVal)) {
        value = "0.0 / 0.0 /* NaN */";
      } else if (Double.isInfinite(dVal)) {
        boolean negativeInf = dVal == Float.NEGATIVE_INFINITY;
        value = (negativeInf ? "-" : "") + "1.0 / 0.0 /* " + (negativeInf ? "Negative " : "") + "Infinity */";
      } else {
        value = String.valueOf(dVal);
      }
      typeSort = Type.DOUBLE;
    } else if (constant instanceof Long) {
      value = String.valueOf(constant);
      value += "L";
      typeSort = Type.LONG;
    } else if (constant instanceof Integer) {
      value = String.valueOf(constant);
      typeSort = Type.INT;
    } else {
      value = String.valueOf(constant);
      typeSort = Type.getType(constant.getClass()).getSort();
    }
    textBlock.appendLine(variables.acquireStackVar(insertionStackIndex, typeSort) + " = " + value + "; // ldc " + constant);
    return textBlock;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return true;
  }
}
