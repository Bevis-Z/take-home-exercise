package com.bowen.analyzer.util;

import com.bowen.analyzer.model.DependencyGraph;
import com.bowen.analyzer.model.enums.MethodUsageType;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bowen.analyzer.util.TypeResolutionUtil.resolveAnnotationName;
import static com.bowen.analyzer.util.TypeResolutionUtil.resolveTypeName;

/**
 * Utility class for analyzing code and updating dependency graphs.
 */
public class CodeAnalysisUtil {
    
    /**
     * Processes class annotations to mark framework usage.
     */
    public static void processClassAnnotations(ClassOrInterfaceDeclaration clazz, String className, DependencyGraph graph) {
        clazz.getAnnotations().forEach(anno -> {
            String name = anno.getNameAsString();
            if (AnnotationUtil.isInjectionAndRestAnnotation(name)) {
                graph.markClassUsedByFramework(className);
            }
        });
    }

    /**
     * Processes imports to add dependencies.
     */
    public static void processImports(CompilationUnit cu, String className, DependencyGraph graph) {
        Map<String, ImportDeclaration> imports = cu.getImports().stream()
                .collect(Collectors.toMap(
                    ImportDeclaration::getNameAsString,
                    importDecl -> importDecl,
                    (a, b) -> a  // In case of duplicates, keep the first one
                ));
        
        // Get simple class names to avoid misclassified references
        Map<String, String> simpleNameToFqn = new HashMap<>();
        imports.forEach((fqn, importDecl) -> {
            String[] parts = fqn.split("\\.");
            if (parts.length > 0) {
                simpleNameToFqn.put(parts[parts.length - 1], fqn);
            }
        });
        
        imports.forEach((importName, importDecl) -> {
            boolean isStarImport = importDecl.isAsterisk();
            boolean isStaticImport = importDecl.isStatic();
            
            graph.addDependency(className, importName, isStaticImport ? "STATIC_IMPORT" : "IMPORT");
            
            if (isStarImport) {
                return;
            }
            
            if (isStaticImport) {
                graph.markImportAsUsed(className, importName);
            }
        });
    }

    /**
     * Processes type references, annotations, method signatures and fields to add dependencies.
     */
    public static void processTypeReferences(CompilationUnit cu, String packageName, String className, DependencyGraph graph) {
        Set<String> referencedTypes = new HashSet<>();
        
        // Process class/interface types in the code
        cu.findAll(ClassOrInterfaceType.class).forEach(refType -> {
            String ref;
            try {
                var resolvedType = refType.resolve();
                if (resolvedType.isReferenceType()) {
                    ref = resolvedType.asReferenceType().getQualifiedName();
                    graph.addDependency(className, ref, "REFERENCE");
                    referencedTypes.add(ref);
                    graph.markImportAsUsed(className, ref);
                } else {
                    ref = TypeResolutionUtil.fallbackName(packageName, refType.getNameAsString());
                    graph.addDependency(className, ref, "UNRESOLVED_REFERENCE");
                }
            } catch (Exception e) {
                ref = TypeResolutionUtil.fallbackName(packageName, refType.getNameAsString());
                graph.addDependency(className, ref, "UNRESOLVED_REFERENCE");
            }
        });
        
        processAnnotations(cu, className, packageName, graph, referencedTypes);
        processMethodSignatures(cu, className, packageName, graph, referencedTypes);
        processFieldTypes(cu, className, packageName, graph, referencedTypes);
    }
    
    /**
     * Processes annotations in the code.
     */
    public static void processAnnotations(CompilationUnit cu, String className, String packageName, 
                                   DependencyGraph graph, Set<String> referencedTypes) {
        cu.findAll(com.github.javaparser.ast.expr.AnnotationExpr.class).forEach(annoExpr -> {
            String annoName = annoExpr.getNameAsString();
            
            String fullyQualifiedName = resolveAnnotationName(annoName, cu);
            
            if (fullyQualifiedName != null) {
                graph.addDependency(className, fullyQualifiedName, "ANNOTATION_REFERENCE");
                referencedTypes.add(fullyQualifiedName);
                graph.markImportAsUsed(className, fullyQualifiedName);
                
                annoExpr.findAll(com.github.javaparser.ast.expr.FieldAccessExpr.class).forEach(fieldExpr -> {
                    String scope = fieldExpr.getScope().toString();
                    
                    for (ImportDeclaration importDecl : cu.getImports()) {
                        String importName = importDecl.getNameAsString();
                        if (importName.endsWith("." + scope)) {
                            referencedTypes.add(importName);
                            graph.markImportAsUsed(className, importName);
                            graph.addDependency(className, importName, "REFERENCE");
                            break;
                        }
                    }
                });
                
                if (AnnotationUtil.isInjectionAndRestAnnotation(annoName)) {
                    graph.markClassUsedByFramework(className);
                } else if (AnnotationUtil.isTestAnnotation(annoName)) {
                    graph.markClassUsedByTest(className);
                }
            }
        });
    }
    
    /**
     * Processes method signatures including return types and parameter types.
     */
    public static void processMethodSignatures(CompilationUnit cu, String className, String packageName, 
                                       DependencyGraph graph, Set<String> referencedTypes) {
        cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).forEach(method -> {
            // Process return type
            method.getType().ifClassOrInterfaceType(returnType -> {
                try {
                    var resolvedType = returnType.resolve();
                    if (resolvedType.isReferenceType()) {
                        String typeName = resolvedType.asReferenceType().getQualifiedName();
                        referencedTypes.add(typeName);
                        graph.markImportAsUsed(className, typeName);
                        graph.addDependency(className, typeName, "REFERENCE");
                    }
                } catch (Exception e) {
                    String typeName = returnType.getNameAsString();
                    String fullyQualifiedName = resolveTypeName(typeName, cu, packageName);
                    if (fullyQualifiedName != null) {
                        referencedTypes.add(fullyQualifiedName);
                        graph.markImportAsUsed(className, fullyQualifiedName);
                    }
                }
            });
            
            // Process generic type arguments in return type
            method.getType().ifClassOrInterfaceType(returnType -> {
                returnType.getTypeArguments().ifPresent(typeArgs -> {
                    for (com.github.javaparser.ast.type.Type typeArg : typeArgs) {
                        if (typeArg.isClassOrInterfaceType()) {
                            processTypeArgument(typeArg.asClassOrInterfaceType(), cu, className, packageName, graph, referencedTypes);
                        }
                    }
                });
            });
            
            // Process method parameters
            method.getParameters().forEach(param -> {
                param.getType().ifClassOrInterfaceType(paramType -> {
                    try {
                        var resolvedType = paramType.resolve();
                        if (resolvedType.isReferenceType()) {
                            String typeName = resolvedType.asReferenceType().getQualifiedName();
                            referencedTypes.add(typeName);
                            graph.markImportAsUsed(className, typeName);
                            graph.addDependency(className, typeName, "REFERENCE");
                        }
                    } catch (Exception e) {
                        String typeName = paramType.getNameAsString();
                        String fullyQualifiedName = resolveTypeName(typeName, cu, packageName);
                        if (fullyQualifiedName != null) {
                            referencedTypes.add(fullyQualifiedName);
                            graph.markImportAsUsed(className, fullyQualifiedName);
                        }
                    }
                    
                    // Process generic type arguments in parameter type
                    paramType.getTypeArguments().ifPresent(typeArgs -> {
                        for (com.github.javaparser.ast.type.Type typeArg : typeArgs) {
                            if (typeArg.isClassOrInterfaceType()) {
                                processTypeArgument(typeArg.asClassOrInterfaceType(), cu, className, packageName, graph, referencedTypes);
                            }
                        }
                    });
                });
                
                // Process parameter annotations for qualifiers
                param.getAnnotations().forEach(anno -> {
                    String annoName = anno.getNameAsString();
                    String fullyQualifiedName = resolveAnnotationName(annoName, cu);
                    
                    if (fullyQualifiedName != null) {
                        referencedTypes.add(fullyQualifiedName);
                        graph.markImportAsUsed(className, fullyQualifiedName);
                    }
                });
            });
            
            // Process exceptions thrown by the method
            method.getThrownExceptions().forEach(excType -> {
                String excName = excType.asString();
                String fullyQualifiedName = resolveTypeName(excName, cu, packageName);
                if (fullyQualifiedName != null) {
                    referencedTypes.add(fullyQualifiedName);
                    graph.markImportAsUsed(className, fullyQualifiedName);
                    graph.addDependency(className, fullyQualifiedName, "REFERENCE");
                }
            });
        });
    }
    
    /**
     * Processes field types and generic type parameters.
     */
    public static void processFieldTypes(CompilationUnit cu, String className, String packageName, 
                                 DependencyGraph graph, Set<String> referencedTypes) {
        cu.findAll(com.github.javaparser.ast.body.FieldDeclaration.class).forEach(field -> {
            field.getVariables().forEach(var -> {
                var.getType().ifClassOrInterfaceType(fieldType -> {
                    try {
                        var resolvedType = fieldType.resolve();
                        if (resolvedType.isReferenceType()) {
                            String typeName = resolvedType.asReferenceType().getQualifiedName();
                            referencedTypes.add(typeName);
                            graph.markImportAsUsed(className, typeName);
                            graph.addDependency(className, typeName, "REFERENCE");
                        }
                    } catch (Exception e) {
                        String typeName = fieldType.getNameAsString();
                        String fullyQualifiedName = resolveTypeName(typeName, cu, packageName);
                        if (fullyQualifiedName != null) {
                            referencedTypes.add(fullyQualifiedName);
                            graph.markImportAsUsed(className, fullyQualifiedName);
                        }
                    }
                    
                    // Process generic type arguments
                    fieldType.getTypeArguments().ifPresent(typeArgs -> {
                        for (com.github.javaparser.ast.type.Type typeArg : typeArgs) {
                            if (typeArg.isClassOrInterfaceType()) {
                                processTypeArgument(typeArg.asClassOrInterfaceType(), cu, className, packageName, graph, referencedTypes);
                            }
                        }
                    });
                });
            });
        });
    }
    
    /**
     * Processes a generic type argument.
     */
    public static void processTypeArgument(com.github.javaparser.ast.type.ClassOrInterfaceType typeArg, 
                                   CompilationUnit cu, String className, String packageName, 
                                   DependencyGraph graph, Set<String> referencedTypes) {
        try {
            var resolvedType = typeArg.resolve();
            if (resolvedType.isReferenceType()) {
                String typeName = resolvedType.asReferenceType().getQualifiedName();
                referencedTypes.add(typeName);
                graph.markImportAsUsed(className, typeName);
                graph.addDependency(className, typeName, "REFERENCE");
            }
        } catch (Exception e) {
            String typeName = typeArg.getNameAsString();
            String fullyQualifiedName = resolveTypeName(typeName, cu, packageName);
            if (fullyQualifiedName != null) {
                referencedTypes.add(fullyQualifiedName);
                graph.markImportAsUsed(className, fullyQualifiedName);
            }
        }
        
        // Process nested generic type arguments
        typeArg.getTypeArguments().ifPresent(nestedTypeArgs -> {
            for (com.github.javaparser.ast.type.Type nestedTypeArg : nestedTypeArgs) {
                if (nestedTypeArg.isClassOrInterfaceType()) {
                    processTypeArgument(nestedTypeArg.asClassOrInterfaceType(), cu, className, packageName, graph, referencedTypes);
                }
            }
        });
    }

    /**
     * Processes method calls to track caller-callee relationships.
     */
    public static void processMethodCalls(CompilationUnit cu, String className, DependencyGraph graph, boolean isTestCode) {
        Set<String> referencedTypes = new HashSet<>();
        
        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String calleeMethod = methodCall.getNameAsString();
            String calleeClass;
            String callerMethod = TypeResolutionUtil.findEnclosingMethodName(methodCall);
            boolean resolved = false;

            try {
                var resolvedMethod = methodCall.resolve();
                calleeClass = resolvedMethod.declaringType().getQualifiedName();
                calleeMethod = resolvedMethod.getName();
                resolved = true;
                
                referencedTypes.add(calleeClass);
                graph.markImportAsUsed(className, calleeClass);
                graph.addDependency(className, calleeClass, "REFERENCE");
            } catch (Exception e) {
                calleeClass = "(unresolved)";
                
                // Try to resolve possible static imports
                if (methodCall.getScope().isEmpty()) {
                    for (ImportDeclaration importDecl : cu.getImports()) {
                        if (importDecl.isStatic()) {
                            String importName = importDecl.getNameAsString();
                            if (importName.endsWith("." + calleeMethod)) {
                                String staticImportClass = importName.substring(0, importName.lastIndexOf("."));
                                referencedTypes.add(staticImportClass);
                                graph.markImportAsUsed(className, staticImportClass);
                                graph.addDependency(className, staticImportClass, "REFERENCE");
                                break;
                            }
                        }
                    }
                } else {
                    String scope = methodCall.getScope().get().toString();
                    for (ImportDeclaration importDecl : cu.getImports()) {
                        String importName = importDecl.getNameAsString();
                        if (importName.endsWith("." + scope)) {
                            referencedTypes.add(importName);
                            graph.markImportAsUsed(className, importName);
                            graph.addDependency(className, importName, "REFERENCE");
                            break;
                        }
                    }
                }
            }

            graph.addMethodCall(className, callerMethod, calleeClass, calleeMethod);

            if (resolved) {
                graph.markMethodUsage(calleeClass, calleeMethod, MethodUsageType.CALLED);
                
                if (isTestCode) {
                    graph.markClassUsedByTest(calleeClass);
                    graph.markMethodUsedByTest(calleeClass, calleeMethod);
                    graph.markMethodUsage(calleeClass, calleeMethod, MethodUsageType.TEST);
                }
            } else if (isTestCode) {
                for (String potentialClass : graph.getAllClasses()) {
                    if (graph.getMethodCalls(potentialClass).contains(calleeMethod)) {
                        graph.markMethodUsage(potentialClass, calleeMethod, MethodUsageType.TEST);
                    }
                }
            }
            
            // Check arguments to method calls for potential references
            processMethodCallArguments(methodCall, cu, className, graph, referencedTypes);
        });
        
        // Add any referenced types to the dependency graph
        for (String type : referencedTypes) {
            graph.markImportAsUsed(className, type);
        }
    }
    
    /**
     * Processes arguments of method calls for potential references.
     */
    private static void processMethodCallArguments(MethodCallExpr methodCall, CompilationUnit cu, 
                                        String className, DependencyGraph graph, Set<String> referencedTypes) {
        methodCall.getArguments().forEach(arg -> {
            // Check for class reference arguments
            arg.findAll(com.github.javaparser.ast.expr.ClassExpr.class).forEach(classExpr -> {
                String typeName = classExpr.getType().asString();
                String fullyQualifiedName = resolveTypeName(typeName, cu, 
                        cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse(""));
                if (fullyQualifiedName != null) {
                    referencedTypes.add(fullyQualifiedName);
                    graph.markImportAsUsed(className, fullyQualifiedName);
                    graph.addDependency(className, fullyQualifiedName, "REFERENCE");
                }
            });
            
            // Check for static field references in arguments
            arg.findAll(com.github.javaparser.ast.expr.FieldAccessExpr.class).forEach(fieldExpr -> {
                String scope = fieldExpr.getScope().toString();
                
                for (ImportDeclaration importDecl : cu.getImports()) {
                    String importName = importDecl.getNameAsString();
                    if (importName.endsWith("." + scope)) {
                        referencedTypes.add(importName);
                        graph.markImportAsUsed(className, importName);
                        graph.addDependency(className, importName, "REFERENCE");
                        break;
                    }
                }
            });
            
            // Check for methods called in arguments
            arg.findAll(MethodCallExpr.class).forEach(nestedCall -> {
                if (nestedCall.getScope().isPresent()) {
                    String scope = nestedCall.getScope().get().toString();
                    for (ImportDeclaration importDecl : cu.getImports()) {
                        String importName = importDecl.getNameAsString();
                        if (importName.endsWith("." + scope)) {
                            referencedTypes.add(importName);
                            graph.markImportAsUsed(className, importName);
                            break;
                        }
                    }
                }
            });
        });
    }

    /**
     * Processes method declarations to track methods and their annotations.
     */
    public static void processMethodDeclarations(ClassOrInterfaceDeclaration clazz, String className, DependencyGraph graph) {
        clazz.findAll(MethodDeclaration.class).forEach(method -> {
            String methodName = method.getNameAsString();
            graph.registerMethod(className, methodName);

            // Check method annotations
            method.getAnnotations().forEach(anno -> {
                String annoName = anno.getNameAsString();
                if (AnnotationUtil.isInjectionAndRestAnnotation(annoName)) {
                    graph.markMethodUsedByFramework(className, methodName);
                    graph.markMethodUsage(className, methodName, MethodUsageType.FRAMEWORK);
                }
                if (AnnotationUtil.isTestAnnotation(annoName)) {
                    graph.markMethodUsedByFramework(className, methodName);
                    graph.markMethodUsage(className, methodName, MethodUsageType.TEST);
                }
            });

            // Check parameter annotations
            method.getParameters().forEach(param -> {
                param.getAnnotations().forEach(paramAnno -> {
                    String paramAnnoName = paramAnno.getNameAsString();
                    if (AnnotationUtil.isInjectionAndRestAnnotation(paramAnnoName)) {
                        graph.markMethodUsedByFramework(className, methodName);
                        graph.markMethodUsage(className, methodName, MethodUsageType.FRAMEWORK);
                    }
                });
            });
        });
    }
} 