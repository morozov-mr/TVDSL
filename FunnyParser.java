package org.example;

import java.util.*;

public class FunnyParser {
    private List<Token> tokens;
    private int pos;
    public List<String> errors = new ArrayList<>();

    public FunnyParser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : new Token(TokenType.EOF, "", -1, -1);
    }

    private Token advance() {
        Token t = peek();
        if (pos < tokens.size())
            pos++;
        return t;
    }

    private boolean check(TokenType type) {
        return peek().type == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token expect(TokenType type, String message) {
        if (check(type))
            return advance();
        throw new ParseException(peek(), message);
    }

    private Map<String, Object> node(String type, Token t) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("type", type);
        if (t != null) {
            n.put("line", t.line);
            n.put("col", t.col);
        }
        return n;
    }

    private void synchronize() {
        while (!check(TokenType.EOF)) {
            if (check(TokenType.IDENT) && pos + 1 < tokens.size() && tokens.get(pos + 1).type == TokenType.LPAREN)
                return;
            advance();
        }
    }

    private void synchronizeStmt() {
        while (!check(TokenType.EOF)) {
            if (check(TokenType.SEMICOLON)) {
                advance();
                return;
            }
            if (check(TokenType.RBRACE))
                return;
            switch (peek().type) {
                case KW_IF: case KW_WHILE: case KW_ASSERT: case KW_ASSUME: case LBRACE: case IDENT: return;
            }
            advance();
        }
    }

    public Map<String, Object> parseProgram() {
        List<Map<String, Object>> functions = new ArrayList<>();
        List<Map<String, Object>> formulas = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            try {
                if (check(TokenType.IDENT)) {
                    Map<String, Object> decl = parseTopLevelDecl();
                    if (decl.get("type").equals("Function"))
                        functions.add(decl);
                    else
                        formulas.add(decl);
                } else {
                    throw new ParseException(peek(), "Ожидалось объявление функции или формулы");
                }
            } catch (ParseException e) {
                errors.add("Синтаксическая ошибка на " + e.token.line + ":" + e.token.col + " - " + e.message);
                synchronize();
            }
        }
        Map<String, Object> ast = new LinkedHashMap<>();
        ast.put("type", "Program");
        ast.put("functions", functions);
        ast.put("formulas", formulas);
        return ast;
    }

    private Map<String, Object> parseTopLevelDecl() {
        Token name = expect(TokenType.IDENT, "Ожидалось имя");
        expect(TokenType.LPAREN, "Ожидалось '('");
        List<Map<String, Object>> params = parseParamList();
        expect(TokenType.RPAREN, "Ожидалось ')'");

        if (match(TokenType.DEF_ARROW)) {
            Map<String, Object> body = parsePredicate();
            expect(TokenType.SEMICOLON, "Ожидалась ';' в конце формулы");
            Map<String, Object> n = node("Formula", name);
            n.put("name", name.lexeme);
            n.put("params", params);
            n.put("body", body);
            return n;
        } else {
            Map<String, Object> requires = null;
            if (match(TokenType.KW_REQUIRES))
                requires = parsePredicate();
            expect(TokenType.KW_RETURNS, "Ожидалось 'returns'");
            List<Map<String, Object>> returns = parseParamList();
            if (returns.isEmpty())
                throw new ParseException(peek(), "Функция должна возвращать хотя бы одно значение");
            Map<String, Object> ensures = null;
            if (match(TokenType.KW_ENSURES))
                ensures = parsePredicate();
            List<Map<String, Object>> uses = new ArrayList<>();
            if (match(TokenType.KW_USES))
                uses = parseLocalVarList();
            Map<String, Object> body = parseStmt();
            match(TokenType.SEMICOLON);

            Map<String, Object> n = node("Function", name);
            n.put("name", name.lexeme);
            n.put("params", params);
            if (requires != null)
                n.put("requires", requires);
            n.put("returns", returns);
            if (ensures != null)
                n.put("ensures", ensures);
            if (!uses.isEmpty())
                n.put("uses", uses);
            n.put("body", body);
            return n;
        }
    }

    private List<Map<String, Object>> parseParamList() {
        List<Map<String, Object>> list = new ArrayList<>();
        if (check(TokenType.RPAREN) || check(TokenType.DEF_ARROW) || check(TokenType.KW_RETURNS))
            return list;
        list.add(parseVariableDef());
        while (match(TokenType.COMMA))
            list.add(parseVariableDef());
        return list;
    }

    private Map<String, Object> parseVariableDef() {
        Token name = expect(TokenType.IDENT, "Ожидалось имя переменной");
        expect(TokenType.COLON, "Ожидалось ':'");
        String type = parseVariableType();
        Map<String, Object> n = node("Param", name);
        n.put("name", name.lexeme);
        n.put("varType", type);
        return n;
    }

    private String parseVariableType() {
        expect(TokenType.KW_INT, "Ожидался тип 'int'");
        if (match(TokenType.LBRACK)) {
            expect(TokenType.RBRACK, "Ожидалось ']'");
            return "int[]";
        }
        return "int";
    }

    private List<Map<String, Object>> parseLocalVarList() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(parseLocalVarDef());
        while (match(TokenType.COMMA))
            list.add(parseLocalVarDef());
        return list;
    }

    private Map<String, Object> parseLocalVarDef() {
        Token name = expect(TokenType.IDENT, "Ожидалось имя переменной");
        String type = null;
        if (match(TokenType.COLON))
            type = parseVariableType();
        Map<String, Object> n = node("LocalVar", name);
        n.put("name", name.lexeme);
        if (type != null)
            n.put("varType", type);
        return n;
    }

    private Map<String, Object> parseStmt() {
        if (check(TokenType.LBRACE)) return parseBlock();
        if (check(TokenType.KW_IF)) return parseIf();
        if (check(TokenType.KW_WHILE)) return parseWhile();
        if (check(TokenType.KW_ASSERT)) return parseAssert();
        if (check(TokenType.KW_ASSUME)) return parseAssume();
        if (check(TokenType.IDENT)) return parseAssignment();
        throw new ParseException(peek(), "Ожидался оператор (statement)");
    }

    private Map<String, Object> parseBlock() {
        Token start = expect(TokenType.LBRACE, "Ожидалось '{'");
        List<Map<String, Object>> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            try { stmts.add(parseStmt()); }
            catch (ParseException e) {
                errors.add("Синтаксическая ошибка на " + e.token.line + ":" + e.token.col + " - " + e.message);
                synchronizeStmt();
            }
        }
        expect(TokenType.RBRACE, "Ожидалось '}'");
        Map<String, Object> n = node("Block", start);
        n.put("statements", stmts);
        return n;
    }

    private Map<String, Object> parseIf() {
        Token start = expect(TokenType.KW_IF, "Ожидалось 'if'");
        expect(TokenType.LPAREN, "Ожидалось '('");
        Map<String, Object> cond = parseCondition();
        expect(TokenType.RPAREN, "Ожидалось ')'");
        Map<String, Object> thenB = parseStmt();
        Map<String, Object> elseB = null;
        if (match(TokenType.KW_ELSE))
            elseB = parseStmt();
        Map<String, Object> n = node("If", start);
        n.put("condition", cond);
        n.put("then", thenB);
        if (elseB != null)
            n.put("else", elseB);
        return n;
    }

    private Map<String, Object> parseWhile() {
        Token start = expect(TokenType.KW_WHILE, "Ожидалось 'while'");
        expect(TokenType.LPAREN, "Ожидалось '('");
        Map<String, Object> cond = parseCondition();
        expect(TokenType.RPAREN, "Ожидалось ')'");
        Map<String, Object> inv = null;
        if (match(TokenType.KW_INVARIANT))
            inv = parsePredicate();
        Map<String, Object> body = parseStmt();
        Map<String, Object> n = node("While", start);
        n.put("condition", cond);
        if (inv != null)
            n.put("invariant", inv);
        n.put("body", body);
        return n;
    }

    private Map<String, Object> parseAssert() {
        Token start = expect(TokenType.KW_ASSERT, "Ожидалось 'assert'");
        Map<String, Object> pred = parsePredicate();
        expect(TokenType.SEMICOLON, "Ожидалась ';'");
        Map<String, Object> n = node("Assert", start);
        n.put("predicate", pred);
        return n;
    }

    private Map<String, Object> parseAssume() {
        Token start = expect(TokenType.KW_ASSUME, "Ожидалось 'assume'");
        Map<String, Object> pred = parsePredicate();
        expect(TokenType.SEMICOLON, "Ожидалась ';'");
        Map<String, Object> n = node("Assume", start);
        n.put("predicate", pred);
        return n;
    }

    private Map<String, Object> parseAssignment() {
        Token start = peek();
        List<Map<String, Object>> targets = new ArrayList<>();
        targets.add(parseAssignTarget());
        while (match(TokenType.COMMA)) targets.add(parseAssignTarget());
        expect(TokenType.ASSIGN, "Ожидалось '='");

        Map<String, Object> val;
        if (targets.size() > 1) val = parseFunctionCallExpr();
        else val = parseExpr();
        expect(TokenType.SEMICOLON, "Ожидалась ';'");

        Map<String, Object> n = node("Assignment", start);
        n.put("targets", targets);
        n.put("value", val);
        return n;
    }

    private Map<String, Object> parseAssignTarget() {
        Token name = expect(TokenType.IDENT, "Ожидался идентификатор");
        Map<String, Object> target = node("Variable", name);
        target.put("name", name.lexeme);
        if (match(TokenType.LBRACK)) {
            Map<String, Object> index = parseExpr();
            expect(TokenType.RBRACK, "Ожидалось ']'");
            Map<String, Object> acc = node("ArrayAccess", name);
            acc.put("array", target);
            acc.put("index", index);
            return acc;
        }
        return target;
    }

    private Map<String, Object> parseExpr() { return parseAddExpr(); }
    private Map<String, Object> parseAddExpr() {
        Map<String, Object> left = parseMulExpr();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> right = parseMulExpr();
            Map<String, Object> n = node("BinaryOp", op);
            n.put("operator", op.lexeme);
            n.put("left", left);
            n.put("right", right);
            left = n;
        }
        return left;
    }
    private Map<String, Object> parseMulExpr() {
        Map<String, Object> left = parseUnaryExpr();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> right = parseUnaryExpr();
            Map<String, Object> n = node("BinaryOp", op);
            n.put("operator", op.lexeme);
            n.put("left", left);
            n.put("right", right);
            left = n;
        }
        return left;
    }
    private Map<String, Object> parseUnaryExpr() {
        if (match(TokenType.MINUS)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> operand = parseUnaryExpr();
            Map<String, Object> n = node("UnaryOp", op);
            n.put("operator", "-");
            n.put("operand", operand);
            return n;
        }
        return parsePrimaryExpr();
    }
    private Map<String, Object> parsePrimaryExpr() {
        if (match(TokenType.INT_LIT)) {
            Token t = tokens.get(pos - 1);
            Map<String, Object> n = node("IntLiteral", t);
            n.put("value", Integer.parseInt(t.lexeme));
            return n;
        }
        if (match(TokenType.KW_TRUE)) {
            Token t = tokens.get(pos - 1);
            Map<String, Object> n = node("BoolLiteral", t);
            n.put("value", true);
            return n;
        }
        if (match(TokenType.KW_FALSE)) {
            Token t = tokens.get(pos - 1);
            Map<String, Object> n = node("BoolLiteral", t);
            n.put("value", false);
            return n;
        }
        if (match(TokenType.KW_LENGTH)) {
            Token t = tokens.get(pos - 1);
            expect(TokenType.LPAREN, "Ожидалось '(' после length");
            Map<String, Object> expr = parseExpr();
            expect(TokenType.RPAREN, "Ожидалось ')'");
            Map<String, Object> n = node("Length", t);
            n.put("expression", expr);
            return n;
        }
        if (check(TokenType.IDENT)) {
            Token name = advance();
            if (match(TokenType.LPAREN)) {
                List<Map<String, Object>> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do
                        args.add(parseExpr());
                    while (match(TokenType.COMMA));
                }
                expect(TokenType.RPAREN, "Ожидалось ')'");
                Map<String, Object> n = node("FunctionCall", name);
                n.put("function", name.lexeme);
                n.put("arguments", args);
                return n;
            } else if (match(TokenType.LBRACK)) {
                Map<String, Object> index = parseExpr();
                expect(TokenType.RBRACK, "Ожидалось ']'");
                Map<String, Object> varNode = node("Variable", name);
                varNode.put("name", name.lexeme);
                Map<String, Object> n = node("ArrayAccess", name);
                n.put("array", varNode);
                n.put("index", index);
                return n;
            } else {
                Map<String, Object> n = node("Variable", name);
                n.put("name", name.lexeme);
                return n;
            }
        }
        if (match(TokenType.LPAREN)) {
            Token t = tokens.get(pos - 1);
            Map<String, Object> expr = parseExpr();
            expect(TokenType.RPAREN, "Ожидалось ')'");
            return expr;
        }
        throw new ParseException(peek(), "Ожидалось выражение");
    }

    private Map<String, Object> parseFunctionCallExpr() {
        Token name = expect(TokenType.IDENT, "Ожидалось имя функции");
        expect(TokenType.LPAREN, "Ожидалось '('");
        List<Map<String, Object>> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            args.add(parseExpr());
            while (match(TokenType.COMMA))
                args.add(parseExpr());
        }
        expect(TokenType.RPAREN, "Ожидалось ')'");
        Map<String, Object> n = node("FunctionCall", name);
        n.put("function", name.lexeme);
        n.put("arguments", args);
        return n;
    }

    private Map<String, Object> parseCondition() {
        return parseImpliesCond();
    }

    private Map<String, Object> parseImpliesCond() {
        Map<String, Object> left = parseOrCond();
        if (match(TokenType.IMPLIES)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> right = parseImpliesCond();
            Map<String, Object> n = node("BinaryOp", op);
            n.put("operator", "->");
            n.put("left", left); n.put("right", right);
            return n;
        }
        return left;
    }
    private Map<String, Object> parseOrCond() {
        Map<String, Object> left = parseAndCond();
        while (match(TokenType.KW_OR)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> right = parseAndCond();
            Map<String, Object> n = node("BinaryOp", op);
            n.put("operator", "or");
            n.put("left", left);
            n.put("right", right);
            left = n;
        }
        return left;
    }
    private Map<String, Object> parseAndCond() {
        Map<String, Object> left = parseNotCond();
        while (match(TokenType.KW_AND)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> right = parseNotCond();
            Map<String, Object> n = node("BinaryOp", op);
            n.put("operator", "and");
            n.put("left", left);
            n.put("right", right);
            left = n;
        }
        return left;
    }
    private Map<String, Object> parseNotCond() {
        if (match(TokenType.KW_NOT)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> operand = parseNotCond();
            Map<String, Object> n = node("UnaryOp", op);
            n.put("operator", "not");
            n.put("operand", operand);
            return n;
        }
        return parsePrimaryCond();
    }
    private Map<String, Object> parsePrimaryCond() {
        if (match(TokenType.KW_TRUE)) {
            Token t = tokens.get(pos - 1);
            Map<String, Object> n = node("BoolLiteral", t);
            n.put("value", true);
            return n;
        }
        if (match(TokenType.KW_FALSE)) {
            Token t = tokens.get(pos - 1);
            Map<String, Object> n = node("BoolLiteral", t);
            n.put("value", false);
            return n;
        }
        if (match(TokenType.LPAREN)) {
            Map<String, Object> cond = parseCondition();
            expect(TokenType.RPAREN, "Ожидалось ')'");
            return cond;
        }
        return parseComparison();
    }
    private Map<String, Object> parseComparison() {
        Map<String, Object> left = parseExpr();
        if (match(TokenType.EQ_EQ, TokenType.NOT_EQ, TokenType.LESS_EQ, TokenType.GREATER_EQ, TokenType.LESS, TokenType.GREATER)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> right = parseExpr();
            Map<String, Object> n = node("BinaryOp", op);
            n.put("operator", op.lexeme);
            n.put("left", left);
            n.put("right", right);
            return n;
        }
        throw new ParseException(peek(), "Ожидалось сравнение");
    }

    private Map<String, Object> parsePredicate() {
        return parseImpliesPred();
    }

    private Map<String, Object> parseImpliesPred() {
        Map<String, Object> left = parseOrPred();
        if (match(TokenType.IMPLIES)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> right = parseImpliesPred();
            Map<String, Object> n = node("BinaryOp", op);
            n.put("operator", "->");
            n.put("left", left);
            n.put("right", right); return n;
        }
        return left;
    }
    private Map<String, Object> parseOrPred() {
        Map<String, Object> left = parseAndPred();
        while (match(TokenType.KW_OR)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> right = parseAndPred();
            Map<String, Object> n = node("BinaryOp", op);
            n.put("operator", "or");
            n.put("left", left);
            n.put("right", right);
            left = n;
        }
        return left;
    }
    private Map<String, Object> parseAndPred() {
        Map<String, Object> left = parseNotPred();
        while (match(TokenType.KW_AND)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> right = parseNotPred();
            Map<String, Object> n = node("BinaryOp", op);
            n.put("operator", "and");
            n.put("left", left);
            n.put("right", right);
            left = n;
        }
        return left;
    }
    private Map<String, Object> parseNotPred() {
        if (match(TokenType.KW_NOT)) {
            Token op = tokens.get(pos - 1);
            Map<String, Object> operand = parseNotPred();
            Map<String, Object> n = node("UnaryOp", op);
            n.put("operator", "not");
            n.put("operand", operand);
            return n;
        }
        return parsePrimaryPred();
    }
    private Map<String, Object> parsePrimaryPred() {
        if (match(TokenType.KW_TRUE)) {
            Token t = tokens.get(pos - 1);
            Map<String, Object> n = node("BoolLiteral", t);
            n.put("value", true);
            return n;
        }
        if (match(TokenType.KW_FALSE)) {
            Token t = tokens.get(pos - 1);
            Map<String, Object> n = node("BoolLiteral", t);
            n.put("value", false);
            return n;
        }
        if (match(TokenType.LPAREN)) {
            Map<String, Object> pred = parsePredicate();
            expect(TokenType.RPAREN, "Ожидалось ')'");
            return pred;
        }
        if (check(TokenType.KW_FORALL) || check(TokenType.KW_EXISTS))
            return parseQuantifier();
        if (check(TokenType.IDENT)) {
            if (pos + 1 < tokens.size() && tokens.get(pos + 1).type == TokenType.LPAREN)
                return parseFormulaRef();
            else
                return parseComparison();
        }
        return parseComparison();
    }
    private Map<String, Object> parseQuantifier() {
        Token q = advance();
        expect(TokenType.LPAREN, "Ожидалось '('");
        Map<String, Object> param = parseVariableDef();
        expect(TokenType.PIPE, "Ожидалось '|'");
        Map<String, Object> body = parsePredicate();
        expect(TokenType.RPAREN, "Ожидалось ')'");
        Map<String, Object> n = node("Quantifier", q);
        n.put("quantifier", q.lexeme);
        n.put("param", param);
        n.put("body", body);
        return n;
    }
    private Map<String, Object> parseFormulaRef() {
        Token name = expect(TokenType.IDENT, "Ожидалось имя формулы");
        expect(TokenType.LPAREN, "Ожидалось '('");
        List<Map<String, Object>> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            args.add(parseExpr());
            while (match(TokenType.COMMA))
                args.add(parseExpr());
        }
        expect(TokenType.RPAREN, "Ожидалось ')'");
        Map<String, Object> n = node("FunctionCall", name);
        n.put("function", name.lexeme);
        n.put("arguments", args);
        return n;
    }

    private static class ParseException extends RuntimeException {
        Token token; String message;
        ParseException(Token token, String message) {
            this.token = token;
            this.message = message;
        }
    }

    public static String toJson(Object obj) {
        return toJson(obj, 0);
    }
    private static String toJson(Object obj, int indent) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escape((String)obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            if (map.isEmpty()) return "{}";
            StringBuilder sb = new StringBuilder("{\n");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",\n");
                appendIndent(sb, indent + 1);
                sb.append("\"").append(escape(entry.getKey().toString())).append("\": ").append(toJson(entry.getValue(), indent + 1));
                first = false;
            }
            sb.append("\n"); appendIndent(sb, indent); sb.append("}"); return sb.toString();
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (list.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[\n");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",\n");
                appendIndent(sb, indent + 1); sb.append(toJson(item, indent + 1)); first = false;
            }
            sb.append("\n"); appendIndent(sb, indent); sb.append("]"); return sb.toString();
        }
        return "\"" + escape(obj.toString()) + "\"";
    }
    private static void appendIndent(StringBuilder sb, int indent) { for (int i = 0; i < indent; i++) sb.append("  "); }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"); }
}