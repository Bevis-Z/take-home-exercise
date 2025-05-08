package com.bowen.analyzer.exporter;

import com.bowen.analyzer.model.DependencyGraph;
import com.bowen.analyzer.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExportToolTest {

    @TempDir
    Path tempDir;
    
    private File outputDir;
    private DependencyGraph graph;
    private AnalysisService service;
    
    private static final String CLASS_A = "com.example.ClassA";
    private static final String CLASS_B = "com.example.ClassB";
    private static final String METHOD_A = "methodA";
    private static final String METHOD_B = "methodB";
    
    @BeforeEach
    void setUp() {
        outputDir = tempDir.toFile();
        
        // Set up a real graph with some sample data
        graph = new DependencyGraph();
        graph.addClass(CLASS_A);
        graph.addClass(CLASS_B);
        graph.addDependency(CLASS_A, CLASS_B, "IMPORT");
        graph.addMethodCall(CLASS_A, METHOD_A, CLASS_B, METHOD_B);
        graph.markImportAsUsed(CLASS_A, CLASS_B);
        
        // Mock the analysis service
        service = Mockito.mock(AnalysisService.class);
        when(service.getGraph()).thenReturn(graph);
        
        // Setup service methods that are called during export
        Map<String, String> dependencies = new HashMap<>();
        dependencies.put(CLASS_B, "IMPORT");
        when(service.getClassDependencies(any())).thenReturn(dependencies);
        
        Map<String, List<String>> methodCallHierarchy = new HashMap<>();
        methodCallHierarchy.put(CLASS_A + "." + METHOD_A, List.of(CLASS_B + "." + METHOD_B));
        when(service.getMethodCallHierarchy()).thenReturn(methodCallHierarchy);
        
        Set<String> unusedClasses = new HashSet<>();
        when(service.getUnusedClasses()).thenReturn(unusedClasses);
        
        Set<String> unusedMethods = new HashSet<>();
        when(service.getUnusedMethods()).thenReturn(unusedMethods);
        
        Map<String, Map<String, Boolean>> usageTypes = new HashMap<>();
        Map<String, Boolean> types = new HashMap<>();
        types.put("CALLED", true);
        types.put("FRAMEWORK", false);
        types.put("TEST", false);
        usageTypes.put(CLASS_A + "." + METHOD_A, types);
        when(service.getMethodUsageTypes()).thenReturn(usageTypes);
    }
    
    @Test
    void testInitialSetup() {
        // Just a placeholder test for the setup
        assertTrue(outputDir.exists(), "Temp directory should exist");
        assertTrue(outputDir.isDirectory(), "Temp path should be a directory");
    }
    
    @Test
    void testExportDataWithMinimalGraph() throws Exception {
        // Export the data using our mocked service and real graph
        ExportTool.exportDataForFrontend(graph, service, outputDir);
        
        // Verify the JSON file was created
        File jsonFile = new File(outputDir, "code-data.json");
        assertTrue(jsonFile.exists(), "JSON file should be created");
        
        // Read the content to verify it contains expected data
        String content = Files.readString(jsonFile.toPath());
        
        // Verify key elements are in the JSON based on the actual structure
        assertTrue(content.contains(CLASS_A), "JSON should contain class A");
        assertTrue(content.contains(CLASS_B), "JSON should contain class B");
        
        // Verify the JSON structure matches what's actually generated
        assertTrue(content.contains("\"classes\""), "JSON should have classes section");
        assertTrue(content.contains("\"callGraph\""), "JSON should have callGraph section");
        assertTrue(content.contains("\"nodes\""), "JSON should have nodes in callGraph section");
        assertTrue(content.contains("\"edges\""), "JSON should have edges in callGraph section");
        assertTrue(content.contains("\"methods\""), "JSON should have methods section");
        assertTrue(content.contains("\"unusedCode\""), "JSON should have unusedCode section");
        
        // Verify the method call is present in the callGraph
        assertTrue(content.contains("\"from\" : \"" + CLASS_A + "." + METHOD_A + "\""), 
                "JSON should contain the method call source");
        assertTrue(content.contains("\"to\" : \"" + CLASS_B + "." + METHOD_B + "\""), 
                "JSON should contain the method call target");
    }
} 