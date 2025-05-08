package com.bowen.analyzer.model;

import com.bowen.analyzer.model.enums.MethodUsageType;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents usage information for a specific method.
 */
public class MethodUsage {
    private final String className;
    private final String methodName;
    private final Set<MethodUsageType> usages = new HashSet<>();

    public MethodUsage(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public void addUsage(MethodUsageType type) {
        usages.add(type);
    }

    public boolean hasUsage(MethodUsageType type) {
        return usages.contains(type);
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        return className + "." + methodName;
    }
} 