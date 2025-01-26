package parser;

import java.util.ArrayList;
import java.util.List;

public class ExecutableFinalMoveSelection implements ExecutableStatement {
    private String name;
    private List<ExecutableStatement> statements;

    public void setName(String name) {
        this.name = name;
    }

    public void addStatement(ExecutableStatement statement) {
        if (statements == null) {
            statements = new ArrayList<>();
        }
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
        return "ExecutableFinalMoveSelection{name='" + name + "', statements=" + statements + '}';
    }
}