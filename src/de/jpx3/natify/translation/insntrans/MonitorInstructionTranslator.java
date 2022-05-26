package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static org.objectweb.asm.Opcodes.MONITORENTER;
import static org.objectweb.asm.Opcodes.MONITOREXIT;

public final class MonitorInstructionTranslator extends InstructionTranslator<InsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchRange(MONITORENTER, MONITOREXIT);

  @Override
  public Class<InsnNode>[] targetInstructions() {
    return new Class[]{InsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    int opcode = instruction.getOpcode();
    String methodName;
    if (opcode == MONITORENTER) {
      methodName = "MonitorEnter";
    } else {
      methodName = "MonitorExit";
    }
    String lockStackAccess = variables.acquireStackVar(executionFrame.stackSize() - 1, executionFrame);
    code.appendLine("(*env)->" + methodName + "(env, " + lockStackAccess + ");");
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return true;
  }
}
