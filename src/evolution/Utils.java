package evolution;

import parser.ASTNode;
import parser.Parser;
import parser.Tokenizer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// Utility class to hold all shared resources and functions
public class Utils {

    private static final Logger generationLogger = LogManager.getGenerationLogger();

    static int nextId = 1;  // Static variable to assign unique IDs

    // Class to represent a pair of objects
    static class Pair<F, S> {
        public F first;
        public S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }

    public static class Individual {
        int id;
        ASTNode astRoot;
        double score;  // Score for ranking individuals
        Set<Integer> opponentsPlayed;  // Keep track of opponents played

        public Individual(ASTNode astRoot) {
            this.astRoot = astRoot;
            this.id = nextId++;
            this.opponentsPlayed = new HashSet<>();
        }

        public Individual clone() {
            return new Individual(this.astRoot.clone());
        }
    }

    static class Operator {
        String name;
        int arity;
        List<String> inputTypes;  // List of expected input types
        String returnType;        // Return type of the operator

        public Operator(String name, int arity, List<String> inputTypes, String returnType) {
            this.name = name;
            this.arity = arity;
            this.inputTypes = inputTypes;
            this.returnType = returnType;
        }
    }



    // Operators list
    static List<Operator> operators = Arrays.asList(
            new Operator("+", 2, Arrays.asList("Number", "Number"), "Number"),
            new Operator("-", 2, Arrays.asList("Number", "Number"), "Number"),
            new Operator("*", 2, Arrays.asList("Number", "Number"), "Number"),
            new Operator("/", 2, Arrays.asList("Number", "Number"), "Number"),
            new Operator("sqrt", 1, List.of("Number"), "Number"),
            new Operator("log", 1, List.of("Number"), "Number"),
            new Operator("argmin", 1, List.of("Number"), "Node"),
            new Operator("argmax", 1, List.of("Number"), "Node"),
            new Operator("eq", 2, Arrays.asList("Any", "Any"), "Boolean"),
            new Operator("neq", 2, Arrays.asList("Any", "Any"), "Boolean"),
            new Operator("lt", 2, Arrays.asList("Number", "Number"), "Boolean"),
            new Operator("gt", 2, Arrays.asList("Number", "Number"), "Boolean"),
            new Operator("lte", 2, Arrays.asList("Number", "Number"), "Boolean"),
            new Operator("gte", 2, Arrays.asList("Number", "Number"), "Boolean"),
            new Operator("and", 2, Arrays.asList("Boolean", "Boolean"), "Boolean"),
            new Operator("or", 2, Arrays.asList("Boolean", "Boolean"), "Boolean")
    );

    static Set<String> aggregates = Set.of("min", "max", "avg", "sum");
    static Set<String> numericalVariables = Set.of("proofNumber", "disproofNumber", "value", "visitCount", "C", "T", "valueEstimate", "numChildren", "depth");
    static Set<String> nodeTypes = Set.of("maxNode", "minNode", "orNode", "andNode");
    static Set<String> proofValue = Set.of("true", "false", "unknown");

    // Utility function to log messages to a file
    public static void logToFile(String message) {
        try (FileWriter fw = new FileWriter("generation_log2.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(message);
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    // Helper function to create an AST from a file
    public static ASTNode createASTFromFile(String filePath) {
        try {
            String SADL_input = new String(Files.readAllBytes(Paths.get(filePath)));
            return new Parser(new Tokenizer(SADL_input).tokenize()).parse();
        } catch (IOException e) {
            System.err.println("Error reading SADL file: " + filePath);
        }
        return new ASTNode("SearchAlgorithm", ASTNode.NodeType.SYMBOL);
    }

    public static void logGenerationResults(int generation, List<Individual> population) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n=== Results for Generation ").append(generation).append(" ===\n");

        for (int i = population.size(); i > 0; i--) {
            Individual ind = population.get(i - 1);
            String individualName = ind.astRoot.getChildren().get(0).getValue();
            logBuilder.append("Individual ").append(ind.id)
                    .append(" (").append(individualName).append(")")
                    .append(" - Rank ").append(i)
                    .append(": Score = ").append(ind.score).append("\n");
            logBuilder.append(ind.astRoot.toSADLString()).append("\n");
        }
        logBuilder.append("=====================\n");

        generationLogger.info(logBuilder.toString());
    }

    public static Pair<Integer, List<Individual>> loadGenerationFromFile(String filePath) {
        List<Individual> population = new ArrayList<>();
        int generation = 0;
        int maxId = 0; // Track the maximum ID encountered

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();

            // Parse the generation header
            if (line != null && line.startsWith("=== Results for Generation")) {
                String[] parts = line.split(" ");
                generation = Integer.parseInt(parts[4].trim());
            }

            StringBuilder sadlBuilder = new StringBuilder();
            int id = 0;
            String name = "";
            double score = 0.0;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("Individual")) {
                    // Process the previous SADL block, if it exists
                    if (sadlBuilder.length() > 0) {
                        processSADLBlock(sadlBuilder.toString(), id, name, score, population);
                        sadlBuilder.setLength(0); // Clear the builder for the next individual
                    }

                    // Parse the individual header line
                    String[] parts = line.split(":");
                    String[] metadata = parts[0].split(" ");
                    id = Integer.parseInt(metadata[1].trim());
                    name = metadata[2].trim();
                    score = Double.parseDouble(parts[1].split("=")[1].trim());


                    // Update max Id for new individuals
                    nextId = Math.max(nextId, id + 1);
                } else if (line.startsWith("=====================")) {
                    // Finalize any remaining SADL block
                    if (sadlBuilder.length() > 0) {
                        processSADLBlock(sadlBuilder.toString(), id, name, score, population);
                        sadlBuilder.setLength(0);
                    }
                } else {
                    // Accumulate SADL lines
                    sadlBuilder.append(line).append("\n");
                }
            }

            // Process the last SADL block (if any)
            if (sadlBuilder.length() > 0) {
                processSADLBlock(sadlBuilder.toString(), id, name, score, population);
            }
        } catch (IOException e) {
            generationLogger.log(Level.SEVERE, "Error reading generation file", e);
        }

        return new Pair<>(generation + 1, population); // Start from the next generation
    }

    private static void processSADLBlock(String sadl, int id, String name, double score, List<Individual> population) {
        try {
            // Parse the SADL block into an AST
            ASTNode astRoot = new Parser(new Tokenizer(sadl).tokenize()).parse();
            Individual ind = new Individual(astRoot);
            ind.id = id;
            ind.score = score;

            // Optionally set the name or log it for debugging
            generationLogger.info("Processed Individual " + id + " (" + name + ") with Score: " + score);

            population.add(ind);
        } catch (Exception e) {
            generationLogger.log(Level.WARNING, "Error parsing SADL block for individual " + id, e);
        }
    }



    public static void logInitialPopulation(List<Individual> population) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n=== Initial Population ===\n");

        for (Individual ind : population) {
            logBuilder.append("Individual ").append(ind.id).append(":\n");
            logBuilder.append(ind.astRoot.toSADLString()).append("\n");
        }
        logBuilder.append("=====================\n");

        generationLogger.info(logBuilder.toString());
    }

}