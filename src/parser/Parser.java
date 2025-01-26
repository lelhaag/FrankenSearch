package parser;

import java.util.List;

import parser.ASTNode.NodeType;
import parser.Token.TokenType;

public class Parser {
    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    public ASTNode parse() {
        return parseSearchAlgorithm();
    }

    private ASTNode parseSearchAlgorithm() {
        expect(Token.TokenType.SYMBOL, "(");
        expect(Token.TokenType.SYMBOL, "SearchAlgorithm");
        ASTNode node = new ASTNode("SearchAlgorithm", ASTNode.NodeType.SYMBOL);
        node.addChild(parseName()); // Algorithm name
        while (!check(Token.TokenType.SYMBOL, ")")) {
            if (check(Token.TokenType.SYMBOL, "(")) {
                node.addChild(parseSearchAlgorithmComponent());
            } else {
                throw new RuntimeException("Unexpected token: " + tokens.get(pos));
            }
        }
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }


    private ASTNode parseDefine() {
        ASTNode node = new ASTNode("Define", ASTNode.NodeType.SYMBOL);
        node.addChild(parseSymbol()); // Variable name
        node.addChild(parseExpression()); // Variable value
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }

    private ASTNode parseSearchAlgorithmComponent() {
        expect(Token.TokenType.SYMBOL, "(");
        Token token = expect(Token.TokenType.SYMBOL);
        return switch (token.getValue()) {
            case "Selection" -> parseSelection();
            case "Evaluation" -> parseEvaluation();
            case "Backpropagation" -> parseBackpropagation();
            case "FinalMoveSelection" -> parseFinalMoveSelection();
            case "Define" -> parseDefine();
            default -> throw new RuntimeException("Unexpected category: " + token.getValue());
        };
    }

    private ASTNode parseSelection() {
        ASTNode node = new ASTNode("Selection", ASTNode.NodeType.SYMBOL);
        node.addChild(parseName()); // Selection name
        while (!check(Token.TokenType.SYMBOL, ")")) {
            node.addChild(parseConditionOrSelectNode());
        }
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }

    private ASTNode parseConditionOrSelectNode() {
        expect(Token.TokenType.SYMBOL, "(");
        if (check(Token.TokenType.SYMBOL, "Condition")) {
            return parseCondition();
        } else if (check(Token.TokenType.SYMBOL, "SelectNode")) {
            return parseSelectNode();
        } else {
            throw new RuntimeException("Unexpected token in Selection: " + tokens.get(pos));
        }
    }

    private ASTNode parseSelectNode() {
        expect(Token.TokenType.SYMBOL, "SelectNode");
        ASTNode node = new ASTNode("SelectNode", ASTNode.NodeType.SYMBOL);
        node.addChild(parseExpression()); // argmax or argmin
        node.addChild(parseExpression()); // UCB1 or proofNumber etc.
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }

    private ASTNode parseEvaluation() {
        ASTNode node = new ASTNode("Evaluation", ASTNode.NodeType.SYMBOL);
        while (!check(Token.TokenType.SYMBOL, ")")) {
            node.addChild(parseConditionOrSet());
        }
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }

    private ASTNode parseConditionOrSet() {
        expect(Token.TokenType.SYMBOL, "(");
        if (check(Token.TokenType.SYMBOL, "Condition")) {
            return parseCondition();
        } else if (check(Token.TokenType.SYMBOL, "Set")) {
            return parseSet();
        } else {
            throw new RuntimeException("Unexpected token in Evaluation: " + tokens.get(pos));
        }
    }

    private ASTNode parseSet() {
        expect(Token.TokenType.SYMBOL, "Set");
        ASTNode node = new ASTNode("Set", ASTNode.NodeType.SYMBOL);
        node.addChild(parseExpression());
        node.addChild(parseExpression());
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }

    private ASTNode parseBackpropagation() {
        ASTNode node = new ASTNode("Backpropagation", ASTNode.NodeType.SYMBOL);
        while (!check(Token.TokenType.SYMBOL, ")")) {
            node.addChild(parseConditionOrSet());
        }
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }

    private ASTNode parseFinalMoveSelection() {
        ASTNode node = new ASTNode("FinalMoveSelection", ASTNode.NodeType.SYMBOL);
        while (!check(Token.TokenType.SYMBOL, ")")) {
            node.addChild(parseExpression());
        }
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }

    private ASTNode parseCondition() {
        expect(Token.TokenType.SYMBOL, "Condition");
        ASTNode node = new ASTNode("Condition", ASTNode.NodeType.SYMBOL);
        node.addChild(parseExpression());
        while (!check(Token.TokenType.SYMBOL, ")")) {
            node.addChild(parseExpression());
        }
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }


    private ASTNode parseExpression() {
        if (check(Token.TokenType.SYMBOL, "(")) {
            return parseInnerNode();
        } else {
            return parseAtomic();
        }
    }

    private ASTNode parseAtomic() {
        if (check(Token.TokenType.SYMBOL)) {
            return parseSymbol();
        } else if (check(Token.TokenType.NAME)) {
            return parseName();
        } else if (check(Token.TokenType.NUMBER)) {
            return parseNumber();
        } else if (check(Token.TokenType.BOOLEAN)) {
            return parseBoolean();
        } else {
            throw new RuntimeException("Unexpected token: " + tokens.get(pos));
        }
    }


    private ASTNode parseInnerNode() {
        expect(Token.TokenType.SYMBOL, "(");
        Token token = expect(Token.TokenType.SYMBOL);
        ASTNode node = new ASTNode(token.getValue(), ASTNode.NodeType.SYMBOL);
        while (!check(Token.TokenType.SYMBOL, ")")) {
            node.addChild(parseExpression());
        }
        expect(Token.TokenType.SYMBOL, ")");
        return node;
    }

    private ASTNode parseSymbol() {
        Token token = expect(Token.TokenType.SYMBOL);
        return new ASTNode(token.getValue(), ASTNode.NodeType.SYMBOL);
    }

    private ASTNode parseName() {
        Token token = expect(Token.TokenType.NAME);
        return new ASTNode(token.getValue(), ASTNode.NodeType.NAME);
    }

    private ASTNode parseNumber() {
        Token token = expect(Token.TokenType.NUMBER);
        return new ASTNode(token.getValue(), ASTNode.NodeType.NUMBER);
    }

    private ASTNode parseBoolean() {
        Token token = expect(Token.TokenType.BOOLEAN);
        return new ASTNode(token.getValue(), ASTNode.NodeType.SYMBOL); // Using SYMBOL for boolean for simplicity
    }

    private Token expect(Token.TokenType type, String value) {
        Token token = tokens.get(pos);
        if (token.getType() != type || !token.getValue().equals(value)) {
            throw new RuntimeException("Expected " + value + ", found " + token.getValue());
        }
        pos++;
        return token;
    }

    private Token expect(Token.TokenType type) {
        Token token = tokens.get(pos);
        if (token.getType() != type) {
            throw new RuntimeException("Expected " + type + ", found " + token.getType());
        }
        pos++;
        return token;
    }

    private boolean check(Token.TokenType type, String value) {
        Token token = tokens.get(pos);
        return token.getType() == type && token.getValue().equals(value);
    }

    private boolean check(Token.TokenType type) {
        Token token = tokens.get(pos);
        return token.getType() == type;
    }
}
