package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.NatifyClassTranslator;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static org.objectweb.asm.Opcodes.*;

public final class ConditionalJumpInstructionTranslator extends InstructionTranslator<JumpInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = IntegerMatcher.mergeOr(new IntegerMatchRange(IFEQ, IF_ACMPNE), new IntegerMatchRange(IFNULL, IFNONNULL));

  @Override
  public Class<JumpInsnNode>[] targetInstructions() {
    return new Class[]{JumpInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    JumpInsnNode jumpInsnNode = (JumpInsnNode) instruction;
    int opcode = instruction.getOpcode();
    boolean valueCompare = opcode >= IF_ICMPEQ && opcode <= IF_ACMPNE;
    int baseStackPosition = executionFrame.stackSize();
    baseStackPosition -= valueCompare ? 2 : 1;
    String operand0;
    String operand1;
    ComparisonOperator operator = ComparisonOperator.suitable(opcode);
    String jumpLabel = labelExpressionFrom(jumpInsnNode.label, labelIndices);
    operand0 = variables.acquireStackVar(baseStackPosition, executionFrame);
    if(valueCompare) {
      operand1 = variables.acquireStackVar(baseStackPosition + 1, executionFrame);
    } else { // zero compare;
      operand1 = opcode >= IFNULL ? "NULL" : "0";
    }
    code.appendLine("if ("+operator.statementProcessor().apply(operand0, operand1)+") {");
    TextBlock jumpToLocalCode = TextBlock.newEmpty();
    {
      jumpToLocalCode.appendLine("goto " + jumpLabel + ";");
    }
    code.append(jumpToLocalCode, NatifyClassTranslator.SCOPE_SPACING);
    code.appendLine("}");
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }

  public enum ComparisonOperator {
    EQUAL((op0, op1) -> op0 + " == " + op1, IFEQ, IF_ICMPEQ, IF_ACMPEQ, IFNULL),
    NON_EQUAL((op0, op1) -> op0 + " != " + op1, IFNE, IF_ICMPNE, IF_ACMPNE, IFNONNULL),
    LESS_THAN( (op0, op1) -> op0 + " < " + op1, IFLT, IF_ICMPLT),
    LESS_THAN_OR_EQUAL((op0, op1) -> op0 + " <= " + op1, IFLE, IF_ICMPLE),
    GREATER_THAN((op0, op1) -> op0 + " > " + op1, IFGT, IF_ICMPGT),
    GREATER_THAN_OR_EQUAL((op0, op1) -> op0 + " >= " + op1, IFGE, IF_ICMPGE),

    ;

    final int[] opcodes;
    final BiFunction<String, String, String> statementProcessor;

    ComparisonOperator(BiFunction<String, String, String> statementProcessor, int... opcodes) {
      this.opcodes = opcodes;
      this.statementProcessor = statementProcessor;
    }

    public BiFunction<String, String, String> statementProcessor() {
      return statementProcessor;
    }

    public static ComparisonOperator suitable(int opcode) {
      return Arrays.stream(values()).filter(value -> arrayContains(value.opcodes, opcode)).findFirst().orElse(null);
    }

    private static boolean arrayContains(int[] array, int value) {
      return Arrays.stream(array).anyMatch(r -> r == value);
    }
  }
}
