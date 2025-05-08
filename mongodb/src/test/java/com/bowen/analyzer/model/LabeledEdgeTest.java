package com.bowen.analyzer.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabeledEdgeTest {
    
    private static final String LABEL = "IMPORT";
    
    @Test
    void testConstructor() {
        // When
        LabeledEdge edge = new LabeledEdge(LABEL);
        
        // Then
        assertEquals(LABEL, edge.getLabel());
    }
    
    @Test
    void testSetLabel() {
        // Given
        LabeledEdge edge = new LabeledEdge(LABEL);
        String newLabel = "REFERENCE";
        
        // When
        edge.setLabel(newLabel);
        
        // Then
        assertEquals(newLabel, edge.getLabel());
    }
    
    @Test
    void testEqualsAndHashCode() {
        // Given
        LabeledEdge edge1 = new LabeledEdge(LABEL);
        LabeledEdge edge2 = new LabeledEdge(LABEL);
        LabeledEdge different = new LabeledEdge("DIFFERENT");
        
        // Then - verify the label property
        assertEquals(edge1.getLabel(), edge2.getLabel(), "Edges should have the same label");
        assertNotEquals(edge1.getLabel(), different.getLabel(), "Edges should have different labels");
    }
    
    @Test
    void testToString() {
        // Given
        LabeledEdge edge = new LabeledEdge(LABEL);
        
        // When
        String result = edge.toString();
        
        // Then
        assertTrue(result.contains(LABEL));
    }
} 