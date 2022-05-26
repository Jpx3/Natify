package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchValue;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static org.objectweb.asm.Opcodes.INSTANCEOF;

public final class InstanceOfInstructionTranslator extends InstructionTranslator<TypeInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchValue(INSTANCEOF);

  @Override
  public Class<TypeInsnNode>[] targetInstructions() {
    return new Class[]{TypeInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    TypeInsnNode typeInsnNode = (TypeInsnNode) instruction;
    int baseStackPos = executionFrame.stackSize() - 1;
    String targetStackVar = variables.acquireStackVar(baseStackPos, 'i');
    String sourceStackVar = variables.acquireStackVar(baseStackPos, 'l');
    String classAccess = references.classAccess(typeInsnNode.desc);

    // JNI IsInstanceOf does return **true** if input var is NULL
    // instanceof does return **false** if input var is NULL

    code.appendLine(targetStackVar + " = " + sourceStackVar + " != NULL && (*env)->IsInstanceOf(env, " + sourceStackVar + ", " + classAccess + ");");
    // stack0.i = (*env)->IsInstanceOf(env, stack0.l, (*env)->FindClass(env, "java/lang/String"));
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }
}
