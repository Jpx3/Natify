package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatchValue;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static de.jpx3.natify.translation.select.IntegerMatcher.MergeOperation.OR;
import static org.objectweb.asm.Opcodes.*;

public final class MathInstructionTranslator extends InstructionTranslator<InsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = IntegerMatcher.merge(OR, new IntegerMatchRange(IADD, IINC), new IntegerMatchRange(LCMP, DCMPG));

  @Override
  public Class<InsnNode>[] targetInstructions() {
    return new Class[]{InsnNode.class, IincInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  private final static char[] typeAccess = {'i'/*integer*/, 'j' /*long*/, 'f'/*float*/, 'd' /*double*/};

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    int stackPosition = executionFrame.stackSize();
    int opcode = instruction.getOpcode();
    if (opcode >= LCMP) { // comparison
      int baseStackPosition = stackPosition - 1 - 1;
      int value1StackPos = baseStackPosition;
      int value2StackPos = baseStackPosition + 1;
      char outputType = 'i';
      BasicValue inputType = executionFrame.getStack(value1StackPos + 1);
      String escapeValue;
      // load escape values
      switch (opcode) {
        case DCMPL:
        case FCMPL:
          escapeValue = "-1";
          break;
        case DCMPG:
        case FCMPG:
          escapeValue = "1";
          break;
        default:
          escapeValue = "";
          break;
      }
      if (escapeValue.isEmpty()) { // no escape value given, using fast method
        String tempStore = variables.acquireTempVar(0, inputType);
        String finalStore = variables.acquireStackVar(value1StackPos, outputType);
        String value1Variable = variables.acquireStackVar(value1StackPos, inputType);
        String value2Variable = variables.acquireStackVar(value2StackPos, inputType);
        code.appendLine(tempStore + " = " + value1Variable + " - " + value2Variable + ";");
        code.appendLine(finalStore + " = (" + tempStore + " > 0) - (" + tempStore + " < 0);");
      } else { // escape value given, using low method
        String finalStore = variables.acquireStackVar(value1StackPos, outputType);
        String value1Variable = variables.acquireStackVar(value1StackPos, inputType);
        String value2Variable = variables.acquireStackVar(value2StackPos, inputType);
        // x = v1 < v2 ? (-1 : v1 > v2 ? (1 : v1 == v2 ? (0 : <escape>)))
        String equalsV2OrNanHandle = value1Variable + " == " + value2Variable + " ? 0 : " + escapeValue;
        String biggerOrEqualAsV2Handle = value1Variable + " > " + value2Variable + " ? " + "1 : " + equalsV2OrNanHandle;
        String statement = finalStore + " = " + value1Variable + " < " + value2Variable + " ? -1 : " + biggerOrEqualAsV2Handle + ";";
        code.appendLine(statement);
      }
    } else if (opcode == IINC) { // direct increment
      IincInsnNode iincInsnNode = (IincInsnNode) instruction;
      String targetVariable = variables.acquireLocalVar(iincInsnNode.var, Type.INT);
      code.appendLine(targetVariable + " += " + iincInsnNode.incr + ";");
    } else { // parsed math operation
      int fold = opcode > DNEG ? 2 : 4;
      char suitableTypeAccess = typeAccess[(opcode - IADD) % fold];
      MathOperation mathOperation = MathOperation.suitableFor(opcode);
      if (mathOperation == null) {
        throw new IllegalStateException();
      }
      int stackInputLength = mathOperation.inputValues();
      int baseStackPosition = ((stackPosition + 1/*go one stack pos higher*/) - stackInputLength) - 1 /*zero translation*/;
      String op0 = variables.acquireStackVar(baseStackPosition, suitableTypeAccess);
      String op1 = stackInputLength >= 2 ? variables.acquireStackVar(baseStackPosition + 1, suitableTypeAccess) : "";
      String writtenMathOperation = mathOperation.statementProcessor().apply(op0, op1);
      String targetStackVar = variables.acquireStackVar(baseStackPosition, suitableTypeAccess);
      String finalOperation = targetStackVar + " = " + writtenMathOperation + ";";
      code.appendLine(finalOperation);
    }
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }

  private enum MathOperation {
    ADDITION(new IntegerMatchRange(IADD, DADD), (op0, op1) -> op0 + " + " + op1, 2),
    SUBTRACTION(new IntegerMatchRange(ISUB, DSUB), (op0, op1) -> op0 + " - " + op1, 2),
    MULTIPLICATION(new IntegerMatchRange(IMUL, DMUL), (op0, op1) -> op0 + " * " + op1, 2),
    DIVISION(new IntegerMatchRange(IDIV, DDIV), (op0, op1) -> op0 + " / " + op1, 2),
    REMAINDER(new IntegerMatchRange(IREM, DREM), (op0, op1) -> op0 + " % " + op1, 2),
    NEGATE(new IntegerMatchRange(INEG, DNEG), (op0, op1) -> "-" + op0, 1),
    SHIFT_LEFT(new IntegerMatchRange(ISHL, LSHL), (op0, op1) -> op0 + " << (" + op1 + " & 0x1f)", 2),
    SHIFT_RIGHT(new IntegerMatchRange(ISHR, LSHR), (op0, op1) -> op0 + " >> (" + op1 + " & 0x1f)", 2),
    LOGICAL_SHIFT_RIGHT_INT(new IntegerMatchValue(IUSHR/*, LUSHR*/), (op0, op1) -> "(jint) ((unsigned int)" + op0 + " >> (" + op1 + " & 0x1f))", 2),
    LOGICAL_SHIFT_RIGHT_LONG(new IntegerMatchValue(/*IUSHR,*/ LUSHR), (op0, op1) -> "(jlong) ((unsigned long)" + op0 + " >> (" + op1 + " & 0x1f))", 2),
    AND(new IntegerMatchRange(IAND, LAND), (op0, op1) -> op0 + " & " + op1, 2),
    OR(new IntegerMatchRange(IOR, LOR), (op0, op1) -> op0 + " | " + op1, 2),
    XOR(new IntegerMatchRange(IXOR, LXOR), (op0, op1) -> op0 + " ^ " + op1, 2),

    ;

    final IntegerMatcher opcodeMatcher;
    final BiFunction<String, String, String> statementProcessor;
    final int inputValues;

    MathOperation(IntegerMatcher opcodeMatcher, BiFunction<String, String, String> statementProcessor, int inputValues) {
      this.opcodeMatcher = opcodeMatcher;
      this.statementProcessor = statementProcessor;
      this.inputValues = inputValues;
    }

    public boolean matches(int opcode) {
      return opcodeMatcher.matches(opcode);
    }

    public BiFunction<String, String, String> statementProcessor() {
      return statementProcessor;
    }

    public int inputValues() {
      return inputValues;
    }

    public static MathOperation suitableFor(int opcode) {
      return Arrays.stream(values()).filter(operation -> operation.matches(opcode)).findFirst().orElse(null);
    }
  }
}