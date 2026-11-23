package org.example;

import java.util.*;

@SuppressWarnings("unchecked")
public class IRGenerator {
    private IR.Program prog;
    private final Set<String> localNames = new HashSet<>();

    public List<IR.Program> generate(Map<String, Object> ast) {
        List<IR.Program> out = new ArrayList<>();
        List<Map<String, Object>> funcs = (List<Map<String, Object>>) ast.get("functions");
        if (funcs != null)
            for (Map<String, Object> f : funcs)
                out.add(genFunction(f));
        return out;
    }

    private IR.Program genFunction(Map<String, Object> f) {
        prog = new IR.Program();
        prog.name = (String) f.get("name");
        List<Map<String, Object>> returns = (List<Map<String, Object>>) f.get("returns");
        String currentRet = returns != null && !returns.isEmpty() ? (String) returns.get(0).get("name") : null;
        List<Map<String, Object>> params = (List<Map<String, Object>>) f.get("params");
        if (params != null)
            for (Map<String, Object> p : params)
                localNames.add((String) p.get("name"));
        List<Map<String, Object>> uses = (List<Map<String, Object>>) f.get("uses");
        if (uses != null)
            for (Map<String, Object> u : uses)
                localNames.add((String) u.get("name"));
        if (returns != null)
            for (Map<String, Object> r : returns)
                localNames.add((String) r.get("name"));
        genStmt((Map<String, Object>) f.get("body"));
        if (currentRet != null)
            prog.code.add(IR.Instr.ret(currentRet));
        else
            prog.code.add(IR.Instr.ret(null));
        return prog;
    }

    private String genExpr(Map<String, Object> e) {
        if (e == null)
            return null;
        String t = (String) e.get("type");
        switch (t) {
            case "IntLiteral": {
                String d = prog.newTmp();
                prog.code.add(IR.Instr.constInt(d, ((Number) e.get("value")).longValue()));
                return d;
            }
            case "BoolLiteral": {
                String d = prog.newTmp();
                prog.code.add(IR.Instr.constBool(d, (Boolean) e.get("value")));
                return d;
            }
            case "Variable":
                return (String) e.get("name");
            case "UnaryOp": {
                String a = genExpr((Map<String, Object>) e.get("operand"));
                String op = (String) e.get("operator");
                String d = prog.newTmp();
                IR.Op irOp = op.equals("not") ? IR.Op.NOT : IR.Op.NEG;
                prog.code.add(IR.Instr.un(irOp, d, a));
                return d;
            }
            case "BinaryOp": {
                String a = genExpr((Map<String, Object>) e.get("left"));
                String b = genExpr((Map<String, Object>) e.get("right"));
                String op = (String) e.get("operator");
                IR.Op irOp = mapBin(op);
                String d = prog.newTmp();
                prog.code.add(IR.Instr.bin(irOp, d, a, b));
                return d;
            }
            case "ArrayAccess": {
                String arr = genExpr((Map<String, Object>) e.get("array"));
                String idx = genExpr((Map<String, Object>) e.get("index"));
                String d = prog.newTmp();
                IR.Instr i = new IR.Instr(IR.Op.ARRAY_GET);
                i.dst = d;
                i.a = arr;
                i.b = idx;
                prog.code.add(i);
                return d;
            }
            case "Length": {
                String a = genExpr((Map<String, Object>) e.get("expression"));
                String d = prog.newTmp();
                prog.code.add(IR.Instr.un(IR.Op.LENGTH, d, a));
                return d;
            }
            case "FunctionCall": {
                List<Map<String, Object>> args = (List<Map<String, Object>>) e.get("arguments");
                List<String> argVals = new ArrayList<>();
                if (args != null)
                    for (Map<String, Object> a : args)
                        argVals.add(genExpr(a));
                for (String av : argVals) {
                    IR.Instr p = new IR.Instr(IR.Op.PARAM);
                    p.a = av;
                    prog.code.add(p);
                }
                String d = prog.newTmp();
                prog.code.add(IR.Instr.call(d, (String) e.get("function"), argVals.size()));
                return d;
            }
        }
        return null;
    }

    private IR.Op mapBin(String op) {
        return switch (op) {
            case "-" -> IR.Op.SUB;
            case "*" -> IR.Op.MUL;
            case "/" -> IR.Op.DIV;
            case "==" -> IR.Op.EQ;
            case "!=" -> IR.Op.NE;
            case "<" -> IR.Op.LT;
            case "<=" -> IR.Op.LE;
            case ">" -> IR.Op.GT;
            case ">=" -> IR.Op.GE;
            case "and" -> IR.Op.AND;
            case "or" -> IR.Op.OR;
            case "->" -> IR.Op.IMPLIES;
            default -> IR.Op.ADD;
        };
    }

    private void genStmt(Map<String, Object> s) {
        if (s == null)
            return;
        String t = (String) s.get("type");
        switch (t) {
            case "Block": {
                List<Map<String, Object>> stmts = (List<Map<String, Object>>) s.get("statements");
                if (stmts != null)
                    for (Map<String, Object> st : stmts)
                        genStmt(st);
                break;
            }
            case "Assignment": {
                List<Map<String, Object>> targets = (List<Map<String, Object>>) s.get("targets");
                Map<String, Object> val = (Map<String, Object>) s.get("value");
                String v = genExpr(val);
                if (targets.size() == 1) {
                    genAssignTarget(targets.getFirst(), v);
                } else {
                    for (Map<String, Object> target : targets) {
                        String tmp = prog.newTmp();
                        IR.Instr cp = new IR.Instr(IR.Op.COPY);
                        cp.dst = tmp;
                        cp.a = v;
                        prog.code.add(cp);
                        genAssignTarget(target, tmp);
                    }
                }
                break;
            }
            case "If": {
                String cond = genExpr((Map<String, Object>) s.get("condition"));
                String elseL = prog.newLabel();
                String endL = prog.newLabel();
                prog.code.add(IR.Instr.branch(cond, elseL));
                genStmt((Map<String, Object>) s.get("then"));
                prog.code.add(IR.Instr.jump(endL));
                prog.code.add(IR.Instr.label(elseL));
                if (s.get("else") != null)
                    genStmt((Map<String, Object>) s.get("else"));
                prog.code.add(IR.Instr.label(endL));
                break;
            }
            case "While": {
                String startL = prog.newLabel();
                String endL = prog.newLabel();
                prog.code.add(IR.Instr.label(startL));
                String cond = genExpr((Map<String, Object>) s.get("condition"));
                prog.code.add(IR.Instr.branch(cond, endL));
                genStmt((Map<String, Object>) s.get("body"));
                prog.code.add(IR.Instr.jump(startL));
                prog.code.add(IR.Instr.label(endL));
                break;
            }
            case "Assert": case "Assume": {
                String a = genExpr((Map<String, Object>) s.get("predicate"));
                IR.Instr i = new IR.Instr(t.equals("Assert") ? IR.Op.ASSERT : IR.Op.ASSUME);
                i.a = a;
                prog.code.add(i);
                break;
            }
        }
    }

    private void genAssignTarget(Map<String, Object> target, String v) {
        String tt = (String) target.get("type");
        if ("Variable".equals(tt)) {
            IR.Instr i = new IR.Instr(IR.Op.COPY);
            i.dst = (String) target.get("name");
            i.a = v;
            prog.code.add(i);
        } else if ("ArrayAccess".equals(tt)) {
            String arr = (String) ((Map<String, Object>) target.get("array")).get("name");
            String idx = genExpr((Map<String, Object>) target.get("index"));
            IR.Instr i = new IR.Instr(IR.Op.ARRAY_SET);
            i.dst = arr;
            i.a = idx;
            i.b = v;
            prog.code.add(i);
        }
    }
}