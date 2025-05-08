package com.bowen;

import com.bowen.analyzer.model.DependencyGraph;
import com.bowen.analyzer.service.AnalysisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @TempDir
    Path tempDir;
    private File projectRoot;
    private File outputDir;
    
    @BeforeEach
    void setUp() throws IOException {
        System.setOut(new PrintStream(outContent));
        
        // Create test directories
        projectRoot = tempDir.resolve("project").toFile();
        projectRoot.mkdir();
        
        // Create src directories
        new File(projectRoot, "src/main/java").mkdirs();
        new File(projectRoot, "src/test/java").mkdirs();
        
        // Create output directory
        outputDir = tempDir.resolve("output").toFile();
        outputDir.mkdir();
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }
    
    @Test
    void testMainWithDefaultArguments() throws Exception {
        // Execute the main method with default arguments
        String[] args = {};
        Main.main(args);
        
        // Capture output and verify it contains expected messages
        String output = outContent.toString();
        assertTrue(output.contains("Analyzing project:"), "Should show analyzing message");
        assertTrue(output.contains("kitchensink"), "Should use default project path");
        assertTrue(output.contains("Analysis Report"), "Should include analysis report");
    }
    
    @Test
    void testMainWithCustomArguments() throws Exception {
        // Execute the main method with custom arguments
        String[] args = {projectRoot.getAbsolutePath(), outputDir.getAbsolutePath()};
        Main.main(args);
        
        // Capture output and verify it contains expected messages
        String output = outContent.toString();
        assertTrue(output.contains("Analyzing project: " + projectRoot.getAbsolutePath()), 
                "Should show analyzing message with custom path");
        assertTrue(output.contains("Exporting data to: " + outputDir.getAbsolutePath()), 
                "Should use custom output directory");
    }
    
    @Test
    void testMainCreatesOutputDirectoryIfNotExists() throws Exception {
        // Delete output directory to test creation
        outputDir.delete();
        assertFalse(outputDir.exists());
        
        // Execute main with output directory that doesn't exist
        String[] args = {projectRoot.getAbsolutePath(), outputDir.getAbsolutePath()};
        Main.main(args);
        
        // Verify the directory was created
        assertTrue(outputDir.exists(), "Output directory should be created");
    }
} 