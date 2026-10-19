package org.example;

public class Token {
    public TokenType type;
    public String lexeme;
    public int line;
    public int col;

    public Token(TokenType type, String lexeme, int line, int col) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        return type + "('" + lexeme + "') at " + line + ":" + col;
    }
}