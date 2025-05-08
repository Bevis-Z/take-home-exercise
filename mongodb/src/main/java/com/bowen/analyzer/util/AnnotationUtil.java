package com.bowen.analyzer.util;

import java.util.List;

/**
 * Utility class for handling Java annotations.
 */
public class AnnotationUtil {

    /**
     * List of common framework annotations that indicate a class or method is used by a framework.
     */
    private static final List<String> INJECTIONWithREST_ANNOTATIONS = List.of(
            // REST annotations
            "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH",
            "Path", "Produces", "Consumes", "PathParam", "QueryParam", "FormParam",
            "RestController", "Controller", "RequestMapping", "RequestBody", "ApplicationPath",
            
            // CDI/Injection annotations  
            "Autowired", "Inject", "Qualifier", "Named", "Produces", "Observes",
            "ApplicationScoped", "RequestScoped", "SessionScoped", "ConversationScoped",
            
            // Bean validations
            "Valid", "NotNull", "NotEmpty", "NotBlank", "Size", "Min", "Max", "Pattern",
            
            // JPA/Persistence
            "Entity", "Stateless", "Stateful", "Singleton", "MessageDriven",
            "Table", "Column", "Id", "GeneratedValue", "ManyToOne", "OneToMany", 
            "ManyToMany", "OneToOne", "JoinColumn", "JoinTable", "Transactional",
            
            // Common qualifiers
            "Default", "Alternative", "Model", "Repository", "Service", "Component",
            
            // Events and bindings
            "Observes", "ObservesAsync", "Reception", "TransactionPhase"
    );

    /**
     * List of common test annotations.
     */
    private static final List<String> TEST_ANNOTATIONS = List.of(
            "Test", "Before", "After", "BeforeEach", "AfterEach", "BeforeAll", "AfterAll",
            "Deployment", "RunWith", "Rule", "ClassRule", "ExtendWith", "Timeout",
            "DisplayName", "Disabled", "DisabledOnOs", "EnabledOnOs", "Tag", 
            "Nested", "ParameterizedTest", "RepeatedTest", "TestFactory"
    );

    /**
     * Checks if an annotation name indicates framework usage.
     *
     * @param name The annotation name
     * @return true if it's a framework annotation
     */
    public static boolean isInjectionAndRestAnnotation(String name) {
        // Strip any package prefix to handle both qualified and simple names
        String simpleName = name;
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            simpleName = name.substring(lastDot + 1);
        }
        
        return INJECTIONWithREST_ANNOTATIONS.contains(simpleName);
    }

    /**
     * Checks if an annotation name indicates test usage.
     *
     * @param name The annotation name
     * @return true if it's a test annotation
     */
    public static boolean isTestAnnotation(String name) {
        // Strip any package prefix to handle both qualified and simple names
        String simpleName = name;
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            simpleName = name.substring(lastDot + 1);
        }
        
        return TEST_ANNOTATIONS.contains(simpleName);
    }
} 