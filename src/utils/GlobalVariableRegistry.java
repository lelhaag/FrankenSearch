package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalVariableRegistry {
    private static final Map<String, Map<String, Double>> instanceVariables = new ConcurrentHashMap<>();

    public static void createInstanceSpace(String instanceId, Map<String, Double> initialValues) {
        instanceVariables.put(instanceId, new ConcurrentHashMap<>(initialValues));
    }

    public static void setGlobalVariable(String instanceId, String name, Double value) {
        instanceVariables.get(instanceId).put(name, value);
    }

    public static Double getGlobalVariable(String instanceId, String name) {
        return instanceVariables.get(instanceId).get(name);
    }

    public static boolean hasGlobalVariable(String instanceId, String name) {
        return instanceVariables.containsKey(instanceId) &&
                instanceVariables.get(instanceId).containsKey(name);
    }

    public static void cleanupInstance(String instanceId) {
        instanceVariables.remove(instanceId);
    }

    public static boolean variableExistsInAnyInstance(String name) {
        return instanceVariables.values().stream()
                .anyMatch(variables -> variables.containsKey(name));
    }

    public static Map<String, Double> getInstanceVariables(String instanceId) {
        return instanceVariables.get(instanceId);
    }

}
