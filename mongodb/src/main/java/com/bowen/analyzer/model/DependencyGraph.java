package com.bowen.analyzer.model;

import com.bowen.analyzer.model.enums.MethodUsageType;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The core model that holds all dependency information from the code analysis.
 */
public class DependencyGraph {

    private DefaultDirectedGraph<String, LabeledEdge> graph = new DefaultDirectedGraph<>(LabeledEdge.class);
    private final Map<String, Set<String>> methodCallsByClass = new HashMap<>();
    private final List<MethodCall> methodCalls = new ArrayList<>();

    // Framework and test usage tracking
    private final Set<String> usedClassesByFramework = new HashSet<>();
    private final Set<String> usedMethodsByFramework = new HashSet<>();
    private final Set<String> usedClassesByTest = new HashSet<>();
    private final Set<String> usedMethodsByTest = new HashSet<>();
    
    // Import usage tracking
    private final Map<String, Set<String>> usedImports = new HashMap<>();
    private final Map<String, Set<String>> unusedImports = new HashMap<>();

    // All method usage tracking
    private final Map<String, MethodUsage> allMethodUsages = new HashMap<>();

    /**
     * Adds a class to the dependency graph.
     */
    public void addClass(String className) {
        graph.addVertex(className);
    }

    /**
     * Adds a dependency between two classes with a label.
     */
    public void addDependency(String fromClass, String toClass, String label) {
        // Skip adding self-dependencies (where a class depends on itself)
        if (fromClass.equals(toClass)) {
            return;
        }
        
        graph.addVertex(fromClass);
        graph.addVertex(toClass);
        graph.addEdge(fromClass, toClass, new LabeledEdge(label));
    }

    /**
     * Records a method call from caller to callee.
     */
    public void addMethodCall(String callerClass, String callerMethod, String calleeClass, String calleeMethod) {
        methodCalls.add(new MethodCall(callerClass, callerMethod, calleeClass, calleeMethod));
    }

    /**
     * Returns all classes in the dependency graph.
     */
    public Set<String> getAllClasses() {
        return graph.vertexSet();
    }

    /**
     * Returns all outgoing dependencies for a class.
     */
    public Set<LabeledEdge> getDependencies(String className) {
        return graph.outgoingEdgesOf(className);
    }

    /**
     * Returns all methods called in a class. Query Ability
     */
    public Set<String> getMethodCalls(String className) {
        return methodCallsByClass.getOrDefault(className, Collections.emptySet());
    }

    /**
     * Returns all method calls in the codebase.
     */
    public List<MethodCall> getAllMethodCalls() {
        return methodCalls;
    }

    /**
     * Returns the underlying graph.
     */
    public DefaultDirectedGraph<String, LabeledEdge> getGraph() {
        return graph;
    }

    /**
     * Marks a class as used by the framework.
     */
    public void markClassUsedByFramework(String className) {
        usedClassesByFramework.add(className);
    }

    /**
     * Marks a method as used by the framework.
     */
    public void markMethodUsedByFramework(String className, String methodName) {
        usedMethodsByFramework.add(className + "." + methodName);
    }

    /**
     * Marks a class as used by tests.
     */
    public void markClassUsedByTest(String className) {
        usedClassesByTest.add(className);
    }

    /**
     * Marks a method as used by tests.
     */
    public void markMethodUsedByTest(String className, String methodName) {
        usedMethodsByTest.add(className + "." + methodName);
    }

    /**
     * Registers a method in the dependency graph.
     */
    public void registerMethod(String className, String methodName) {
        String key = getMethodKey(className, methodName);
        allMethodUsages.putIfAbsent(key, new MethodUsage(className, methodName));
    }

    /**
     * Marks a method as having a specific usage type.
     */
    public void markMethodUsage(String className, String methodName, MethodUsageType usageType) {
        registerMethod(className, methodName);
        allMethodUsages.get(getMethodKey(className, methodName)).addUsage(usageType);
    }

    /**
     * Returns all method usages in the codebase.
     */
    public Collection<MethodUsage> getAllMethodUsages() {
        return allMethodUsages.values();
    }

    /**
     * Finds all unused classes in the codebase.
     */
    public Set<String> findUnusedClasses() {
        Set<String> unused = new HashSet<>();
        for (String clazz : graph.vertexSet()) {
            boolean isReferenced = false;
            
            // Check if any class depends on this class
            for (LabeledEdge edge : graph.incomingEdgesOf(clazz)) {
                String fromClass = graph.getEdgeSource(edge);
                String label = edge.getLabel();
                
                // Consider a class used only if it's actively referenced, not just imported
                if (label.equals("REFERENCE") || 
                    label.equals("ANNOTATION_REFERENCE") || 
                    label.equals("STATIC_IMPORT")) {
                    isReferenced = true;
                    break;
                }
                
                // Explicitly check if this import is marked as used
                if ((label.equals("IMPORT") || label.equals("UNUSED_IMPORT")) && 
                    isImportUsed(fromClass, clazz)) {
                    isReferenced = true;
                    break;
                }
            }
            
            if (!isReferenced && 
                !usedClassesByFramework.contains(clazz) && 
                !usedClassesByTest.contains(clazz)) {
                unused.add(clazz);
            }
        }
        return unused;
    }

    /**
     * Finds all unused methods in the codebase.
     */
    public Set<String> findUnusedMethods() {
        return getAllMethodUsages().stream()
                .filter(usage -> !usage.hasUsage(MethodUsageType.CALLED)
                        && !usage.hasUsage(MethodUsageType.FRAMEWORK)
                        && !usage.hasUsage(MethodUsageType.TEST))
                .map(usage -> usage.getClassName() + "." + usage.getMethodName())
                .collect(Collectors.toSet());
    }

    /**
     * Checks if a class is used by the framework.
     */
    public boolean isClassUsedByFramework(String className) {
        return usedClassesByFramework.contains(className);
    }

    /**
     * Checks if a class is used by tests.
     */
    public boolean isClassUsedByTest(String className) {
        return usedClassesByTest.contains(className);
    }

    /**
     * Gets a key for a method.
     */
    private String getMethodKey(String className, String methodName) {
        return className + "." + methodName;
    }

    /**
     * Marks an import as used in a class.
     */
    public void markImportAsUsed(String className, String importName) {
        usedImports.computeIfAbsent(className, k -> new HashSet<>()).add(importName);
        
        // If it was previously marked as unused, remove it
        if (unusedImports.containsKey(className)) {
            unusedImports.get(className).remove(importName);
        }
        
        // Update edge label if exists
        updateDependencyLabel(className, importName, "REFERENCE");
    }

    /**
     * Checks if an import is used in a class.
     */
    public boolean isImportUsed(String className, String importName) {
        return usedImports.containsKey(className) && 
               usedImports.get(className).contains(importName);
    }
    
    /**
     * Gets all used imports for a class.
     */
    public Set<String> getUsedImports(String className) {
        return usedImports.getOrDefault(className, Collections.emptySet());
    }
    
    /**
     * Gets all unused imports for a class.
     */
    public Set<String> getUnusedImports(String className) {
        return unusedImports.getOrDefault(className, Collections.emptySet());
    }
    
    /**
     * Updates an existing dependency edge label.
     */
    private void updateDependencyLabel(String fromClass, String toClass, String newLabel) {
        // Only update if both vertices exist
        if (graph.containsVertex(fromClass) && graph.containsVertex(toClass)) {
            // Get the edge
            Set<LabeledEdge> edges = graph.getAllEdges(fromClass, toClass);
            if (!edges.isEmpty()) {
                LabeledEdge edge = edges.iterator().next();
                // Only update if the edge exists
                if (edge != null) {
                    edge.setLabel(newLabel);
                }
            }
        }
    }
} 