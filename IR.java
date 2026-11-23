package org.example;

import java.util.*;

public class IR {
    public enum Op {
        CONST, COPY, ADD, SUB, MUL, DIV, NEG, NOT,
        EQ, NE, LT, LE, GT, GE,
        AND, OR, IMPLIES,
        LABEL, JUMP, BRANCH,
        CALL, RET, PARAM, ARRAY_GET, ARRAY_SET, LENGTH,
        ASSERT, ASSUME
    }

    public static class Instr {
        public Op op;
        public String dst;      // result
        public String a, b;     // operands
        public long iconst;     // int const
        public boolean bconst;  // bool const
        public String label;    // label/jump/branch
        public String callName; // call
        public int argc;        // call/param

        public Instr(Op op) {
            this.op = op;
        }

        public static Instr constInt(String d, long v) {
            Instr i = new Instr(Op.CONST);
            i.dst = d;
            i.iconst = v;
            return i;
        }

        public static Instr constBool(String d, boolean v) {
            Instr i = new Instr(Op.CONST);
            i.dst = d;
            i.bconst = true;
            i.iconst = v ? 1 : 0;
            return i;
        }

        public static Instr bin(Op op, String d, String a, String b) {
            Instr i = new Instr(op);
            i.dst = d;
            i.a = a;
            i.b = b;
            return i;
        }

        public static Instr un(Op op, String d, String a) {
            Instr i = new Instr(op);
            i.dst = d;
            i.a = a;
            return i;
        }

        public static Instr label(String l) {
            Instr i = new Instr(Op.LABEL);
            i.label = l;
            return i;
        }

        public static Instr jump(String l) {
            Instr i = new Instr(Op.JUMP);
            i.label = l;
            return i;
        }

        //if cond goto Label
        public static Instr branch(String cond, String l) {
            Instr i = new Instr(Op.BRANCH);
            i.a = cond;
            i.label = l;
            return i;
        }

        public static Instr call(String d, String name, int argc) {
            Instr i = new Instr(Op.CALL);
            i.dst = d;
            i.callName = name;
            i.argc = argc;
            return i;
        }

        public static Instr ret(String a) {
            Instr i = new Instr(Op.RET);
            i.a = a;
            return i;
        }

        @Override public String toString() {
            switch (op) {
                case CONST: return dst + " = const " + (bconst ? (iconst != 0) : iconst);
                case COPY: return dst + " = " + a;
                case LABEL: return label + ":";
                case JUMP: return "goto " + label;
                case BRANCH: return "if " + a + " goto " + label;
                case CALL: return dst + " = call " + callName + "/" + argc;
                case RET: return "return " + (a == null ? "" : a);
                case ASSERT: return "assert " + a;
                case ASSUME: return "assume " + a;
                default:
                    if (b != null)
                        return dst + " = " + a + " " + op + " " + b;
                    return dst + " = " + op + " " + a;
            }
        }
    }

    public static class Program {
        public String name;
        public List<Instr> code = new ArrayList<>();
        public int tmpCounter = 0;
        public String newTmp() { return "t" + (tmpCounter++); }
        public String newLabel() { return "L" + (tmpCounter++); }
    }
}