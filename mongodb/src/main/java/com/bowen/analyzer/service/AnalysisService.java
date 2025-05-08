package com.bowen.analyzer.service;

import com.bowen.analyzer.model.*;
import com.bowen.analyzer.model.enums.MethodUsageType;
import com.bowen.analyzer.parser.JavaProjectParser;
import com.bowen.analyzer.parser.XhtmlParser;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Service to analyze Java projects and generate reports.
 */
public class AnalysisService {
    private final DependencyGraph graph;
    private final File projectRoot;
    private final List<File> sourceRoots;

    /**
     * Creates a new analysis service.
     *
     * @param projectRoot The root directory of the project
     * @param sourceRoots The source roots to analyze
     */
    public AnalysisService(File projectRoot, List<File> sourceRoots) {
        this.projectRoot = projectRoot;
        this.sourceRoots = sourceRoots;
        this.graph = new DependencyGraph();
    }

    /**
     * Analyzes the project and generates a dependency graph.
     *
     * @return The populated dependency graph
     * @throws IOException If an I/O error occurs
     */
    public DependencyGraph analyzeProject() throws IOException {
        JavaProjectParser javaParser = new JavaProjectParser(sourceRoots, projectRoot);
        javaParser.parseProject(graph);

        // Parse XHTML files
        File webappDir = new File(projectRoot, "src/main/webapp");
        if (webappDir.exists() && webappDir.isDirectory()) {
            XhtmlParser xhtmlParser = new XhtmlParser(graph, webappDir);
            xhtmlParser.parseProject(graph);
        } else {
            System.out.println("Warning: Webapp directory not found: " + webappDir.getAbsolutePath());
        }

        return graph;
    }

    /**
     * Prints a report of the analysis.
     */
    public void printReport() {
        System.out.println("======= All Classes =======");
        graph.getAllClasses().forEach(System.out::println);

        System.out.println("\n======= Class Dependencies（IMPORT & REFERENCE） =======");
        for (String className : graph.getAllClasses()) {
            if (!className.startsWith("org.jboss.as.quickstarts.kitchensink")) {
                continue;
            }
            System.out.println("\n【" + className + " has the following dependencies】");
            getClassDependencies(className).forEach((target, type) ->
                    System.out.println("  -> " + target + " [" + type + "]"));
                    
            // Print used and unused imports
            System.out.println("\n  Used Imports:");
            getUsedImports(className).forEach(imp -> 
                    System.out.println("    ✓ " + imp));
                    
            System.out.println("\n  Unused Imports:");
            getUnusedImports(className).forEach(imp -> 
                    System.out.println("    ✗ " + imp));
        }

        System.out.println("\n======= Method Call Hierarchy =======");
        getMethodCallHierarchy().forEach((caller, callees) -> {
            StringBuilder line = new StringBuilder(caller);
            line.append(" → ");
            line.append(String.join(", ", callees));
            System.out.println(line);
        });

        System.out.println("\n======= Unused Classes =======");
        getUnusedClasses().forEach(System.out::println);

        System.out.println("\n======= Unused Methods =======");
        getUnusedMethods().forEach(System.out::println);

        System.out.println("\n======= Method Usage Types =======");
        getMethodUsageTypes().forEach((methodName, types) ->
                System.out.println(methodName + " " + types));
    }

    /* ----------  Public query helpers ---------- */

    public Set<String> getAllClasses() {
        return new HashSet<>(graph.getAllClasses());
    }

    public Map<String, String> getClassDependencies(String className) {
        Map<String, String> dependencies = new HashMap<>();
        for (LabeledEdge edge : graph.getDependencies(className)) {
            String target = graph.getGraph().getEdgeTarget(edge);
            String dependencyType = edge.getLabel();
            
            // Make sure to retain imports properly
            dependencies.put(target, dependencyType);
        }
        return dependencies;
    }

    public Map<String, List<String>> getMethodCallHierarchy() {
        Map<String, Set<String>> callsByCallerMethod = new HashMap<>();

        for (MethodCall mc : graph.getAllMethodCalls()) {
            String caller = mc.getCallerClass() + "." + mc.getCallerMethod();
            String callee = mc.getCalleeClass() + "." + mc.getCalleeMethod();
            callsByCallerMethod.computeIfAbsent(caller, k -> new HashSet<>()).add(callee);
        }

        Map<String, List<String>> sortedCalls = new TreeMap<>();
        for (Map.Entry<String, Set<String>> entry : callsByCallerMethod.entrySet()) {
            List<String> sortedCallees = new ArrayList<>(entry.getValue());
            Collections.sort(sortedCallees);
            sortedCalls.put(entry.getKey(), sortedCallees);
        }

        return sortedCalls;
    }

    public Set<String> getUnusedClasses() {
        return graph.findUnusedClasses();
    }

    public Set<String> getUnusedMethods() {
        return graph.findUnusedMethods();
    }

    public Map<String, Map<String, Boolean>> getMethodUsageTypes() {
        Map<String, Map<String, Boolean>> usageTypes = new HashMap<>();

        for (MethodUsage usage : graph.getAllMethodUsages()) {
            String methodFullName = usage.getClassName() + "." + usage.getMethodName();
            Map<String, Boolean> types = new HashMap<>();

            types.put("CALLED", usage.hasUsage(MethodUsageType.CALLED));
            types.put("FRAMEWORK", usage.hasUsage(MethodUsageType.FRAMEWORK));
            types.put("TEST", usage.hasUsage(MethodUsageType.TEST));

            usageTypes.put(methodFullName, types);
        }

        return usageTypes;
    }

    /**
     * Determines the impact radius for a class.
     * @param className The class name
     * @return Map of dependent class to dependency path
     */
    public Map<String, List<String>> determineImpactRadius(String className) {
        Map<String, List<String>> impactMap = new HashMap<>();
        Set<String> visited = new HashSet<>();

        Deque<String> path = new ArrayDeque<>();
        path.add(className);    // 根节点在末尾

        for (LabeledEdge edge : graph.getGraph().incomingEdgesOf(className)) {
            String dependent = graph.getGraph().getEdgeSource(edge);
            determineDependencyPath(dependent, impactMap, visited, path);
        }

        return impactMap;
    }

    /**
     * Recursively walks up the dependency graph, building paths.
     */
    private void determineDependencyPath(String current,
                                         Map<String, List<String>> impactMap,
                                         Set<String> visited,
                                         Deque<String> path) {
        if (!visited.add(current)) {
            return; // 已访问，避免循环
        }

        path.addFirst(current);                       // 前插
        impactMap.put(current, List.copyOf(path));    // 存副本

        for (LabeledEdge edge : graph.getGraph().incomingEdgesOf(current)) {
            String dependent = graph.getGraph().getEdgeSource(edge);
            determineDependencyPath(dependent, impactMap, visited, path);
        }

        path.removeFirst();                           // 回溯
    }

    /**
     * Determines the impact radius for a method.
     * @param methodFullName The fully qualified method name (className.methodName)
     * @return Map of dependent methods to dependency path
     */
    public Map<String, List<String>> determineMethodImpactRadius(String methodFullName) {
        Map<String, List<String>> impactMap = new HashMap<>();
        Set<String> visited = new HashSet<>();
        
        // Get all method calls
        Map<String, List<String>> methodCalls = getMethodCallHierarchy();
        
        // Find all methods that call this method directly
        List<String> directCallers = findDirectMethodCallers(methodFullName);
        
        // For each caller, trace the impact
        for (String caller : directCallers) {
            List<String> path = new ArrayList<>();
            path.add(methodFullName);
            determineMethodDependencyPath(caller, methodFullName, impactMap, visited, path, methodCalls);
        }
        
        return impactMap;
    }

    /**
     * Finds all methods that directly call the given method.
     * @param methodFullName The method being called
     * @return List of methods that call this method
     */
    public List<String> findDirectMethodCallers(String methodFullName) {
        List<String> callers = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : getMethodCallHierarchy().entrySet()) {
            if (entry.getValue().contains(methodFullName)) {
                callers.add(entry.getKey());
            }
        }
        
        return callers;
    }

    /**
     * Recursively determines the dependency path for methods.
     */
    private void determineMethodDependencyPath(String current, String target,
                                            Map<String, List<String>> impactMap,
                                            Set<String> visited, List<String> currentPath,
                                            Map<String, List<String>> methodCalls) {
        if (visited.contains(current)) {
            return;
        }
        
        visited.add(current);
        
        List<String> path = new ArrayList<>(currentPath);
        path.add(0, current);
        impactMap.put(current, path);
        
        // Find methods that call this method
        for (Map.Entry<String, List<String>> entry : methodCalls.entrySet()) {
            if (entry.getValue().contains(current)) {
                determineMethodDependencyPath(entry.getKey(), target, impactMap, visited, path, methodCalls);
            }
        }
    }


    public List<MethodUsage> getAllMethodUsages() {
        return new ArrayList<>(graph.getAllMethodUsages());
    }

    public boolean isClassUsedByFramework(String className) {
        return graph.isClassUsedByFramework(className);
    }

    public boolean isClassUsedByTest(String className) {
        return graph.isClassUsedByTest(className);
    }

    /**
     * Gets information about the class's used imports.
     */
    public Set<String> getUsedImports(String className) {
        return graph.getUsedImports(className);
    }
    
    /**
     * Gets information about the class's unused imports.
     */
    public Set<String> getUnusedImports(String className) {
        return graph.getUnusedImports(className);
    }

    public DependencyGraph getGraph() {
        return graph;
    }
}