package parser;

import java.util.Map;

import utils.GlobalVariableRegistry;

public class ExecutableSet implements ExecutableStatement {
    private String variable;
    private ASTNode expression;

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public void setExpression(ASTNode expression) {
        this.expression = expression;
    }

    @Override
    public <T extends Node<T>> T execute(T node) {
        double value = ExpressionEvaluator.evaluateExpression(expression, node);

        Map<String, Double> variables = GlobalVariableRegistry.getInstanceVariables(node.getSearchId());

        if (variables != null && variables.containsKey(variable)) {
            variables.put(variable, value);
        } else {
            node.setValue(variable, value);
        }


        return node;
    }

    @Override
    public String toString() {
        return "ExecutableSet{variable='" + variable + "', expression=" + expression + '}';
    }
}
