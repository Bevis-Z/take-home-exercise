package com.bowen.analyzer.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for setting up and configuring symbol solvers for Java code analysis.
 */
public class SymbolResolverUtil {

    /**
     * Sets up the symbol solver for resolving Java types and methods.
     *
     * @param sourceRoots List of source root directories
     * @param projectRoot The root directory of the project
     */
    public static void setupSymbolSolver(List<File> sourceRoots, File projectRoot) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());

        // Add source roots
        for (File root : sourceRoots) {
            if (root.exists() && root.isDirectory()) {
                typeSolver.add(new JavaParserTypeSolver(root));
            } else {
                System.out.println("Source root does not exist or is not a directory: " + root.getAbsolutePath());
            }
        }

        // Automatically find the lib directory under the project root
        loadLibraryJars(typeSolver, projectRoot);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    }

    /**
     * Loads JAR files from the project's lib directory.
     *
     * @param typeSolver The type solver to add JAR resolvers to
     * @param projectRoot The root directory of the project
     */
    private static void loadLibraryJars(CombinedTypeSolver typeSolver, File projectRoot) {
        File libDir = new File(projectRoot, "target/lib");
        if (libDir.exists() && libDir.isDirectory()) {
            File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    try {
                        typeSolver.add(new JarTypeSolver(jar));
                        System.out.println("Loaded dependency: " + jar.getName());
                    } catch (IOException e) {
                        System.out.println("Failed to load JAR: " + jar.getName() + " Error: " + e.getMessage());
                    }
                }
            }
        } else {
            System.out.println("⚠ Dependency JAR folder not found: " + libDir.getAbsolutePath());
        }
    }
} 