package evolution;

import algos.*;
import parser.ASTNode;
import parser.Compiler;
import parser.ExecutableSearchAlgorithm;
import utils.FunctionRegistry;
import utils.GlobalVariableRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static evolution.EvolutionaryAlgorithm.*;
import static evolution.Utils.*;
import static evolution.Utils.proofValue;

public class Mutation {

    private static final Random random = new Random();

    public static Individual crossover(Individual parent1, Individual parent2) {
        int attempts = 0;
        boolean success = false;
        Individual offspring = null;

        while (attempts < MAX_CROSSOVER_ATTEMPTS && !success) {
            ASTNode root1 = parent1.astRoot.clone();
            ASTNode root2 = parent2.astRoot.clone();

            // Perform crossover
            success = performCrossover(root1, root2);

            if (success) {
                // Attempt to compile the new AST
                try {
                    ExecutableSearchAlgorithm algo = Compiler.compile(root1);
                    // Compilation successful
                    offspring = new Individual(root1);
                } catch (Exception e) {
                    // Compilation failed, try crossover again
                    success = false;
                }
            }

            if (!success) {
                attempts++;
            }

        }

        if (!success) {
            // If all attempts failed, you might decide to return one of the parents or handle differently
            System.out.println("Crossover failed after " + MAX_CROSSOVER_ATTEMPTS + " attempts.");
            offspring = new Individual(parent1.astRoot.clone());
        }

        return offspring;
    }

    static boolean performCrossover(ASTNode root1, ASTNode root2) {
        // Get all possible crossover points
        List<ASTNode> nodes1 = getAllNodes(root1);
        List<ASTNode> nodes2 = getAllNodes(root2);

        // Shuffle lists for random crossovers
        Collections.shuffle(nodes1, random);
        Collections.shuffle(nodes2, random);

        for (ASTNode crossoverNode1 : nodes1) {
            if (crossoverNode1.getParent() == null) {
                continue;
            }
            ASTNode crossoverNode2 = findCompatibleNodeForCrossover(nodes2, crossoverNode1);

            if (crossoverNode2 != null && areSubtreesDifferent(crossoverNode1, crossoverNode2)) {
                replaceNode(crossoverNode1, crossoverNode2);
                return true; // Successful crossover
            }
        }

        return false; // No compatible nodes found
    }

    static boolean areSubtreesDifferent(ASTNode node1, ASTNode node2) {
        if (node1 == null && node2 == null) {
            return false;
        }
        if (node1 == null || node2 == null) {
            return true;
        }
        if (!node1.equals(node2)) {
            return true;
        }

        List<ASTNode> children1 = node1.getChildren();
        List<ASTNode> children2 = node2.getChildren();

        if (children1.size() != children2.size()) {
            return true;
        }

        for (int i = 0; i < children1.size(); i++) {
            if (areSubtreesDifferent(children1.get(i), children2.get(i))) {
                return true;
            }
        }

        return false;
    }

    static ASTNode findCompatibleNodeForCrossover(List<ASTNode> nodes, ASTNode targetNode) {
        for (ASTNode node : nodes) {
            if (areNodesCompatibleForCrossover(node, targetNode)) {
                return node;
            }
        }
        return null;
    }

    static boolean areNodesCompatibleForCrossover(ASTNode node, ASTNode targetNode) {
        // Same name should always be compatible, includes the core components (Selection, Evaluation etc.)
        if (node.getValue().equals(targetNode.getValue())) {
            return true;
        }

        // General compatibility check for other nodes
        Operator op1 = getOperator(node.getValue());
        Operator op2 = getOperator(targetNode.getValue());

        if (op1 != null && op2 != null) {
            // Both are operators: they must have the same arity, and compatible types
            return op1.arity == op2.arity && typeMatches(op1.returnType, op2.returnType) && typesMatch(op1.inputTypes, op2.inputTypes);
        }

        if (numericalVariables.contains(node.getValue()) && numericalVariables.contains(targetNode.getValue())) {
            return true;
        }

        if (nodeTypes.contains(node.getValue()) && nodeTypes.contains(targetNode.getValue())) {
            return true;
        }

        if (proofValue.contains(node.getValue()) && proofValue.contains(targetNode.getValue())) {
            return true;
        }

        return isNumber(node.getValue()) && isNumber(targetNode.getValue()); // Both are numerical constants
    }


    public static void mutate(Individual individual) {
        int attempts = 0;
        boolean success = false;

        ASTNode mutatedAST;
        double mutationType;
        boolean successAST;
        boolean successNUM;

        while (attempts < MAX_MUTATION_ATTEMPTS && !success) {
            // Clone the individual's AST to avoid modifying the original in case of failure

            mutatedAST = individual.astRoot.clone();
            mutationType = random.nextDouble();
            successAST = false;
            successNUM = false;

            // Perform structural AST mutation, or change numerical values, or both

            if (mutationType < 0.66) {
                successAST = mutateAST(mutatedAST);
            }

            if (mutationType < 0.50) {
                successNUM = mutateNumericalValues(mutatedAST);
            }

            success =  successAST || successNUM;


            if (success) {
                // Attempt to compile the mutated AST
                try {
                    ExecutableSearchAlgorithm algo = Compiler.compile(mutatedAST);
                    // Compilation successful
                    individual.astRoot = mutatedAST;  // Update individual's AST
                } catch (Exception e) {
                    // Compilation failed, try mutating again
                    success = false;
                }
            }

            if (!success) {
                attempts++;
            }
        }

        if (!success) {
            // If all attempts failed, you might decide to keep the original AST or handle it differently
            System.out.println("Mutation failed after " + MAX_MUTATION_ATTEMPTS + " attempts.");
        }
    }

     static boolean mutateAST(ASTNode root) {
        List<ASTNode> allNodes = getAllNodes(root);
        String nodeValue;

        // Shuffle the nodes to ensure randomness
        Collections.shuffle(allNodes, random);

        for (ASTNode selectedNode : allNodes) {
            nodeValue = selectedNode.getValue();
            if (aggregates.contains(nodeValue)) {
                // Replace aggregate operation with another aggregate operation
                String[] aggregateArray = aggregates.toArray(new String[0]);
                selectedNode.setValue(aggregateArray[random.nextInt(aggregateArray.length)]);
                return true; // Successful mutation
            } else if (nodeTypes.contains(nodeValue)) {
                // Replace aggregate operation with another aggregate operation
                String[] nodeTypesArray = nodeTypes.toArray(new String[0]);
                selectedNode.setValue(nodeTypesArray[random.nextInt(nodeTypesArray.length)]);
                return true; // Successful mutation
            } else if (proofValue.contains(nodeValue)) {
                // Replace aggregate operation with another aggregate operation
                String[] proofValueArray = proofValue.toArray(new String[0]);
                selectedNode.setValue(proofValueArray[random.nextInt(proofValueArray.length)]);
                return true; // Successful mutation
            } else if (isOperator(nodeValue)) {
                // Replace the operator with another operator of the same arity and return type
                Operator currentOperator = getOperator(nodeValue);

                List<Operator> compatibleOperators = getCompatibleOperators(currentOperator);
                if (!compatibleOperators.isEmpty()) {
                    Operator newOperator = compatibleOperators.get(random.nextInt(compatibleOperators.size()));
                    selectedNode.setValue(newOperator.name);
                    return true; // Successful mutation
                }
            } else if (isNumber(nodeValue) || numericalVariables.contains(nodeValue)) {
                // Replace the variable with a more complex subtree of the same return type
                ASTNode newSubtree = generateRandomSubtreeForType("Number", selectedNode, 1);
                replaceNode(selectedNode, newSubtree);
                return true; // Successful mutation
            }
        }

        return false; // no Successful mutation
    }


    static ASTNode generateRandomSubtreeForType(String expectedType, ASTNode node, int depth) {

        if (random.nextDouble() < 0.5 / depth) {

            List<Operator> compatibleOperators = new ArrayList<>();
            for (Operator op : operators) {
                // Ensure input types are compatible
                if (typeMatches(op.returnType, expectedType)) {
                    compatibleOperators.add(op);
                }
            }

            Operator selectedOperator = compatibleOperators.get(random.nextInt(compatibleOperators.size()));
            ASTNode operatorNode = new ASTNode(selectedOperator.name, ASTNode.NodeType.SYMBOL);

            // Generate appropriate children for the operator
            for (String inputType : selectedOperator.inputTypes) {
                operatorNode.addChild(generateRandomSubtreeForType(inputType, node, depth + 1));
            }
            return operatorNode;

        } else if (depth > 1 && expectedType.equals("Number") && random.nextDouble() < 0.5) {
            return new ASTNode(node.getValue(), node.getType());
        }

        // Fallback to a variable or literal that matches the expected type
        if (expectedType.equals("Number")) {
            return new ASTNode(Double.toString(random.nextDouble() * 3), ASTNode.NodeType.NUMBER);
        } else if (expectedType.equals("Boolean")) {
            return new ASTNode(random.nextBoolean() ? "true" : "false", ASTNode.NodeType.SYMBOL);
        }
        throw new IllegalArgumentException("Unsupported operator type: " + expectedType);
    }


    static List<ASTNode> getAllNodes(ASTNode node) {
        List<ASTNode> nodes = new ArrayList<>();
        nodes.add(node);
        for (ASTNode child : node.getChildren()) {
            nodes.addAll(getAllNodes(child));
        }
        return nodes;
    }

    static void replaceNode(ASTNode oldNode, ASTNode newNode) {
        ASTNode parent = oldNode.getParent();
        int index = parent.getChildren().indexOf(oldNode);
        parent.getChildren().set(index, newNode);
        newNode.setParent(parent);
    }

    static List<Operator> getCompatibleOperators(Operator operator) {
        List<Operator> compatibleOperators = new ArrayList<>();
        for (Operator op : operators) {

            // don't return same operator
            if (op.name.equals(operator.name)) {
                continue;
            }
            // Check arity compatibility
            if (op.arity != operator.arity) {
                continue;
            }

            // Check return type compatibility
            if (!typeMatches(op.returnType, operator.returnType)) {
                continue;
            }

            // Ensure input types are compatible
            if (typesMatch(op.inputTypes, operator.inputTypes)) {
                compatibleOperators.add(op);
            }
        }
        return compatibleOperators;
    }

    static boolean typeMatches(String expected, String actual) {
        // If "Any" type is allowed, then it's considered a match for any type
        return expected.equals(actual) || expected.equals("Any");
    }

    static boolean typesMatch(List<String> expectedTypes, List<String> actualTypes) {
        if (expectedTypes.size() != actualTypes.size()) {
            return false; // Different number of arguments
        }

        for (int i = 0; i < expectedTypes.size(); i++) {
            if (!typeMatches(expectedTypes.get(i), actualTypes.get(i))) {
                return false; // Types do not match
            }
        }
        return true; // All types match
    }

    static boolean mutateNumericalValues(ASTNode node) {

        // Find and mutate numerical values in the AST
        List<ASTNode> numberNodes = getNumberNodes(node);

        if (!numberNodes.isEmpty()) {
            ASTNode selectedNode = numberNodes.get(random.nextInt(numberNodes.size()));
            double currentValue = Double.parseDouble(selectedNode.getValue());
            double newValue;
            if (currentValue == 0.0) {
                // Special handling for zero to ensure non-zero mutation
                newValue = random.nextGaussian() * 0.25; // Small random offset
            } else {
                // Apply random change (approx. +/- 50%)
                newValue = currentValue * (1.0 + (random.nextGaussian() * 0.25));
            }
            selectedNode.setValue(Double.toString(newValue));
        }
        return true;
    }

    static List<ASTNode> getNumberNodes(ASTNode node) {
        List<ASTNode> numberNodes = new ArrayList<>();
        if (isNumber(node.getValue())) {
            numberNodes.add(node);
        }
        for (ASTNode child : node.getChildren()) {
            numberNodes.addAll(getNumberNodes(child));
        }
        return numberNodes;
    }

    static boolean isOperator(String value) {
        return getOperator(value) != null;
    }

    static Operator getOperator(String name) {
        for (Operator op : operators) {
            if (op.name.equals(name)) {
                return op;
            }
        }
        return null;
    }


    String inferNodeType(ASTNode node) {
        String value = node.getValue();

        if (isNumber(value) || numericalVariables.contains(value) || value.equals("inf")) {
            return "Number";
        } else if (value.equals("true") || value.equals("false")) {
            return "Boolean";
        } else if (nodeTypes.contains(value)) {
            return "NodeType";
        } else if (isOperator(value)) {
            return getOperator(value).returnType;
        } else if (GlobalVariableRegistry.variableExistsInAnyInstance(value)) {
            // Assume global variables are numbers
            return "Number";
        } else if (FunctionRegistry.hasEvalFunction(value)) {
            // Assume evaluation functions return numbers
            return "Number";
        } else {
            return "Unknown";
        }
    }

    static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
