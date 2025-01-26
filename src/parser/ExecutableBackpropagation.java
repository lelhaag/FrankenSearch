package parser;

import java.util.ArrayList;
import java.util.List;

public class ExecutableBackpropagation implements ExecutableStatement {
    private String name;
    private final List<ExecutableStatement> statements;

    public ExecutableBackpropagation() {
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
        T currentNode = node;
        T prevNode = node;

        while (currentNode != null) {
            currentNode.setValue("visitCount", currentNode.getValue("visitCount") + 1);

            for (ExecutableStatement statement : statements) {
                currentNode = statement.execute(currentNode);
            }

            if (currentNode.hasValue("proofNumber") && currentNode.getDepth() > 1) {
                if (currentNode.getValue("proofNumber") == 0 || currentNode.getValue("disproofNumber") == 0) {
                    currentNode.clearChildren();

                }
            }

            prevNode = currentNode;
            currentNode = currentNode.getParent();
        }

        return prevNode;
    }

    @Override
    public String toString() {
        return "ExecutableBackpropagation{name='" + name + "', statements=" + statements + '}';
    }
}