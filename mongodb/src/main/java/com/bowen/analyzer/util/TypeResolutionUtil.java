package com.bowen.analyzer.util;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.*;

/**
 * Utility class for resolving type and annotation names.
 */
public class TypeResolutionUtil {

    /**
     * Resolves a simple type name to its fully qualified name.
     */
    public static String resolveTypeName(String typeName, CompilationUnit cu, String packageName) {
        if (typeName.contains(".")) {
            return typeName;
        }

        for (ImportDeclaration importDecl : cu.getImports()) {
            String importName = importDecl.getNameAsString();
            if (importName.endsWith("." + typeName)) {
                return importName;
            }

            if (importDecl.isAsterisk()) {
                String basePackage = importName.substring(0, importName.lastIndexOf("."));
                return basePackage + "." + typeName;
            }
        }

        if (isJavaLangType(typeName)) {
            return "java.lang." + typeName;
        }

        return packageName.isEmpty() ? typeName : packageName + "." + typeName;
    }

    /**
     * Resolves an annotation name to its fully qualified name.
     */
    public static String resolveAnnotationName(String annotationName, CompilationUnit cu) {
        if (annotationName.contains(".")) {
            return annotationName;
        }

        for (ImportDeclaration importDecl : cu.getImports()) {
            String importName = importDecl.getNameAsString();
            if (importName.endsWith("." + annotationName)) {
                return importName;
            }

            if (importDecl.isAsterisk()) {
                String basePackage = importName.substring(0, importName.lastIndexOf("."));
                return basePackage + "." + annotationName;
            }
        }

        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        return packageName.isEmpty() ? annotationName : packageName + "." + annotationName;
    }

    /**
     * Fallback fully qualified name construction.
     */
    public static String fallbackName(String packageName, String simpleName) {
        if (simpleName.contains(".")) {
            return simpleName;
        }
        return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
    }

    /**
     * Determines if the type is a standard java.lang type.
     */
    public static boolean isJavaLangType(String typeName) {
        Set<String> javaLangTypes = Set.of(
                "String", "Object", "Exception", "RuntimeException", "Integer",
                "Boolean", "Long", "Double", "Float", "Byte", "Character",
                "Short", "Void", "Class"
        );
        return javaLangTypes.contains(typeName);
    }

    /**
     * Finds the name of the method that contains a method call.
     */
    public static String findEnclosingMethodName(MethodCallExpr call) {
        return call.findAncestor(MethodDeclaration.class)
                .map(m -> m.getNameAsString())
                .orElse("(field initializer or unknown method)");
    }
}