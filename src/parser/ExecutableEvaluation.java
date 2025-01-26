package parser;

import java.util.ArrayList;
import java.util.List;

public class ExecutableEvaluation implements ExecutableStatement {
    private String name;
    private final List<ExecutableStatement> statements;

    public ExecutableEvaluation() {
        this.statements = new ArrayList<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addStatement(ExecutableStatement statement) {
        statements.add(statement);
    }

    @Override
    public <T extends Node<T>> T execute(T node) {
        for (ExecutableStatement statement : statements) {
            node = statement.execute(node);
        }
        return node;
    }

    @Override
    public String toString() {
        return "ExecutableEvaluation{name='" + name + "', statements=" + statements + '}';
    }
}