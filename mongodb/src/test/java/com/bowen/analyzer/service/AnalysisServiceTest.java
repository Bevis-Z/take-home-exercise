package com.bowen.analyzer.service;

import com.bowen.analyzer.model.*;
import com.bowen.analyzer.model.enums.MethodUsageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalysisServiceTest {

    private AnalysisService analysisService;
    private DependencyGraph mockGraph;
    
    @TempDir
    Path tempDir;
    
    private File projectRoot;
    private List<File> sourceRoots;
    
    private static final String CLASS_A = "com.example.ClassA";
    private static final String CLASS_B = "com.example.ClassB";
    private static final String METHOD_A = "methodA";
    private static final String METHOD_B = "methodB";
    
    @BeforeEach
    void setUp() throws IOException {
        projectRoot = tempDir.toFile();
        
        // Create source directories
        File srcMainJava = new File(projectRoot, "src/main/java");
        srcMainJava.mkdirs();
        
        File srcTestJava = new File(projectRoot, "src/test/java");
        srcTestJava.mkdirs();
        
        sourceRoots = Arrays.asList(srcMainJava, srcTestJava);
        
        // Create a real service with a clean graph for proper testing
        analysisService = new AnalysisService(projectRoot, sourceRoots);
    }
    
    @Test
    void testConstructorInitializesGraph() {
        // Test that the analysis service properly initializes the graph
        assertNotNull(analysisService.getGraph());
        assertTrue(analysisService.getGraph() instanceof DependencyGraph);
    }
    
    // Test simple methods that don't require complex setup
    @Test
    void testGetAllClasses() {
        // Act
        Set<String> classes = analysisService.getAllClasses();
        
        // Assert
        assertNotNull(classes);
        // Initially, it should be empty as no analysis has been run
        assertTrue(classes.isEmpty());
    }
    
    @Test
    void testGetMethodCallHierarchy() {
        // Act
        Map<String, List<String>> hierarchy = analysisService.getMethodCallHierarchy();
        
        // Assert
        assertNotNull(hierarchy);
        // Initially, it should be empty as no analysis has been run
        assertTrue(hierarchy.isEmpty());
    }
    
    @Test
    void testGetUnusedClasses() {
        // Act
        Set<String> unusedClasses = analysisService.getUnusedClasses();
        
        // Assert
        assertNotNull(unusedClasses);
        // Initially, it should be empty as no analysis has been run
        assertTrue(unusedClasses.isEmpty());
    }
    
    @Test
    void testGetUnusedMethods() {
        // Act
        Set<String> unusedMethods = analysisService.getUnusedMethods();
        
        // Assert
        assertNotNull(unusedMethods);
        // Initially, it should be empty as no analysis has been run
        assertTrue(unusedMethods.isEmpty());
    }
    
    @Test
    void testGetMethodUsageTypes() {
        // Act
        Map<String, Map<String, Boolean>> usageTypes = analysisService.getMethodUsageTypes();
        
        // Assert
        assertNotNull(usageTypes);
        // Initially, it should be empty as no analysis has been run
        assertTrue(usageTypes.isEmpty());
    }
    
    @Test
    void testGetUsedImports() {
        // Act
        Set<String> usedImports = analysisService.getUsedImports(CLASS_A);
        
        // Assert
        assertNotNull(usedImports);
        // Initially, it should be empty as no analysis has been run
        assertTrue(usedImports.isEmpty());
    }
    
    @Test
    void testGetUnusedImports() {
        // Act
        Set<String> unusedImports = analysisService.getUnusedImports(CLASS_A);
        
        // Assert
        assertNotNull(unusedImports);
        // Initially, it should be empty as no analysis has been run
        assertTrue(unusedImports.isEmpty());
    }
    
    @Test
    void testIsClassUsedByFramework() {
        // Act
        boolean isUsed = analysisService.isClassUsedByFramework(CLASS_A);
        
        // Assert
        // Initially, it should be false as no analysis has been run
        assertFalse(isUsed);
    }
    
    @Test
    void testIsClassUsedByTest() {
        // Act
        boolean isUsed = analysisService.isClassUsedByTest(CLASS_A);
        
        // Assert
        // Initially, it should be false as no analysis has been run
        assertFalse(isUsed);
    }
} 