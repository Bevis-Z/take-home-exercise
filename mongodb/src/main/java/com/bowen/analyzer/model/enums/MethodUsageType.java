package com.bowen.analyzer.model.enums;

/**
 * Represents how a method is being used in the codebase.
 */
public enum MethodUsageType {
    /**
     * Directly called from Java code
     */
    CALLED,
    
    /**
     * Used by a framework (via annotations, EL expressions, etc.)
     */
    FRAMEWORK,
    
    /**
     * Used in test code
     */
    TEST
} 