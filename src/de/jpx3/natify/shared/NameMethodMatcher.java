package de.jpx3.natify.shared;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class NameMethodMatcher implements MethodMatcher {
  private final String className;
  private final String methodName;
  private final String methodDesc;

  public NameMethodMatcher(String className, String methodName, String methodDesc) {
    this.className = className;
    this.methodName = methodName;
    this.methodDesc = methodDesc;
  }

  public boolean matches(ClassNode classNode, MethodNode methodNode) {
    boolean valid = true;
    if (className != null) {
      valid = classNode.name.equals(className);
    }
    if (methodName != null) {
      valid &= methodNode.name.equals(methodName);
    }
    if (methodDesc != null) {
      valid &= methodNode.desc.equals(methodDesc);
    }
    return valid;
  }
}
