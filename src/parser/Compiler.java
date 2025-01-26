package parser;

public class Compiler {

    public static ExecutableSearchAlgorithm compile(ASTNode node) {
        if (!node.getValue().equals("SearchAlgorithm")) {
            throw new RuntimeException("Expected SearchAlgorithm node, found: " + node.getValue());
        }

        ExecutableSearchAlgorithm algorithm = new ExecutableSearchAlgorithm();
        algorithm.setName(node.getChildren().get(0).getValue());

        for (ASTNode child : node.getChildren()) {
            if (child.getType() != ASTNode.NodeType.NAME) {
                switch (child.getValue()) {
                    case "Selection":
                        algorithm.setSelection(compileSelection(child));
                        break;
                    case "Evaluation":
                        algorithm.setEvaluation(compileEvaluation(child));
                        break;
                    case "Backpropagation":
                        algorithm.setBackpropagation(compileBackpropagation(child));
                        break;
                    case "FinalMoveSelection":
                        algorithm.setFinalMoveSelection(compileFinalMoveSelection(child));
                        break;
                    case "Define":
                        algorithm.addGlobalVariable(child.getChildren().get(0).getValue(), Double.parseDouble(child.getChildren().get(1).getValue()));
                        break;
                    default:
                        throw new RuntimeException("Unexpected node: " + child.getValue());
                }
            }
        }

        return algorithm;
    }

    private static ExecutableSelection compileSelection(ASTNode node) {
        ExecutableSelection selection = new ExecutableSelection();
        selection.setName(node.getChildren().get(0).getValue());

        for (ASTNode child : node.getChildren()) {
            if (child.getType() != ASTNode.NodeType.NAME) {
                selection.addStatement(compileStatement(child));
            }
        }

        return selection;
    }

    private static ExecutableEvaluation compileEvaluation(ASTNode node) {
        ExecutableEvaluation evaluation = new ExecutableEvaluation();

        for (ASTNode child : node.getChildren()) {
            evaluation.addStatement(compileStatement(child));
        }

        return evaluation;
    }

    private static ExecutableBackpropagation compileBackpropagation(ASTNode node) {
        ExecutableBackpropagation backpropagation = new ExecutableBackpropagation();

        for (ASTNode child : node.getChildren()) {
            backpropagation.addStatement(compileStatement(child));
        }

        return backpropagation;
    }

    private static ExecutableFinalMoveSelection compileFinalMoveSelection(ASTNode node) {
        ExecutableFinalMoveSelection finalMoveSelection = new ExecutableFinalMoveSelection();

        for (ASTNode child : node.getChildren()) {
            if (child.getType() != ASTNode.NodeType.NAME) {
                finalMoveSelection.addStatement(compileStatement(child));
            }
        }
        return finalMoveSelection;
    }

    private static ExecutableStatement compileStatement(ASTNode node) {
        return switch (node.getValue()) {
            case "Condition" -> compileCondition(node);
            case "Set" -> compileSet(node);
            case "SelectNode" -> compileSelectNode(node);
            default -> throw new RuntimeException("Unexpected node: " + node.getValue());
        };
    }

    private static ExecutableCondition compileCondition(ASTNode node) {
        ExecutableCondition condition = new ExecutableCondition();
        condition.setCondition(node.getChildren().get(0)); // Set the entire condition node

        for (ASTNode child : node.getChildren()) {
            if (!child.equals(node.getChildren().get(0))) {
                condition.addStatement(compileStatement(child));
            }
        }

        return condition;
    }

    private static ExecutableSet compileSet(ASTNode node) {
        ExecutableSet set = new ExecutableSet();
        set.setVariable(node.getChildren().get(0).getValue());
        set.setExpression(node.getChildren().get(1));
        return set;
    }

    private static ExecutableSelectNode compileSelectNode(ASTNode node) {
        ExecutableSelectNode selectNode = new ExecutableSelectNode();
        selectNode.setFunction(node.getChildren().get(0).getValue());
        selectNode.setExpression(node.getChildren().get(1));
        return selectNode;
    }
}
