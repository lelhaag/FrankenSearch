package parser;

public interface ExecutableStatement {
    <T extends Node<T>> T execute(T node);
}