package com.bowen.analyzer.model;

import org.jgrapht.graph.DefaultEdge;

/**
 * Represents an edge in the dependency graph with a label.
 */
public class LabeledEdge extends DefaultEdge {

    private String label;

    public LabeledEdge(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
} 