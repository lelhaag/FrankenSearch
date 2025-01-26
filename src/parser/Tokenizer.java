package parser;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private final String input;
    private int pos;
    private static final List<String> booleanKeywords = List.of("true", "false");

    public Tokenizer(String input) {
        this.input = input;
        this.pos = 0;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char current = input.charAt(pos);
            if (Character.isWhitespace(current)) {
                pos++;
            } else if (current == '-' && isNextCharDigit()) {
                tokens.add(readNumber());
            } else if (Character.isDigit(current) || current == '.') {
                tokens.add(readNumber());
            } else if (Character.isLetter(current)) {
                tokens.add(readSymbolOrBoolean());
            } else if (current == '"') {
                tokens.add(readName());
            } else {
                tokens.add(readSymbol());
            }
        }
        tokens.add(new Token(Token.TokenType.EOF, ""));
        return tokens;
    }

    private Token readSymbolOrBoolean() {
        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
            pos++;
        }
        String value = input.substring(start, pos);
        if (booleanKeywords.contains(value)) {
            return new Token(Token.TokenType.BOOLEAN, value);
        }
        return new Token(Token.TokenType.SYMBOL, value);
    }

    private Token readNumber() {
        int start = pos;

        // Check for a leading negative sign
        if (input.charAt(pos) == '-') {
            pos++;
        }

        boolean hasDecimalPoint = false;
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            if (input.charAt(pos) == '.') {
                if (hasDecimalPoint) {
                    break;
                } else {
                    hasDecimalPoint = true;
                }
            }
            pos++;
        }
        return new Token(Token.TokenType.NUMBER, input.substring(start, pos));
    }

    private Token readName() {
        pos++; // skip opening quote
        int start = pos;
        while (pos < input.length() && input.charAt(pos) != '"') {
            pos++;
        }
        String value = input.substring(start, pos);
        pos++; // skip closing quote
        return new Token(Token.TokenType.NAME, value);
    }

    private Token readSymbol() {
        char current = input.charAt(pos);
        pos++;
        return new Token(Token.TokenType.SYMBOL, String.valueOf(current));
    }

    private boolean isNextCharDigit() {
        // Check if the next character after the current position is a digit
        return pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1));
    }
}
