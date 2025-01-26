package parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ASTNode {
    public enum NodeType {
        SYMBOL, NAME, NUMBER
    }

    private String value;
    private final NodeType type;
    private final List<ASTNode> children;
    private ASTNode parent;

    public ASTNode(String value, NodeType type) {
        this.value = value;
        this.type = type;
        this.children = new ArrayList<>();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public NodeType getType() {
        return type;
    }

    public void addChild(ASTNode child) {
        children.add(child);
        child.setParent(this);
    }

    public void setParent(ASTNode parent) {
        this.parent = parent;
    }

    public ASTNode getParent() {
        return parent;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    // Clone method for deep copying the ASTNode and its subtree
    @Override
    public ASTNode clone() {
        ASTNode clonedNode = new ASTNode(this.value, this.type);
        for (ASTNode child : this.children) {
            clonedNode.addChild(child.clone()); // Recursively clone each child
        }
        return clonedNode;
    }

    @Override
    public String toString() {
        return "ASTNode{" + "value='" + value + '\'' + ", type=" + type + '}';
    }

    public void printTree(String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + value + " (" + type + ")");
        for (int i = 0; i < children.size() - 1; i++) {
            children.get(i).printTree(prefix + (isTail ? "    " : "│   "), false);
        }
        if (!children.isEmpty()) {
            children.get(children.size() - 1).printTree(prefix + (isTail ? "    " : "│   "), true);
        }
    }

    public String getTreeString(String prefix, boolean isTail) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix).append(isTail ? "└── " : "├── ").append(value).append(" (").append(type).append(")\n");
        for (int i = 0; i < children.size() - 1; i++) {
            builder.append(children.get(i).getTreeString(prefix + (isTail ? "    " : "│   "), false));
        }
        if (!children.isEmpty()) {
            builder.append(children.get(children.size() - 1).getTreeString(prefix + (isTail ? "    " : "│   "), true));
        }
        return builder.toString();
    }

    public String toSADLString() {
        StringBuilder builder = new StringBuilder();
        toSADLString(builder, 0);
        return builder.toString().trim();  // Remove any leading/trailing spaces or newlines
    }

    private void toSADLString(StringBuilder builder, int indentLevel) {
        String indentStr = "  ".repeat(indentLevel);

        if (this.children.isEmpty()) {
            // Leaf node
            if (indentLevel > 0) {
                builder.append(indentStr);
            }
            if (this.type == NodeType.NAME) {
                builder.append("\"").append(this.value).append("\"");
            } else {
                builder.append(this.value);
            }
        } else {
            // Non-leaf node
            boolean isBlockNode = isBlockNode();

            if (indentLevel > 0 || builder.isEmpty()) {
                builder.append(indentStr);
            }
            builder.append("(").append(this.value);

            if (isBlockNode) {
                // Print children on new lines with increased indentation
                for (ASTNode child : this.children) {
                    builder.append("\n");
                    child.toSADLString(builder, indentLevel + 1);
                }
                builder.append("\n").append(indentStr).append(")");
            } else {
                // Print children on the same line
                for (ASTNode child : this.children) {
                    builder.append(" ");
                    child.toSADLString(builder, 0); // No additional indentation
                }
                builder.append(")");
            }
        }
    }

    private boolean isBlockNode() {
        Set<String> blockNodes = Set.of(
                "SearchAlgorithm", "Selection", "Evaluation", "Backpropagation", "FinalMoveSelection", "Condition"
        );
        return blockNodes.contains(this.value);
    }


}
