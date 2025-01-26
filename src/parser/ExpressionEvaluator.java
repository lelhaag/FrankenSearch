package parser;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import utils.FunctionRegistry;
import utils.GlobalVariableRegistry;

public class ExpressionEvaluator {

    public static <T extends Node<T>> double evaluateExpression(ASTNode expression, T node) {
        switch (expression.getValue()) {
            case "+":
                return evaluateExpression(expression.getChildren().get(0), node) +
                        evaluateExpression(expression.getChildren().get(1), node);
            case "-":
                return evaluateExpression(expression.getChildren().get(0), node) -
                        evaluateExpression(expression.getChildren().get(1), node);
            case "*":
                return evaluateExpression(expression.getChildren().get(0), node) *
                        evaluateExpression(expression.getChildren().get(1), node);
            case "/":
                double denominator = evaluateExpression(expression.getChildren().get(1), node);
                if (denominator == 0) {
                    denominator = 1;
                }
                return evaluateExpression(expression.getChildren().get(0), node) / denominator;
            case "sqrt":
                return Math.sqrt(evaluateExpression(expression.getChildren().get(0), node));
            case "log":
                return Math.log(evaluateExpression(expression.getChildren().get(0), node));
            case "orNode":
                return 0.0; // Value representing orNode/maxNode
            case "maxNode":
                return 0.0; // Value representing orNode/maxNode
            case "andNode":
                return 1.0; // Value representing andNode/minNode
            case "minNode":
                return 1.0; // Value representing andNode/minNode
            case "true":
                return 1.0; // true
            case "false":
                return 0.0; // false
            case "unknown":
                return -1.0; // unknown
            case "numChildren":
                return node.getChildren().size();
            case "Aggregate":
                return evaluateAggregate(expression, node);
            case "inf":
                return Double.POSITIVE_INFINITY;
            case "depth":
                return node.getDepth();
            case "Parent":
                T parentNode = node.getParent();
                if (parentNode == null) {
                    throw new RuntimeException("Node has no parent when evaluating: " + expression.getValue());
                }
                // Get the parameter name from the first child of Parent
                return evaluateExpression(expression.getChildren().get(0), parentNode);

            case "ExternalFunction":
                String functionName = expression.getChildren().get(0).getValue();

                if (!FunctionRegistry.hasEvalFunction(functionName)) {
                    throw new RuntimeException("External function not found: " + functionName);
                }
                Function<T, Double> function = FunctionRegistry.getEvalFunction(functionName);
                return function.apply(node);

            default:
                Map<String, Double> variables = GlobalVariableRegistry.getInstanceVariables(node.getSearchId());
                if (variables != null && variables.containsKey(expression.getValue())) {
                    return variables.get(expression.getValue());
                } else if (node.hasValue(expression.getValue())) {
                    return node.getValue(expression.getValue());
                } else if (isNumeric(expression.getValue())) {
                    return Double.parseDouble(expression.getValue());
                } else {
                    throw new RuntimeException("Unexpected expression: " + expression.getValue());
                }
        }
    }

    private static <T extends Node<T>> double evaluateAggregate(ASTNode expression, T node) {
        String operation = expression.getChildren().get(0).getValue();
        String parameter = expression.getChildren().get(1).getValue();
        List<T> children = node.getChildren();

        if (children.isEmpty()) {
            return node.getValue(parameter);
        }

        return switch (operation) {
            case "min" -> children.stream()
                    .mapToDouble(child -> child.getValue(parameter))
                    .min()
                    .orElse(Double.POSITIVE_INFINITY);
            case "max" -> children.stream()
                    .mapToDouble(child -> child.getValue(parameter))
                    .max()
                    .orElse(Double.NEGATIVE_INFINITY);
            case "sum" -> children.stream()
                    .mapToDouble(child -> child.getValue(parameter))
                    .sum();
            case "avg" -> children.stream()
                    .mapToDouble(child -> child.getValue(parameter))
                    .average()
                    .orElse(0.0);
            default -> throw new RuntimeException("Unexpected aggregate operation: " + operation);
        };
    }

    public static <T extends Node<T>> boolean evaluateCondition(ASTNode condition, T node) {
        return switch (condition.getValue()) {
            case "eq" -> evaluateExpression(condition.getChildren().get(0), node) ==
                    evaluateExpression(condition.getChildren().get(1), node);
            case "neq" -> evaluateExpression(condition.getChildren().get(0), node) !=
                    evaluateExpression(condition.getChildren().get(1), node);
            case "lt" -> evaluateExpression(condition.getChildren().get(0), node) <
                    evaluateExpression(condition.getChildren().get(1), node);
            case "gt" -> evaluateExpression(condition.getChildren().get(0), node) >
                    evaluateExpression(condition.getChildren().get(1), node);
            case "lte" -> evaluateExpression(condition.getChildren().get(0), node) <=
                    evaluateExpression(condition.getChildren().get(1), node);
            case "gte" -> evaluateExpression(condition.getChildren().get(0), node) >=
                    evaluateExpression(condition.getChildren().get(1), node);
            case "and" -> evaluateCondition(condition.getChildren().get(0), node) &&
                    evaluateCondition(condition.getChildren().get(1), node);
            case "or" -> evaluateCondition(condition.getChildren().get(0), node) ||
                    evaluateCondition(condition.getChildren().get(1), node);
            default -> throw new RuntimeException("Unexpected condition: " + condition.getValue());
        };
    }


    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}