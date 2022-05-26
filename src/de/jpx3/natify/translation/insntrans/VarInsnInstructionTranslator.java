package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static de.jpx3.natify.translation.insntrans.TypeHelper.CONST_INT_LOAD_ACCESS_TYPES;
import static de.jpx3.natify.translation.select.IntegerMatcher.MergeOperation.OR;
import static org.objectweb.asm.Opcodes.*;

// load/save

// TODO: 09/08/20 see RET statement
public final class VarInsnInstructionTranslator extends InstructionTranslator<AbstractInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = IntegerMatcher.merge(OR, new IntegerMatchRange(ILOAD, ALOAD), new IntegerMatchRange(ISTORE, ASTORE));

  @Override
  public Class<AbstractInsnNode>[] targetInstructions() {
    return new Class[]{VarInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    int currentStackSize = executionFrame.stackSize();
    String to, from, comment;
    int opcode = instruction.getOpcode();
    int operand = ((VarInsnNode) instruction).var;
    if (opcode >= ILOAD && opcode <= ALOAD) {
      char jValueAccess = CONST_INT_LOAD_ACCESS_TYPES[opcode - ILOAD];
      // load local variable from {slot} to top of stack
      from = variables.acquireLocalVar(operand, jValueAccess);
      to = variables.acquireStackVar(currentStackSize + 1 - 1, jValueAccess);
      comment = "Load local variable from slot " + operand + " to top of stack";
    } else {
      char jValueAccess = CONST_INT_LOAD_ACCESS_TYPES[opcode - ISTORE];
      // store top of stack to local variables {slot}
      from = variables.acquireStackVar(currentStackSize - 1, jValueAccess);
      to = variables.acquireLocalVar(operand, jValueAccess);
      comment = "Store top of stack to local variables slot " + operand;
    }
    code.appendLine(to + " = " + from + "; // " + comment);
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    // TODO: 09/08/20 figure this out pls
    return false;
  }
}
