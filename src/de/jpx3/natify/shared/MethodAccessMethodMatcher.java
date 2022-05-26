package de.jpx3.natify.shared;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class MethodAccessMethodMatcher implements MethodMatcher {
  private final int filter;

  public MethodAccessMethodMatcher(int filter) {
    this.filter = filter;
  }

  @Override
  public boolean matches(ClassNode classNode, MethodNode methodNode) {
    int access = methodNode.access;

    for (int i = 0; i < Integer.SIZE; i++) {
      int accessBit = (access >> i) & 1;
      int filterBit = (filter >> i) & 1;
      if (filterBit == 1 && accessBit == 1) {
        return true;
      }
    }
    return false;
  }
}
