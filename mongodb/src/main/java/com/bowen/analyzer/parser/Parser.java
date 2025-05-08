package com.bowen.analyzer.parser;

import com.bowen.analyzer.model.DependencyGraph;

import java.io.IOException;

/**
 * Interface for all project code parsers.
 */
public interface Parser {
    
    /**
     * Parse a project and populate the dependency graph with findings.
     * 
     * @param graph The dependency graph to populate
     * @throws IOException If an I/O error occurs
     */
    void parseProject(DependencyGraph graph) throws IOException;
} 