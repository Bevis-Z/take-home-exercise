package com.bowen.analyzer.model;

import com.bowen.analyzer.model.enums.MethodUsageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DependencyGraphTest {

    private DependencyGraph graph;
    private static final String CLASS_A = "com.example.ClassA";
    private static final String CLASS_B = "com.example.ClassB";
    private static final String METHOD_A = "methodA";
    private static final String METHOD_B = "methodB";

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
    }

    @Test
    void testAddClass() {
        // When
        graph.addClass(CLASS_A);
        
        // Then
        assertTrue(graph.getAllClasses().contains(CLASS_A));
        assertEquals(1, graph.getAllClasses().size());
    }

    @Test
    void testAddDependency() {
        // When
        graph.addDependency(CLASS_A, CLASS_B, "IMPORT");
        
        // Then
        assertTrue(graph.getAllClasses().contains(CLASS_A));
        assertTrue(graph.getAllClasses().contains(CLASS_B));
        assertEquals(1, graph.getDependencies(CLASS_A).size());
        
        LabeledEdge edge = graph.getDependencies(CLASS_A).iterator().next();
        assertEquals("IMPORT", edge.getLabel());
    }

    @Test
    void testAddMethodCall() {
        // When
        graph.addMethodCall(CLASS_A, METHOD_A, CLASS_B, METHOD_B);
        
        // Then
        assertEquals(1, graph.getAllMethodCalls().size());
        MethodCall call = graph.getAllMethodCalls().get(0);
        assertEquals(CLASS_A, call.getCallerClass());
        assertEquals(METHOD_A, call.getCallerMethod());
        assertEquals(CLASS_B, call.getCalleeClass());
        assertEquals(METHOD_B, call.getCalleeMethod());
    }

    @Test
    void testMethodUsage() {
        // Given
        graph.registerMethod(CLASS_A, METHOD_A);
        
        // When
        graph.markMethodUsage(CLASS_A, METHOD_A, MethodUsageType.CALLED);
        
        // Then
        Collection<MethodUsage> usages = graph.getAllMethodUsages();
        assertEquals(1, usages.size());
        MethodUsage usage = usages.iterator().next();
        assertEquals(CLASS_A, usage.getClassName());
        assertEquals(METHOD_A, usage.getMethodName());
        assertTrue(usage.hasUsage(MethodUsageType.CALLED));
        assertFalse(usage.hasUsage(MethodUsageType.FRAMEWORK));
    }

    @Test
    void testFindUnusedClasses() {
        // Given
        graph.addClass(CLASS_A);
        graph.addClass(CLASS_B);
        graph.addDependency(CLASS_A, CLASS_B, "REFERENCE");
        
        // When
        Set<String> unusedClasses = graph.findUnusedClasses();
        
        // Then
        assertTrue(unusedClasses.contains(CLASS_A));
        assertFalse(unusedClasses.contains(CLASS_B));
    }

    @Test
    void testFindUnusedMethods() {
        // Given
        graph.registerMethod(CLASS_A, METHOD_A);
        graph.registerMethod(CLASS_B, METHOD_B);
        graph.markMethodUsage(CLASS_A, METHOD_A, MethodUsageType.CALLED);
        
        // When
        Set<String> unusedMethods = graph.findUnusedMethods();
        
        // Then
        assertFalse(unusedMethods.contains(CLASS_A + "." + METHOD_A));
        assertTrue(unusedMethods.contains(CLASS_B + "." + METHOD_B));
    }

    @Test
    void testMarkImportAsUsedAndUnused() {
        // Given
        graph.addClass(CLASS_A);
        graph.addClass(CLASS_B);
        
        // When
        graph.markImportAsUsed(CLASS_A, CLASS_B);
        
        // Then
        assertTrue(graph.isImportUsed(CLASS_A, CLASS_B));
        assertTrue(graph.getUsedImports(CLASS_A).contains(CLASS_B));
        assertFalse(graph.getUnusedImports(CLASS_A).contains(CLASS_B));

        // Test another import that's not used
        String classC = "com.example.ClassC";
        graph.addClass(classC);
        graph.addDependency(CLASS_A, classC, "IMPORT");
        assertFalse(graph.isImportUsed(CLASS_A, classC));
        assertFalse(graph.getUsedImports(CLASS_A).contains(classC));
    }

    @Test
    void testMarkClassUsedByFramework() {
        // When
        graph.markClassUsedByFramework(CLASS_A);
        
        // Then
        assertTrue(graph.isClassUsedByFramework(CLASS_A));
        assertFalse(graph.isClassUsedByFramework(CLASS_B));
    }

    @Test
    void testMarkClassUsedByTest() {
        // When
        graph.markClassUsedByTest(CLASS_A);
        
        // Then
        assertTrue(graph.isClassUsedByTest(CLASS_A));
        assertFalse(graph.isClassUsedByTest(CLASS_B));
    }

    @Test
    void testGetGraph() {
        // When
        DefaultDirectedGraph<String, LabeledEdge> jgrapht = graph.getGraph();
        
        // Then
        assertNotNull(jgrapht);
        assertTrue(jgrapht instanceof DefaultDirectedGraph);
    }
} 