package com.bowen.analyzer.model;

import com.bowen.analyzer.model.enums.MethodUsageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MethodUsageTest {
    
    private static final String CLASS_NAME = "com.example.TestClass";
    private static final String METHOD_NAME = "testMethod";
    
    @Test
    void testConstructor() {
        // When
        MethodUsage usage = new MethodUsage(CLASS_NAME, METHOD_NAME);
        
        // Then
        assertEquals(CLASS_NAME, usage.getClassName());
        assertEquals(METHOD_NAME, usage.getMethodName());
        assertFalse(usage.hasUsage(MethodUsageType.CALLED));
        assertFalse(usage.hasUsage(MethodUsageType.FRAMEWORK));
        assertFalse(usage.hasUsage(MethodUsageType.TEST));
    }
    
    @Test
    void testAddUsage() {
        // Given
        MethodUsage usage = new MethodUsage(CLASS_NAME, METHOD_NAME);
        
        // When
        usage.addUsage(MethodUsageType.CALLED);
        
        // Then
        assertTrue(usage.hasUsage(MethodUsageType.CALLED));
        assertFalse(usage.hasUsage(MethodUsageType.FRAMEWORK));
        
        // When adding another usage type
        usage.addUsage(MethodUsageType.FRAMEWORK);
        
        // Then both should be true
        assertTrue(usage.hasUsage(MethodUsageType.CALLED));
        assertTrue(usage.hasUsage(MethodUsageType.FRAMEWORK));
    }
    
    @Test
    void testEqualsAndHashCode() {
        // Given
        MethodUsage usage1 = new MethodUsage(CLASS_NAME, METHOD_NAME);
        MethodUsage usage2 = new MethodUsage(CLASS_NAME, METHOD_NAME);
        MethodUsage differentMethod = new MethodUsage(CLASS_NAME, "differentMethod");
        MethodUsage differentClass = new MethodUsage("com.example.OtherClass", METHOD_NAME);
        
        // Then - compare properties directly
        assertEquals(usage1.getClassName(), usage2.getClassName(), "Class names should match");
        assertEquals(usage1.getMethodName(), usage2.getMethodName(), "Method names should match");
        
        assertNotEquals(usage1.getMethodName(), differentMethod.getMethodName(), "Method names should differ");
        assertNotEquals(usage1.getClassName(), differentClass.getClassName(), "Class names should differ");
        
        // Usage types shouldn't affect equality
        usage1.addUsage(MethodUsageType.CALLED);
        assertEquals(usage1.getClassName(), usage2.getClassName(), "Class names should still match after adding usage type");
        assertEquals(usage1.getMethodName(), usage2.getMethodName(), "Method names should still match after adding usage type");
    }
    
    @Test
    void testToString() {
        // Given
        MethodUsage usage = new MethodUsage(CLASS_NAME, METHOD_NAME);
        
        // When
        String result = usage.toString();
        
        // Then
        assertTrue(result.contains(CLASS_NAME));
        assertTrue(result.contains(METHOD_NAME));
    }
} 