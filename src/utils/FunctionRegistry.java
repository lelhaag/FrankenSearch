package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import parser.Node;

public class FunctionRegistry {
    private static final Map<String, Function<? extends Node<?>, Double>> evalFunctions = new HashMap<>();

    public static <T extends Node<T>> void setEvalFunction(String name, Function<T, Double> function) {
        evalFunctions.put(name, function);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Node<T>> Function<T, Double> getEvalFunction(String name) {
        return (Function<T, Double>) evalFunctions.get(name);
    }

    public static boolean hasEvalFunction(String name) {
        return evalFunctions.containsKey(name);
    }
}