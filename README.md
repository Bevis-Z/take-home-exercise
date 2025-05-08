# Java Code Analyzer

A tool for analyzing Java codebases to extract dependency information, method calls, and generate visualizations for better understanding of code structure.

## Overview

This project provides a comprehensive solution for analyzing Java projects by:
1. Parsing Java source code to extract class and method information
2. Building dependency graphs between classes and methods
3. Identifying unused code and import statements
4. Generating JSON data for visualization in a web-based frontend
5. Providing impact analysis to determine which classes would be affected by changes

## Features

- **Class Dependency Analysis**: Detects and categorizes dependencies between classes (imports, inheritance, etc.)
- **Method Call Tracking**: Records method call hierarchies throughout the codebase
- **Unused Code Detection**: Identifies unused classes, methods and imports
- **JSON Export**: Generates structured data for visualization
- **Impact Analysis**: Determines which classes would be affected by changes to specific components

## Technologies Used

- **JavaParser**: For Java source code parsing and analysis
- **JGraphT**: For graph representation of dependencies
- **Jackson**: For JSON data processing
- **JUnit 5**: For testing
- **Mockito**: For mocking in tests
- **D3.js** (Frontend): For interactive visualizations

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven
- A Java project to analyze

### Installation

1. Clone the repository:
   ```
   git clone <repository-url>
   ```

2. Build the kitchensink project (sample project to analyze):
   Run the command under kitchensink
   ```
   mvn dependency:copy-dependencies -DoutputDirectory=target/lib
   ```


3. Build the analyzer project:
   Run the command under mongodb
   ```
   mvn clean package
   ```

4. Run the analysis
Run the command under mongodb

```
java -jar target/mongodb-1.0-SNAPSHOT.jar <path-to-java-project> <output-directory>
```

Example:
```
java -jar target/mongodb-1.0-SNAPSHOT.jar ../kitchensink frontend/public/data
```
5. Run the frontend for further visualisation
```
npm install
npm start dev
```
Parameters:
- `<path-to-java-project>`: Path to the root of the Java project to analyze (defaults to "kitchensink")
- `<output-directory>`: Directory where JSON output will be saved (defaults to "mongodb/frontend/public/data")

The analyzer will:
1. Analyze the source code in both `src/main/java` and `src/test/java` directories
2. Print an analysis report to the console
3. Export the data to a JSON file in the specified output directory

## Understanding the Output

The analyzer produces a JSON file named `code-data.json` with the following structure:
- `classes`: Information about all classes including their dependencies and imports
- `methods`: Method details and usage information
- `callGraph`: Representation of method calls with nodes and edges
- `unusedCode`: Lists of unused classes and methods
- `impactAnalysis`: Data showing potential impact of changes
