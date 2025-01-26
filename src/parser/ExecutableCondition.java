package parser;

import java.util.ArrayList;
import java.util.List;

public class ExecutableCondition implements ExecutableStatement {
    private ASTNode condition;
    private List<ExecutableStatement> body;

    public void setCondition(ASTNode condition) {
        this.condition = condition;
    }

    public void addStatement(ExecutableStatement statement) {
        if (body == null) {
            body = new ArrayList<>();
        }
        body.add(statement);
    }

    public <T extends Node<T>> boolean evaluate(T node) {
        return ExpressionEvaluator.evaluateCondition(condition, node);
    }

    @Override
    public <T extends Node<T>> T execute(T node) {
        if (evaluate(node)) {
            for (ExecutableStatement statement : body) {
                node = statement.execute(node);
            }
        }
        return node;
    }

    @Override
    public String toString() {
        return "ExecutableCondition{condition='" + condition + "', body=" + body + '}';
    }
}