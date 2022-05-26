package de.jpx3.natify.shared;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;

public interface MethodMatcher {
  boolean matches(ClassNode classNode, MethodNode methodNode);

  static MethodMatcher orCombine(MethodMatcher... methodMatchers) {
    return (classNode, methodNode) ->
      Arrays.stream(methodMatchers)
        .anyMatch(methodMatcher -> methodMatcher.matches(classNode, methodNode));
  }

  static MethodMatcher andCombine(MethodMatcher... methodMatchers) {
    return (classNode, methodNode) ->
      Arrays.stream(methodMatchers)
        .allMatch(methodMatcher -> methodMatcher.matches(classNode, methodNode));
  }
}
