package parser;

import java.util.*;


public class Node<T extends Node<T>> {
    private final T parent;
    private final Map<String, Double> values;
    private List<T> children;
    private final int depth;
    private final String searchId;

    public Node(T parent) {
        this.values = new HashMap<>();
        this.children = new ArrayList<>();
        this.parent = parent;
        if (parent != null) {
            this.depth = parent.getDepth() + 1;
            this.searchId = parent.getSearchId();
        } else {
            this.depth = 0;
            this.searchId = UUID.randomUUID().toString();
        }

        values.put("visitCount", 0.0);
        values.put("valueEstimate", 0.0);
        values.put("proofNumber", 1.0);
        values.put("disproofNumber", 1.0);
    }

    public String getSearchId() {
        return searchId;
    }

    public T getParent() {
        return parent;
    }

    public void addChild(T child) {
        children.add(child);
    }

    public List<T> getChildren() {
        return children;
    }

    public void clearChildren() {
        children = null;
        children = new ArrayList<>();
    }

    public boolean hasValue(String key) {
        return values.containsKey(key);
    }

    public double getValue(String key) {
        return values.getOrDefault(key, 0.0);
    }

    public int getDepth() {
        return depth;
    }

    public void setValue(String key, double value) {
        values.put(key, value);
    }

    public void setNodeType(String nodeType) {
        if (nodeType.equals("orNode")) {
            values.put("nodeType", 0.0);
        } else if (nodeType.equals("andNode")) {
            values.put("nodeType", 1.0);
        }
    }

    public String getNodeType() {
        double type = values.get("nodeType");
        return type == 0.0 ? "orNode" : "andNode";
    }

    @Override
    public String toString() {
        return "Node{" + "values=" + values + ", children=" + children + '}';
    }
}
