package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.translation.select.IntegerMatchValue;
import org.objectweb.asm.tree.InsnNode;

public final class NopInsnNodeTranslator extends DummyTranslator<InsnNode> {
  private final static IntegerMatchValue OPCODE_RANGE = new IntegerMatchValue(0);

  @Override
  public Class<InsnNode>[] targetInstructions() {
    return new Class[]{InsnNode.class};
  }

  @Override
  public IntegerMatchValue opcodeRange() {
    return OPCODE_RANGE;
  }
}
