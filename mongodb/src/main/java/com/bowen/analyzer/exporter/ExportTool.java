package com.bowen.analyzer.exporter;

import com.bowen.analyzer.model.DependencyGraph;
import com.bowen.analyzer.model.MethodUsage;
import com.bowen.analyzer.service.AnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.*;

/**
 * Tool for exporting dependency graph data to JSON
 */
public class ExportTool {

    /**
     * Exports the data structure needed by the frontend
     */
    public static void exportDataForFrontend(DependencyGraph graph,
                                             AnalysisService analysisService,
                                             File outputDirectory) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        /* ────── root‑level collections for the frontend ────── */
        ArrayNode classesArray     = rootNode.putArray("classes");
        ArrayNode methodsArrayRoot = rootNode.putArray("methods");

        ObjectNode callGraphNode   = rootNode.putObject("callGraph");
        ArrayNode  cgNodes         = callGraphNode.putArray("nodes");
        ArrayNode  cgEdges         = callGraphNode.putArray("edges");

        Set<String> cgNodeIds = new HashSet<>();

        /* ---------------------------------- */
        /*  1.  Classes                       */
        /* ---------------------------------- */

        Map<String, ObjectNode> classNodesMap = new HashMap<>();
        Set<String> allClasses = graph.getAllClasses();

        for (String className : allClasses) {
            ObjectNode classNode = classesArray.addObject();
            classNode.put("id", className);
            classNode.put("fullName", className);

            int lastDot = className.lastIndexOf('.');
            String simpleName  = lastDot > 0 ? className.substring(lastDot + 1) : className;
            String packageName = lastDot > 0 ? className.substring(0, lastDot) : "";

            classNode.put("simpleName", simpleName);
            classNode.put("packageName", packageName);

            boolean isUsedByFramework = analysisService.isClassUsedByFramework(className);
            boolean isUsedByTest = analysisService.isClassUsedByTest(className);
            boolean hasIncomingEdges = !graph.getGraph().incomingEdgesOf(className).isEmpty();
            boolean unused = !isUsedByFramework && !isUsedByTest && !hasIncomingEdges;

            classNode.put("unused",    unused);
            classNode.put("framework", isUsedByFramework);
            classNode.put("test",      isUsedByTest);

            // Empty dependencies array, will be filled later
            classNode.putArray("dependsOn");
            
            // Add arrays for tracking used and unused imports
            classNode.putArray("usedImports");
            classNode.putArray("unusedImports");

            classNodesMap.put(className, classNode);
        }

        /* Fill dependencies */
        for (String className : allClasses) {
            ObjectNode classNode = classNodesMap.get(className);
            ArrayNode  dependsOn = (ArrayNode) classNode.get("dependsOn");
            ArrayNode  usedImports = (ArrayNode) classNode.get("usedImports");
            ArrayNode  unusedImports = (ArrayNode) classNode.get("unusedImports");
            
            Map<String, String> dependencies = analysisService.getClassDependencies(className);
            
            // Get used and unused imports
            Set<String> usedImportsList = graph.getUsedImports(className);
            Set<String> unusedImportsList = graph.getUnusedImports(className);
            
            // Add used imports
            for (String importName : usedImportsList) {
                ObjectNode importNode = usedImports.addObject();
                importNode.put("name", importName);
                importNode.put("type", "IMPORT");
            }
            
            // Add unused imports (keeping them as "IMPORT" type)
            for (String importName : unusedImportsList) {
                ObjectNode importNode = unusedImports.addObject();
                importNode.put("name", importName);
                importNode.put("type", "IMPORT");
            }

            List<Map.Entry<String, String>> entryList = new ArrayList<>(dependencies.entrySet());

            entryList.sort(Map.Entry.comparingByValue());

            for (Map.Entry<String, String> entry : entryList) {
                String targetClass = entry.getKey();
                String dependencyType = entry.getValue();

                ObjectNode depNode = dependsOn.addObject();
                depNode.put("target", targetClass);
                depNode.put("type", dependencyType);
            }
        }

        /* ---------------------------------- */
        /*  2.  Methods                       */
        /* ---------------------------------- */

        Map<String, ObjectNode> methodMapByFullName = new HashMap<>();
        List<MethodUsage> allMethodUsages = analysisService.getAllMethodUsages();

        for (MethodUsage usage : allMethodUsages) {
            String className      = usage.getClassName();
            String methodName     = usage.getMethodName();
            String fullMethodName = className + "." + methodName;

            ObjectNode methodNode = methodsArrayRoot.addObject();
            methodNode.put("declaringClass", className);
            methodNode.put("name",           methodName);
            methodNode.put("fullName",       fullMethodName);
            
            Map<String, Boolean> usageTypes = analysisService.getMethodUsageTypes().get(fullMethodName);
            boolean called = usageTypes.get("CALLED");
            boolean framework = usageTypes.get("FRAMEWORK");
            boolean test = usageTypes.get("TEST");
            boolean unused = !called && !framework && !test;
            
            methodNode.put("called", called);
            methodNode.put("framework", framework);
            methodNode.put("test", test);
            methodNode.put("unused", unused);

            // Empty calls array, will be filled later
            methodNode.putArray("calls");

            methodMapByFullName.put(fullMethodName, methodNode);

            /* Register as call-graph node */
            if (cgNodeIds.add(fullMethodName)) {
                ObjectNode n = cgNodes.addObject();
                n.put("id",   fullMethodName);
                n.put("type", "method");
            }
        }

        /* ---------------------------------- */
        /*  3.  Call graph edges              */
        /* ---------------------------------- */

        Map<String, List<String>> methodCallHierarchy = analysisService.getMethodCallHierarchy();
        
        // Process all method calls from the hierarchy
        for (Map.Entry<String, List<String>> entry : methodCallHierarchy.entrySet()) {
            String caller = entry.getKey();
            List<String> callees = entry.getValue();
            
            // Create a Set to remove duplicates
            Set<String> uniqueCallees = new HashSet<>(callees);
            
            for (String callee : uniqueCallees) {
                // Add edge
                ObjectNode edge = cgEdges.addObject();
                edge.put("from", caller);
                edge.put("to",   callee);
                
                // Ensure callee node exists
                if (cgNodeIds.add(callee)) {
                    ObjectNode n = cgNodes.addObject();
                    n.put("id",   callee);
                    n.put("type", "method");
                }
                // Ensure caller node exists
                if (cgNodeIds.add(caller)) {
                    ObjectNode n = cgNodes.addObject();
                    n.put("id",   caller);
                    n.put("type", "method");
                }
                
                // Add to caller's calls[]
                ObjectNode callerNode = methodMapByFullName.get(caller);
                if (callerNode != null) {
                    ((ArrayNode) callerNode.get("calls")).add(callee);
                }
            }
        }

        /* ---------------------------------- */
        /*  4.  Unused Code Report            */
        /* ---------------------------------- */

        ObjectNode unusedCodeNode = rootNode.putObject("unusedCode");
        ArrayNode  unusedClasses  = unusedCodeNode.putArray("classes");
        ArrayNode  unusedMethods  = unusedCodeNode.putArray("methods");

        // Unused classes
        Set<String> unusedClassSet = analysisService.getUnusedClasses();
        for (String className : unusedClassSet) {
            ObjectNode uc = unusedClasses.addObject();
            uc.put("id",       className);
            uc.put("fullName", className);
            
            boolean usedByTest = analysisService.isClassUsedByTest(className);
            if (usedByTest) {
                uc.put("reason", "only used in Test");
            } else {
                uc.put("reason", "No other classes depends on this class");
            }
        }

        // Unused methods
        Set<String> unusedMethodSet = analysisService.getUnusedMethods();
        Map<String, Map<String, Boolean>> methodUsageTypes = analysisService.getMethodUsageTypes();
        
        for (String methodFullName : unusedMethodSet) {
            int lastDot = methodFullName.lastIndexOf('.');
            String className = methodFullName.substring(0, lastDot);
            String methodName = methodFullName.substring(lastDot + 1);
            
            ObjectNode um = unusedMethods.addObject();
            um.put("id", methodFullName);
            um.put("className", className);
            um.put("methodName", methodName);
            
            Map<String, Boolean> usageTypes = methodUsageTypes.get(methodFullName);
            if (usageTypes != null && usageTypes.get("TEST")) {
                um.put("reason", "only used in Test");
            } else {
                um.put("reason", "No other classes depends on this class");
            }
        }

        /* ---------------------------------- */
        /*  5.  Impact analysis               */
        /* ---------------------------------- */

        ArrayNode impactArray = rootNode.putArray("impactAnalysis");
        
        // Class impact analysis
        for (String className : allClasses) {
            Map<String, List<String>> impactMap = analysisService.determineImpactRadius(className);
            if (impactMap.isEmpty()) continue;

            ObjectNode impactNode = impactArray.addObject();
            impactNode.put("class", className);
            impactNode.put("type", "class");

            ObjectNode ir = impactNode.putObject("impactRadius");
            ArrayNode directArr = ir.putArray("directlyAffected");
            Set<String> directSet = new HashSet<>();

            // Get directly affected classes (immediate dependents)
            for (var edge : graph.getGraph().incomingEdgesOf(className)) {
                String dep = graph.getGraph().getEdgeSource(edge);
                directSet.add(dep);
                directArr.add(dep);
            }

            // Get indirectly affected classes (transitive dependents)
            ArrayNode indirectArr = ir.putArray("indirectlyAffected");
            for (String k : impactMap.keySet()) {
                if (!directSet.contains(k)) indirectArr.add(k);
            }

            int total = impactMap.size();
            ir.put("totalImpact", total);

            String severity = total > 10 ? "HIGH"
                    : total > 5  ? "MEDIUM"
                    : total > 0  ? "LOW"
                    : "NONE";
            ir.put("severityLevel", severity);
        }

        // Method impact analysis
        Set<String> methodNames = new HashSet<>();
        for (MethodUsage usage : analysisService.getAllMethodUsages()) {
            methodNames.add(usage.getClassName() + "." + usage.getMethodName());
        }

        // Analyze impact for methods
        for (String methodName : methodNames) {
            Map<String, List<String>> methodImpactMap = analysisService.determineMethodImpactRadius(methodName);
            if (methodImpactMap.isEmpty()) continue;

            ObjectNode impactNode = impactArray.addObject();
            impactNode.put("method", methodName);
            impactNode.put("type", "method");

            ObjectNode ir = impactNode.putObject("impactRadius");
            ArrayNode directArr = ir.putArray("directlyAffected");
            Set<String> directSet = new HashSet<>();

            // Get direct callers
            List<String> directCallers = analysisService.findDirectMethodCallers(methodName);
            for (String caller : directCallers) {
                directSet.add(caller);
                directArr.add(caller);
            }

            // Get indirect callers (transitive dependents)
            ArrayNode indirectArr = ir.putArray("indirectlyAffected");
            for (String k : methodImpactMap.keySet()) {
                if (!directSet.contains(k)) indirectArr.add(k);
            }

            int total = methodImpactMap.size();
            ir.put("totalImpact", total);

            String severity = total > 15 ? "CRITICAL"
                    : total > 10 ? "HIGH"
                    : total > 5  ? "MEDIUM"
                    : total > 0  ? "LOW"
                    : "NONE";
            ir.put("severityLevel", severity);
        }

        /* ---------------------------------- */
        /*  6.  Write to disk                 */
        /* ---------------------------------- */

        File dataFile = new File(outputDirectory, "code-data.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, rootNode);
    }
}