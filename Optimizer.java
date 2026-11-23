package org.example;

import java.util.*;

public class Optimizer {
    public boolean constantPropagation = true;
    public boolean algebraicSimplify = true;
    public boolean deadCodeElimination = true;
    public boolean cse = true;

    public int constFolded = 0, simplified = 0, eliminated = 0, cseReplaced = 0;

    public IR.Program optimize(IR.Program p, CFG cfg) {
        if (constantPropagation)
            propagateConstants(p, cfg);
        for (CFG.Block b : cfg.blocks) {
            if (cse)
                localCSE(b);
            if (algebraicSimplify)
                simplifyBlock(b);
            if (deadCodeElimination)
                localDCE(b);
        }
        return p;
    }

    private void propagateConstants(IR.Program p, CFG cfg) {
        Map<String, Long> ints = new HashMap<>();
        Map<String, Boolean> bools = new HashMap<>();
        for (CFG.Block b : cfg.blocks) {
            for (IR.Instr in : b.instrs) {
                switch (in.op) {
                    case CONST:
                        if (in.bconst)
                            bools.put(in.dst, in.iconst != 0);
                        else
                            ints.put(in.dst, in.iconst);
                        break;
                    case COPY:
                        if (ints.containsKey(in.a))
                            ints.put(in.dst, ints.get(in.a));
                        else if (bools.containsKey(in.a))
                            bools.put(in.dst, bools.get(in.a));
                        break;
                    case ADD: case SUB: case MUL: case DIV:
                        if (ints.containsKey(in.a) && ints.containsKey(in.b)) {
                            long av = ints.get(in.a), bv = ints.get(in.b), r = 0;
                            switch (in.op) {
                                case ADD: r = av + bv; break;
                                case SUB: r = av - bv; break;
                                case MUL: r = av * bv; break;
                                case DIV: if (bv != 0) r = av / bv; else continue; break;
                            }
                            ints.put(in.dst, r);
                            in.op = IR.Op.CONST; in.iconst = r; in.bconst = false;
                            in.a = in.b = null; constFolded++;
                        }
                        break;
                    case NEG:
                        if (ints.containsKey(in.a)) {
                            long v = -ints.get(in.a);
                            ints.put(in.dst, v);
                            in.op = IR.Op.CONST;
                            in.iconst = v;
                            in.bconst = false;
                            in.a = null;
                            constFolded++;
                        }
                        break;
                    case NOT:
                        if (bools.containsKey(in.a)) {
                            boolean v = !bools.get(in.a);
                            bools.put(in.dst, v);
                            in.op = IR.Op.CONST;
                            in.iconst = v ? 1 : 0;
                            in.bconst = true;
                            in.a = null;
                            constFolded++;
                        }
                        break;
                    default:
                        if (in.dst != null) {
                            ints.remove(in.dst);
                            bools.remove(in.dst);
                        }
                }
            }
        }
    }

    private void simplifyBlock(CFG.Block b) {
        for (int i = 0; i < b.instrs.size(); i++) {
            IR.Instr in = b.instrs.get(i);

            if (in.op == IR.Op.ADD || in.op == IR.Op.SUB || in.op == IR.Op.MUL) {
                // арифметика
                if ((in.op == IR.Op.ADD || in.op == IR.Op.SUB) && isIntConst(b, in.b, 0)) {
                    replaceWithCopy(b, i, in.a);
                } else if (in.op == IR.Op.ADD && isIntConst(b, in.a, 0)) {
                    replaceWithCopy(b, i, in.b);
                } else if (in.op == IR.Op.MUL && isIntConst(b, in.b, 1)) {
                    replaceWithCopy(b, i, in.a);
                } else if (in.op == IR.Op.MUL && isIntConst(b, in.a, 1)) {
                    replaceWithCopy(b, i, in.b);
                } else if (in.op == IR.Op.MUL && (isIntConst(b, in.b, 0) || isIntConst(b, in.a, 0))) {
                    b.instrs.set(i, IR.Instr.constInt(in.dst, 0));
                    simplified++;
                } else if (in.op == IR.Op.SUB && in.a.equals(in.b)) {
                    b.instrs.set(i, IR.Instr.constInt(in.dst, 0));
                    simplified++;
                }
            } else if (in.op == IR.Op.AND || in.op == IR.Op.OR) {
                // булевы операции
                if (in.op == IR.Op.AND) {
                    if (isBoolConst(b, in.b, true)) {
                        replaceWithCopy(b, i, in.a);
                    } else if (isBoolConst(b, in.a, true)) {
                        replaceWithCopy(b, i, in.b);
                    } else if (isBoolConst(b, in.b, false) || isBoolConst(b, in.a, false)) {
                        b.instrs.set(i, IR.Instr.constBool(in.dst, false));
                        simplified++;
                    }
                } else { // OR
                    if (isBoolConst(b, in.b, false)) {
                        replaceWithCopy(b, i, in.a);
                    } else if (isBoolConst(b, in.a, false)) {
                        replaceWithCopy(b, i, in.b);
                    } else if (isBoolConst(b, in.b, true) || isBoolConst(b, in.a, true)) {
                        b.instrs.set(i, IR.Instr.constBool(in.dst, true));
                        simplified++;
                    }
                }
            }
        }
    }

    private boolean isIntConst(CFG.Block b, String name, long v) {
        if (name == null) return false;
        for (IR.Instr in : b.instrs) {
            if (in.op == IR.Op.CONST && !in.bconst && name.equals(in.dst))
                return in.iconst == v;
        }
        return false;
    }

    private boolean isBoolConst(CFG.Block b, String name, boolean v) {
        if (name == null) return false;
        for (IR.Instr in : b.instrs) {
            if (in.op == IR.Op.CONST && in.bconst && name.equals(in.dst))
                return (in.iconst != 0) == v;
        }
        return false;
    }

    private void replaceWithCopy(CFG.Block b, int i, String src) {
        IR.Instr in = b.instrs.get(i);
        IR.Instr c = new IR.Instr(IR.Op.COPY);
        c.dst = in.dst;
        c.a = src;
        b.instrs.set(i, c);
        simplified++;
    }


    private void localCSE(CFG.Block b) {
        Map<String, String> seen = new HashMap<>();
        for (int i = 0; i < b.instrs.size(); i++) {
            IR.Instr in = b.instrs.get(i);
            //invalid instructions
            if (in.op == IR.Op.CONST || in.op == IR.Op.COPY || in.op == IR.Op.LABEL ||
                    in.op == IR.Op.JUMP || in.op == IR.Op.BRANCH || in.op == IR.Op.RET ||
                    in.op == IR.Op.CALL || in.op == IR.Op.ARRAY_SET || in.op == IR.Op.ARRAY_GET ||
                    in.op == IR.Op.ASSERT || in.op == IR.Op.ASSUME) {

                if (in.op == IR.Op.CALL || in.op == IR.Op.ARRAY_SET || in.op == IR.Op.ARRAY_GET)
                    seen.clear();
                continue;
            }
            //doubling key
            String key = in.op + "|" + in.a + "|" + in.b;
            String existing = seen.get(key);
            if (existing != null) {
                IR.Instr cp = new IR.Instr(IR.Op.COPY); cp.dst = in.dst; cp.a = existing;
                b.instrs.set(i, cp); cseReplaced++;
            } else {
                seen.put(key, in.dst);
            }
        }
    }

    private void localDCE(CFG.Block b) {
        Set<String> used = new HashSet<>();
        // all usable instructions
        for (IR.Instr in : b.instrs) {
            if (in.a != null)
                used.add(in.a);
            if (in.b != null)
                used.add(in.b);
            if (in.op == IR.Op.RET && in.a != null)
                used.add(in.a);
        }
        // delete others
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = b.instrs.size() - 1; i >= 0; i--) {
                IR.Instr in = b.instrs.get(i);
                if (hasSideEffects(in))
                    continue;
                if (in.dst == null)
                    continue;
                if (in.dst != null && !used.contains(in.dst) && !in.dst.equals(currentRetVar(b))) {
                    b.instrs.remove(i);
                    eliminated++;
                    changed = true;
                }
            }
            used.clear();
            for (IR.Instr in : b.instrs) {
                if (in.a != null)
                    used.add(in.a);
                if (in.b != null)
                    used.add(in.b);
            }
        }
    }

    private String currentRetVar(CFG.Block b) { return null; }

    private boolean hasSideEffects(IR.Instr in) {
        switch (in.op) {
            case CALL: case RET: case ARRAY_SET: case ASSERT: case ASSUME:
            case JUMP: case BRANCH: case LABEL: case PARAM:
                return true;
            default: return false;
        }
    }
}