package de.jpx3.natify.shared;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.regex.Pattern;

public final class PackageMethodMatcher implements MethodMatcher {
  private final Pattern packagePattern;

  public PackageMethodMatcher(Pattern packagePattern) {
    this.packagePattern = packagePattern;
  }

  @Override
  public boolean matches(ClassNode classNode, MethodNode methodNode) {
    String className = classNode.name;
    String packageName = "";
    if(className.contains("/")) {
      packageName = className.substring(0, className.lastIndexOf("/"));
    }
    //    System.out.println(packageName + " "  + packagePattern.pattern() + " " + matches);
    return packagePattern.matcher(packageName).matches();
  }

  public static PackageMethodMatcher buildFor(String packageQualifier) {
    packageQualifier = packageQualifier.replace(".", "/");
//    if(!packageQualifier.endsWith("/")) {
//      packageQualifier += "/";
//    }
//    packageQualifier = packageQualifier.replace("/", "\\/");
    packageQualifier = packageQualifier.replace("*", "[a-zA-Z0-9/]*");
    return new PackageMethodMatcher(Pattern.compile(packageQualifier));
  }
}
