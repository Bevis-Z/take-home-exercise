package com.bowen.analyzer.parser;

import com.bowen.analyzer.model.DependencyGraph;
import com.bowen.analyzer.model.enums.MethodUsageType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for XHTML files to detect JSF and EL expressions.
 */
public class XhtmlParser implements Parser {

    private final DependencyGraph graph;
    private final File webappDir;

    // EL built-in objects and keywords that should not be considered beans
    private static final Set<String> RESERVED_WORDS = Set.of(
            "empty", "not", "request", "session", "param", "application", "view", "flash",
            "requestScope", "sessionScope", "applicationScope", "flashScope", "viewScope"
    );

    // Page-local variables used in dataTable or other loops
    private final Set<String> pageVars = new HashSet<>();

    /**
     * Creates a new XHTML parser.
     *
     * @param graph The dependency graph to update
     * @param webappDir The webapp directory containing XHTML files
     */
    public XhtmlParser(DependencyGraph graph, File webappDir) {
        this.graph = graph;
        this.webappDir = webappDir;
    }

    @Override
    public void parseProject(DependencyGraph graph) throws IOException {
        if (!webappDir.exists() || !webappDir.isDirectory()) {
            System.out.println("Webapp directory does not exist or is not a directory: " + webappDir.getAbsolutePath());
            return;
        }

        Files.walk(webappDir.toPath())
                .filter(p -> p.toString().endsWith(".xhtml"))
                .forEach(path -> {
                    try {
                        parseXhtmlFile(path.toFile());
                    } catch (IOException e) {
                        System.out.println("Error parsing XHTML file: " + path + " Error: " + e.getMessage());
                    }
                });
    }

    /**
     * Parses a single XHTML file and updates the dependency graph.
     *
     * @param xhtmlFile The XHTML file to parse
     * @throws IOException If an I/O error occurs
     */
    private void parseXhtmlFile(File xhtmlFile) throws IOException {
        String content = Files.readString(xhtmlFile.toPath());

        // First, find page-local variables to avoid false positives
        findPageVars(content);

        // Find bean references like #{beanName}
        findBeanReferences(content, xhtmlFile.getName());

        // Find method references like #{beanName.methodName}
        findMethodUsages(content, xhtmlFile.getName());
    }

    /**
     * Finds page-local variables defined in XHTML (e.g., dataTable's var="_member").
     */
    private void findPageVars(String content) {
        pageVars.clear();  // Reset for each file
        Pattern varPattern = Pattern.compile("var\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = varPattern.matcher(content);
        while (matcher.find()) {
            String varName = matcher.group(1);
            pageVars.add(varName);
        }
    }

    /**
     * Finds and processes bean references in EL expressions.
     */
    private void findBeanReferences(String content, String fileName) {
        Pattern elPattern = Pattern.compile("#\\{([a-zA-Z0-9_]+)");
        Matcher matcher = elPattern.matcher(content);

        while (matcher.find()) {
            String beanName = matcher.group(1);

            if (RESERVED_WORDS.contains(beanName) || pageVars.contains(beanName)) {
                // Skip reserved words and page variables
                continue;
            }

            System.out.println("EL binding found in " + fileName + ": " + beanName);
            matchBeanToClass(beanName);
        }
    }

    /**
     * Matches a bean name to its corresponding Java class.
     */
    private void matchBeanToClass(String beanName) {
        boolean matched = false;
        for (String clazz : graph.getAllClasses()) {
            String simpleName = clazz.contains(".")
                    ? clazz.substring(clazz.lastIndexOf('.') + 1)
                    : clazz;
            String defaultBeanName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);

            if (beanName.equals(defaultBeanName)) {
                graph.markClassUsedByFramework(clazz);
                System.out.println("  -> Mapped to class: " + clazz);
                matched = true;
            }
        }
        if (!matched) {
            System.out.println("  -> ⚠ No matching Java class found for bean '" + beanName + "'");
        }
    }

    /**
     * Finds and processes method usages in EL expressions.
     */
    private void findMethodUsages(String content, String fileName) {
        Pattern elPattern = Pattern.compile("#\\{([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)\\}");
        Matcher matcher = elPattern.matcher(content);

        while (matcher.find()) {
            String beanName = matcher.group(1);
            String methodName = matcher.group(2);

            if (RESERVED_WORDS.contains(beanName) || pageVars.contains(beanName)) {
                continue;
            }

            for (String clazz : graph.getAllClasses()) {
                String simpleName = clazz.contains(".")
                        ? clazz.substring(clazz.lastIndexOf('.') + 1)
                        : clazz;
                String defaultBeanName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);

                if (beanName.equals(defaultBeanName)) {
                    // Mark method as used by framework (JSF/EL)
                    graph.markMethodUsedByFramework(clazz, methodName);
                    graph.markMethodUsage(clazz, methodName, MethodUsageType.FRAMEWORK);
                    System.out.println("Method usage found in " + fileName + ": " + clazz + "." + methodName);
                }
            }
        }
    }
} 