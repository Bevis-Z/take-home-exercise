package com.bowen.analyzer.model;

/**
 * Represents a method call from one method to another.
 */
public class MethodCall {
    private final String callerClass;
    private final String callerMethod;
    private final String calleeClass;
    private final String calleeMethod;

    public MethodCall(String callerClass, String callerMethod, String calleeClass, String calleeMethod) {
        this.callerClass = callerClass;
        this.callerMethod = callerMethod;
        this.calleeClass = calleeClass;
        this.calleeMethod = calleeMethod;
    }

    public String getCallerClass() {
        return callerClass;
    }

    public String getCallerMethod() {
        return callerMethod;
    }

    public String getCalleeClass() {
        return calleeClass;
    }

    public String getCalleeMethod() {
        return calleeMethod;
    }

    @Override
    public String toString() {
        return callerClass + "." + callerMethod + " → " + calleeClass + "." + calleeMethod;
    }
} 