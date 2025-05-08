package com.bowen.analyzer.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MethodCallTest {
    
    private static final String CALLER_CLASS = "com.example.CallerClass";
    private static final String CALLER_METHOD = "callerMethod";
    private static final String CALLEE_CLASS = "com.example.CalleeClass";
    private static final String CALLEE_METHOD = "calleeMethod";
    
    @Test
    void testConstructor() {
        // When
        MethodCall methodCall = new MethodCall(CALLER_CLASS, CALLER_METHOD, CALLEE_CLASS, CALLEE_METHOD);
        
        // Then
        assertEquals(CALLER_CLASS, methodCall.getCallerClass());
        assertEquals(CALLER_METHOD, methodCall.getCallerMethod());
        assertEquals(CALLEE_CLASS, methodCall.getCalleeClass());
        assertEquals(CALLEE_METHOD, methodCall.getCalleeMethod());
    }
    
    @Test
    void testEqualsAndHashCode() {
        // Given
        MethodCall call1 = new MethodCall(CALLER_CLASS, CALLER_METHOD, CALLEE_CLASS, CALLEE_METHOD);
        MethodCall call2 = new MethodCall(CALLER_CLASS, CALLER_METHOD, CALLEE_CLASS, CALLEE_METHOD);
        MethodCall differentCaller = new MethodCall("other.Class", CALLER_METHOD, CALLEE_CLASS, CALLEE_METHOD);
        MethodCall differentCallee = new MethodCall(CALLER_CLASS, CALLER_METHOD, "other.Class", CALLEE_METHOD);
        
        // Then - compare properties individually
        assertEquals(call1.getCallerClass(), call2.getCallerClass(), "Caller classes should match");
        assertEquals(call1.getCallerMethod(), call2.getCallerMethod(), "Caller methods should match");
        assertEquals(call1.getCalleeClass(), call2.getCalleeClass(), "Callee classes should match");
        assertEquals(call1.getCalleeMethod(), call2.getCalleeMethod(), "Callee methods should match");
        
        assertNotEquals(call1.getCallerClass(), differentCaller.getCallerClass(), "Caller classes should differ");
        assertNotEquals(call1.getCalleeClass(), differentCallee.getCalleeClass(), "Callee classes should differ");
    }
    
    @Test
    void testToString() {
        // Given
        MethodCall methodCall = new MethodCall(CALLER_CLASS, CALLER_METHOD, CALLEE_CLASS, CALLEE_METHOD);
        
        // When
        String result = methodCall.toString();
        
        // Then
        assertTrue(result.contains(CALLER_CLASS));
        assertTrue(result.contains(CALLER_METHOD));
        assertTrue(result.contains(CALLEE_CLASS));
        assertTrue(result.contains(CALLEE_METHOD));
    }
} 