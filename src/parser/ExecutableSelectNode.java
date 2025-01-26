package parser;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class ExecutableSelectNode implements ExecutableStatement {
    private String function;
    private ASTNode expression;

    public void setFunction(String function) {
        this.function = function;
    }

    public void setExpression(ASTNode expression) {
        this.expression = expression;
    }

    public <T extends Node<T>> T select(T node) {
        List<T> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return node;
        }

        T bestChild = null;
        double bestValue = function.equals("argmax") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        int numBestFound = 0;

        for (T child : children) {
            if (Objects.equals(expression.getValue(), "Condition")) {
                ExecutableCondition condition = new ExecutableCondition();
                condition.setCondition(expression.getChildren().get(0));
                if (!condition.evaluate(child)) {
                    continue;
                } else {
                    expression = expression.getChildren().get(1);
                }
            }

            double value = ExpressionEvaluator.evaluateExpression(expression, child);
            if ((function.equals("argmax") && value > bestValue) ||
                    (function.equals("argmin") && value < bestValue)) {
                bestValue = value;
                bestChild = child;
                numBestFound = 1;
            } else if (value == bestValue) {
                // Random tie-breaking
                if (ThreadLocalRandom.current().nextInt() % ++numBestFound == 0) {
                    bestChild = child;
                }
            }
        }

        return bestChild;
    }

    @Override
    public <T extends Node<T>> T execute(T node) {
        return select(node);
    }

    @Override
    public String toString() {
        return "ExecutableSelectNode{function='" + function + "', expression=" + expression + '}';
    }
}