package com.bowen;

import com.bowen.analyzer.exporter.ExportTool;
import com.bowen.analyzer.model.DependencyGraph;
import com.bowen.analyzer.service.AnalysisService;

import java.io.File;
import java.util.List;

/**
 * Main entry point for the Java code analyzer that combines
 * analysis and JSON export in a single execution.
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        // Parse common arguments
        String projectPath = args.length > 0 ? args[0] : "kitchensink";
        String outputDir = args.length > 1 ? args[1] : "mongodb/frontend/public/data";

        File projectRoot = new File(projectPath);
        File outputDirectory = new File(outputDir);

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        // Define source code root directories
        List<File> sourceRoots = List.of(
                new File(projectRoot, "src/main/java"),
                new File(projectRoot, "src/test/java")
        );

        System.out.println("Analyzing project: " + projectRoot.getAbsolutePath());

        // Step 1: Run the analysis
        AnalysisService analysisService = new AnalysisService(projectRoot, sourceRoots);
        DependencyGraph graph = analysisService.analyzeProject();
        
        // Print analysis report
        System.out.println("\n======= Analysis Report =======");
        analysisService.printReport();
        
        // Step 3: Export to JSON
        System.out.println("\n======= Exporting Data =======");
        System.out.println("Exporting data to: " + outputDirectory.getAbsolutePath());
        
        ExportTool.exportDataForFrontend(graph, analysisService, outputDirectory);
        
        System.out.println("Export complete. JSON data available at: " + new File(outputDirectory, "code-data.json").getAbsolutePath());
    }
}