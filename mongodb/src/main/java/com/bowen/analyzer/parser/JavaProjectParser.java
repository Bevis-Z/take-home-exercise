package com.bowen.analyzer.parser;

import com.bowen.analyzer.model.DependencyGraph;
import com.bowen.analyzer.util.CodeAnalysisUtil;
import com.bowen.analyzer.util.SymbolResolverUtil;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Parser for Java source code files.
 */
public class JavaProjectParser implements Parser {
    private final List<File> sourceRoots;
    private final File projectRoot;

    /**
     * Creates a new Java project parser.
     *
     * @param sourceRoots All source roots to include in parsing
     * @param projectRoot The root directory of the project
     */
    public JavaProjectParser(List<File> sourceRoots, File projectRoot) {
        this.sourceRoots = sourceRoots;
        this.projectRoot = projectRoot;
        SymbolResolverUtil.setupSymbolSolver(sourceRoots, projectRoot);
    }

    private boolean isTestFile(File file) {
        String path = file.getAbsolutePath();
        return (path.contains("/test/") || path.contains("\\test\\"))
                && (file.getName().endsWith(".java"));
    }
    @Override
    public void parseProject(DependencyGraph graph) throws IOException {
        for (File root : sourceRoots) {
            if (!root.exists() || !root.isDirectory()) {
                System.out.println("Skipping non-existent or non-directory source root: " + root.getAbsolutePath());
                continue;
            }

            Files.walk(root.toPath())
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            File javaFile = path.toFile();
                            boolean isTestCode = isTestFile(javaFile);
                            parseJavaFile(javaFile, graph, isTestCode);
                        } catch (Exception e) {
                            System.out.println("Error parsing file: " + path + " Error: " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Parses a single Java file and updates the dependency graph.
     * Using Static Java Parser from github.javaparser;
     *
     * @param javaFile The Java file to parse
     * @param graph The dependency graph to update
     * @param isTestCode Whether the file is for test code
     * @throws IOException If an I/O error occurs
     */
    private void parseJavaFile(File javaFile, DependencyGraph graph, boolean isTestCode) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        // Find and process all class or interface declarations
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            String className = packageName.isEmpty() ? clazz.getNameAsString()
                    : packageName + "." + clazz.getNameAsString();
            graph.addClass(className);

            if (isTestCode) {
                graph.markClassUsedByTest(className);
            }

            // Process class elements and update the dependency graph
            CodeAnalysisUtil.processClassAnnotations(clazz, className, graph);
            CodeAnalysisUtil.processImports(cu, className, graph);
            CodeAnalysisUtil.processTypeReferences(cu, packageName, className, graph);
            CodeAnalysisUtil.processMethodCalls(cu, className, graph, isTestCode);
            CodeAnalysisUtil.processMethodDeclarations(clazz, className, graph);
        });
    }
} 