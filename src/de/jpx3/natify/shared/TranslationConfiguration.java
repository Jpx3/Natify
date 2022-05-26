package de.jpx3.natify.shared;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public final class TranslationConfiguration {
  public static int STRING_OBFUSCATE = 0b001;
  public static int PARALLEL_PROCESSING = 0b100;
  public static int REFERENCE_OPTIMIZATION = 0b010;

  private final List<OperatingSystem> supportedOperatingSystems;
  private final List<MethodMatcher> matchers;
  private final List<MethodMatcher> blacklistMatchers = Arrays.asList(
    new ClassAccessMethodMatcher(ACC_INTERFACE),
    new MethodAccessMethodMatcher(ACC_NATIVE | ACC_ABSTRACT),
    new NameMethodMatcher(null, "<init>", null)
  );
  private final boolean obfuscateStrings;
  private final boolean parallelProcessing;
  private final boolean referenceOptimization;

  public TranslationConfiguration(
    List<OperatingSystem> supportedOperatingSystems,
    List<MethodMatcher> matchers,
    int procedureFlags
  ) {
    this.supportedOperatingSystems = supportedOperatingSystems;
    this.matchers = matchers;

    this.obfuscateStrings = (procedureFlags & STRING_OBFUSCATE) != 0;
    this.parallelProcessing = (procedureFlags & PARALLEL_PROCESSING) != 0;
    this.referenceOptimization = (procedureFlags & REFERENCE_OPTIMIZATION) != 0;
  }

  public List<ClassNode> filterUsedClasses(List<ClassNode> classNodes) {
    return Collections.unmodifiableList(new ArrayList<>(filterActiveMethods(classNodes).keySet()));
  }

  public Map<ClassNode, List<MethodNode>> filterActiveMethods(List<ClassNode> classNodes) {
    LinkedHashMap<ClassNode, List<MethodNode>> result = classNodes.stream().collect(Collectors.toMap(classNode -> classNode, this::filterActiveMethods, (a, b) -> b, LinkedHashMap::new));
    new HashSet<>(result.keySet()).stream().filter(classNode -> result.get(classNode).isEmpty()).forEach(result::remove);
    return result;
  }

  public List<MethodNode> filterActiveMethods(ClassNode classNode) {
    List<MethodNode> methodNodes = new ArrayList<>(classNode.methods);
    matchers.stream()
      .<Predicate<? super MethodNode>>map(matcher -> methodNode -> !matcher.matches(classNode, methodNode))
      .forEach(methodNodes::removeIf);
    blacklistMatchers.stream()
      .<Predicate<? super MethodNode>>map(blacklistMatcher -> methodNode -> blacklistMatcher.matches(classNode, methodNode))
      .forEach(methodNodes::removeIf);
    return Collections.unmodifiableList(methodNodes);
  }

  public boolean shouldObfuscateStrings() {
    return obfuscateStrings;
  }

  public boolean useReferenceOptimization() {
    return referenceOptimization;
  }

  public boolean useParallelProcessing() {
    return parallelProcessing;
  }

  public List<OperatingSystem> supportedOperatingSystems() {
    return supportedOperatingSystems;
  }

  public static TranslationConfiguration defaultConfiguration() {
    //noinspection PointlessBitwiseExpression
    return new TranslationConfiguration(
      Collections.singletonList(OperatingSystem.WINDOWS),
      Collections.singletonList(PackageMethodMatcher.buildFor("*")),
      0
      | STRING_OBFUSCATE
      | PARALLEL_PROCESSING
//      | REFERENCE_OPTIMIZATION
    );
  }
}
