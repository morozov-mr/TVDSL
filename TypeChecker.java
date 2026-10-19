package org.example;

import java.util.*;

@SuppressWarnings("unchecked")
public class TypeChecker {
    private final List<String> errors;
    private final Map<String, FunctionInfo> functions = new HashMap<>();
    private final Map<String, FormulaInfo> formulas = new HashMap<>();

    static class Symbol {
        String name, type;
        boolean isReadOnly, isInitialized;
        Symbol(String name, String type, boolean isReadOnly, boolean isInitialized) {
            this.name = name;
            this.type = type;
            this.isReadOnly = isReadOnly;
            this.isInitialized = isInitialized;
        }
    }

    static class Scope {
        Scope parent;
        Map<String, Symbol> symbols = new HashMap<>();
        Scope(Scope parent) {
            this.parent = parent;
        }
        boolean declare(String name, Symbol sym) {
            if (symbols.containsKey(name))
                return false;
            symbols.put(name, sym);
            return true;
        }
        Symbol resolve(String name) {
            if (symbols.containsKey(name))
                return symbols.get(name);
            if (parent != null)
                return parent.resolve(name);
            return null;
        }
    }

    static class FunctionInfo {
        String name;
        List<String> paramTypes = new ArrayList<>();
        List<String> returnTypes = new ArrayList<>();
    }

    static class FormulaInfo {
        String name;
        List<String> paramTypes = new ArrayList<>();
    }

    public TypeChecker(List<String> errors) {
        this.errors = errors;
    }

    public void check(Map<String, Object> ast) {
        List<Map<String, Object>> funcs = (List<Map<String, Object>>) ast.get("functions");
        List<Map<String, Object>> forms = (List<Map<String, Object>>) ast.get("formulas");

        if (forms != null) {
            for (Map<String, Object> f : forms)
                registerFormula(f);
        }
        if (funcs != null) {
            for (Map<String, Object> f : funcs)
                registerFunction(f);
        }
        if (funcs != null) {
            for (Map<String, Object> f : funcs)
                checkFunction(f);
        }
    }

    //signature check
    private void registerFormula(Map<String, Object> f) {
        String name = (String) f.get("name");
        if (formulas.containsKey(name) || functions.containsKey(name))
            error(f, "Дубликат имени '" + name + "'");
        else {
            FormulaInfo info = new FormulaInfo();
            info.name = name;
            List<Map<String, Object>> params = (List<Map<String, Object>>) f.get("params");
            if (params != null) {
                for (Map<String, Object> p : params)
                    info.paramTypes.add((String) p.get("varType"));
            }
            formulas.put(name, info);
        }
    }

    //signature check
    private void registerFunction(Map<String, Object> f) {
        String name = (String) f.get("name");
        if (functions.containsKey(name) || formulas.containsKey(name))
            error(f, "Дубликат имени '" + name + "'");
        else {
            FunctionInfo info = new FunctionInfo();
            info.name = name;
            List<Map<String, Object>> params = (List<Map<String, Object>>) f.get("params");
            if (params != null) {
                for (Map<String, Object> p : params)
                    info.paramTypes.add((String) p.get("varType"));
            }
            List<Map<String, Object>> returns = (List<Map<String, Object>>) f.get("returns");
            if (returns != null) {
                for (Map<String, Object> r : returns)
                    info.returnTypes.add((String) r.get("varType"));
            }
            functions.put(name, info);
        }
    }

    //inner function scope analysis
    private void checkFunction(Map<String, Object> f) {
        //params scope
        Scope paramScope = new Scope(null);
        List<Map<String, Object>> params = (List<Map<String, Object>>) f.get("params");
        if (params != null) {
            for (Map<String, Object> p : params) {
                String pName = (String) p.get("name");
                if (!paramScope.declare(pName, new Symbol(pName, (String) p.get("varType"), true, true)))
                    error(p, "Повторное объявление параметра '" + pName + "'");
            }
        }
        //requires
        if (f.get("requires") != null)
            checkPredicate((Map<String, Object>) f.get("requires"), paramScope);

        //params of returns
        Scope fullScope = new Scope(paramScope);
        List<Map<String, Object>> returns = (List<Map<String, Object>>) f.get("returns");
        if (returns != null) {
            for (Map<String, Object> r : returns) {
                String rName = (String) r.get("name");
                if (!fullScope.declare(rName, new Symbol(rName, (String) r.get("varType"), false, false)))
                    error(r, "Повторное объявление возвращаемой переменной '" + rName + "'");
            }
        }

        //uses
        List<Map<String, Object>> uses = (List<Map<String, Object>>) f.get("uses");
        if (uses != null) {
            for (Map<String, Object> u : uses) {
                String uName = (String) u.get("name");
                String uType = (String) u.get("varType");

                if (!fullScope.declare(uName, new Symbol(uName, uType, false, true)))
                    error(u, "Повторное объявление локальной переменной '" + uName + "'");
            }
        }

        //ensures
        if (f.get("ensures") != null)
            checkPredicate((Map<String, Object>) f.get("ensures"), fullScope);
        Map<String, Object> body = (Map<String, Object>) f.get("body");
        //body
        if (body != null)
            checkStmt(body, fullScope);
    }

    private void checkStmt(Map<String, Object> stmt, Scope scope) {
        if (stmt == null)
            return;
        String type = (String) stmt.get("type");
        switch (type) {
            case "Block":
                Scope blockScope = new Scope(scope);
                List<Map<String, Object>> stmts = (List<Map<String, Object>>) stmt.get("statements");
                if (stmts != null) {
                    for (Map<String, Object> s : stmts)
                        checkStmt(s, blockScope);
                }
                break;
            case "If":
                checkCondition((Map<String, Object>) stmt.get("condition"), scope);
                checkStmt((Map<String, Object>) stmt.get("then"), scope);
                if (stmt.get("else") != null)
                    checkStmt((Map<String, Object>) stmt.get("else"), scope);
                break;
            case "While":
                checkCondition((Map<String, Object>) stmt.get("condition"), scope);
                if (stmt.get("invariant") != null)
                    checkPredicate((Map<String, Object>) stmt.get("invariant"), scope);
                checkStmt((Map<String, Object>) stmt.get("body"), scope);
                break;
            case "Assert": case "Assume":
                checkPredicate((Map<String, Object>) stmt.get("predicate"), scope);
                break;
            case "Assignment":
                checkAssignment(stmt, scope);
                break;
        }
    }

    private void checkAssignment(Map<String, Object> stmt, Scope scope) {
        List<Map<String, Object>> targets = (List<Map<String, Object>>) stmt.get("targets");
        Map<String, Object> value = (Map<String, Object>) stmt.get("value");

        //simple assignment
        if (targets.size() == 1) {
            String valType = checkExpr(value, scope);
            checkAssignTarget(targets.get(0), valType, scope);
        } else { //tuple assignment
            if (!"FunctionCall".equals(value.get("type"))) {
                error(value, "Кортежное присваивание требует вызова функции справа");
                return;
            }
            String fName = (String) value.get("function");
            FunctionInfo fInfo = functions.get(fName);
            if (fInfo == null) {
                error(value, "Вызов неопределенной функции '" + fName + "'");
                return;
            }
            if (fInfo.returnTypes.size() != targets.size())
                error(value, "Несоответствие числа возвращаемых значений (" + fInfo.returnTypes.size() + ") и целей (" + targets.size() + ")");

            //target function check
            checkCallArgs((List<Map<String, Object>>) value.get("arguments"), fInfo.paramTypes, value, scope);
            int limit = Math.min(targets.size(), fInfo.returnTypes.size());
            for (int i = 0; i < limit; i++)
                checkAssignTarget(targets.get(i), fInfo.returnTypes.get(i), scope);
        }
    }

    private void checkAssignTarget(Map<String, Object> target, String valType, Scope scope) {
        String tType = (String) target.get("type");

        //simple var
        if ("Variable".equals(tType)) {
            String name = (String) target.get("name");
            Symbol sym = scope.resolve(name);
            if (sym == null) {
                if ("error".equals(valType))
                    error(target, "Невозможно вывести тип для '" + name + "'");
                else
                    scope.declare(name, new Symbol(name, valType, false, true));
                target.put("resolvedType", valType);
            } else {
                if (sym.isReadOnly)
                    error(target, "Запись в read-only параметр '" + name + "'");

                if (sym.type == null) {
                    sym.type = valType;
                    sym.isInitialized = true;
                } else {
                    if (!"error".equals(valType) && !sym.type.equals(valType))
                        error(target, "Несовпадение типов '" + name + "': " + sym.type + " = " + valType);
                    else
                        sym.isInitialized = true;
                }
                target.put("resolvedType", sym.type == null ? "error" : sym.type);
            }
        } else if ("ArrayAccess".equals(tType)) {
            //a[i] = v
            String arrType = checkExpr((Map<String, Object>) target.get("array"), scope);
            String idxType = checkExpr((Map<String, Object>) target.get("index"), scope);
            if (!"int[]".equals(arrType) && !"error".equals(arrType))
                error((Map<String, Object>) target.get("array"), "Ожидался массив int[]");
            if (!"int".equals(idxType) && !"error".equals(idxType))
                error((Map<String, Object>) target.get("index"), "Индекс должен быть int");
            if (!"int".equals(valType) && !"error".equals(valType))
                error(target, "Элемент массива требует int");
            target.put("resolvedType", "int");
        }
    }

    private String checkExpr(Map<String, Object> expr, Scope scope) {
        if (expr == null)
            return "error";
        String type = (String) expr.get("type");
        switch (type) {
            case "IntLiteral":
                expr.put("resolvedType", "int");
                return "int";
            case "BoolLiteral":
                expr.put("resolvedType", "bool");
                return "bool";
            case "Variable":
                String vName = (String) expr.get("name");
                Symbol sym = scope.resolve(vName);
                if (sym == null) {
                    error(expr, "Необъявленная переменная '" + vName + "'");
                    return "error";
                }
                if (!sym.isInitialized)
                    error(expr, "Неинициализированная переменная '" + vName + "'");
                expr.put("resolvedType", sym.type == null ? "error" : sym.type);
                return sym.type == null ? "error" : sym.type;
            case "BinaryOp":
                String lType = checkExpr((Map<String, Object>) expr.get("left"), scope);
                String rType = checkExpr((Map<String, Object>) expr.get("right"), scope);
                if (!"int".equals(lType) && !"error".equals(lType))
                    error((Map<String, Object>) expr.get("left"), "Ожидался int");
                if (!"int".equals(rType) && !"error".equals(rType))
                    error((Map<String, Object>) expr.get("right"), "Ожидался int");
                expr.put("resolvedType", "int");
                return "int";
            case "UnaryOp":
                String oType = checkExpr((Map<String, Object>) expr.get("operand"), scope);
                if (!"int".equals(oType) && !"error".equals(oType))
                    error((Map<String, Object>) expr.get("operand"), "Ожидался int");
                expr.put("resolvedType", "int");
                return "int";
            case "ArrayAccess":
                String aType = checkExpr((Map<String, Object>) expr.get("array"), scope);
                String iType = checkExpr((Map<String, Object>) expr.get("index"), scope);
                if (!"int[]".equals(aType) && !"error".equals(aType))
                    error((Map<String, Object>) expr.get("array"), "Ожидался массив int[]");
                if (!"int".equals(iType) && !"error".equals(iType))
                    error((Map<String, Object>) expr.get("index"), "Индекс должен быть int");
                expr.put("resolvedType", "int");
                return "int";
            case "Length":
                String lenType = checkExpr((Map<String, Object>) expr.get("expression"), scope);
                if (!"int[]".equals(lenType) && !"error".equals(lenType))
                    error((Map<String, Object>) expr.get("expression"), "length() требует int[]");
                expr.put("resolvedType", "int");
                return "int";
            case "FunctionCall":
                String fName = (String) expr.get("function");
                FunctionInfo fInfo = functions.get(fName);
                if (fInfo == null) {
                    error(expr, "Неопределенная функция '" + fName + "'");
                    return "error";
                }
                if (fInfo.returnTypes.size() != 1) {
                    error(expr, "Множественный возврат недопустим в выражениях");
                    return "error";
                }
                checkCallArgs((List<Map<String, Object>>) expr.get("arguments"), fInfo.paramTypes, expr, scope);
                expr.put("resolvedType", fInfo.returnTypes.get(0));
                return fInfo.returnTypes.get(0);
        }
        return "error";
    }

    private void checkCallArgs(List<Map<String, Object>> args, List<String> paramTypes, Map<String, Object> callNode, Scope scope) {
        if (args.size() != paramTypes.size()) {
            error(callNode, "Неверное число аргументов: " + paramTypes.size() + " != " + args.size());
            return;
        }
        for (int i = 0; i < args.size(); i++) {
            String aType = checkExpr(args.get(i), scope);
            if (!"error".equals(aType) && !paramTypes.get(i).equals(aType))
                error(args.get(i), "Тип аргумента: " + paramTypes.get(i) + " != " + aType);
        }
    }

    private void checkCondition(Map<String, Object> cond, Scope scope) {
        if (cond == null)
            return;
        String type = (String) cond.get("type");
        if ("BinaryOp".equals(type)) {
            String op = (String) cond.get("operator");
            if (op.equals("and") || op.equals("or") || op.equals("->")) {
                checkCondition((Map<String, Object>) cond.get("left"), scope);
                checkCondition((Map<String, Object>) cond.get("right"), scope);
            } else {
                String lType = checkExpr((Map<String, Object>) cond.get("left"), scope);
                String rType = checkExpr((Map<String, Object>) cond.get("right"), scope);
                if (!"int".equals(lType) && !"error".equals(lType))
                    error((Map<String, Object>) cond.get("left"), "Сравнение требует int");
                if (!"int".equals(rType) && !"error".equals(rType))
                    error((Map<String, Object>) cond.get("right"), "Сравнение требует int");
            }
        } else if ("UnaryOp".equals(type) && "not".equals(cond.get("operator")))
            checkCondition((Map<String, Object>) cond.get("operand"), scope);
        else if (!"BoolLiteral".equals(type)) error(cond, "Ожидалось логическое условие");
        cond.put("resolvedType", "bool");
    }

    private void checkPredicate(Map<String, Object> pred, Scope scope) {
        if (pred == null)
            return;
        String type = (String) pred.get("type");
        if ("Quantifier".equals(type)) {
            Scope qScope = new Scope(scope);
            Map<String, Object> param = (Map<String, Object>) pred.get("param");
            String pName = (String) param.get("name");
            qScope.declare(pName, new Symbol(pName, (String) param.get("varType"), true, true));
            checkPredicate((Map<String, Object>) pred.get("body"), qScope);
        } else if ("FunctionCall".equals(type)) {
            String fName = (String) pred.get("function");
            FormulaInfo fInfo = formulas.get(fName);
            if (fInfo == null)
                error(pred, "Неопределенная формула '" + fName + "'");
            else checkCallArgs((List<Map<String, Object>>)
                    pred.get("arguments"), fInfo.paramTypes, pred, scope);
        } else
            checkCondition(pred, scope);
        pred.put("resolvedType", "bool");
    }

    private void error(Map<String, Object> node, String message) {
        Integer line = (Integer) node.get("line");
        Integer col = (Integer) node.get("col");
        if (line != null && col != null)
            errors.add("Ошибка типизации на " + line + ":" + col + " - " + message);
        else
            errors.add("Ошибка типизации: " + message);
    }
}