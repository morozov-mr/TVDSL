package org.example;

import java.util.*;

public class CFG {
    public static class Block {
        public String label;
        public List<IR.Instr> instrs = new ArrayList<>();
        public List<Block> succ = new ArrayList<>(); //исходящие рёбра
        public List<Block> pred = new ArrayList<>(); //входящие рёбра

        public Block(String l) {
            label = l;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(label + ":\n");
            for (IR.Instr i : instrs)
                sb.append("  ").append(i).append("\n");
            if (!succ.isEmpty()) {
                sb.append("  -> ");
                for (Block b : succ)
                    sb.append(b.label).append(" ");
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    public List<Block> blocks = new ArrayList<>();
    public Block entry; //entry block

    public static CFG build(IR.Program p) {
        CFG cfg = new CFG();
        // build block
        List<IR.Instr> code = p.code;
        Block cur = new Block("B0");
        cfg.entry = cur;
        cfg.blocks.add(cur);
        Map<String, Block> labelToBlock = new HashMap<>();
        List<IR.Instr> pending = new ArrayList<>();

        for (IR.Instr in : code) {
            if (in.op == IR.Op.LABEL) {
                // new block
                Block b = new Block(in.label);
                labelToBlock.put(in.label, b);
                cfg.blocks.add(b);
                if (!cur.instrs.isEmpty() || cur == cfg.entry) {
                    // connect current block with next if there is no jump
                    boolean endsWithJump = !cur.instrs.isEmpty() &&
                            (cur.instrs.get(cur.instrs.size() - 1).op == IR.Op.JUMP ||
                                    cur.instrs.get(cur.instrs.size() - 1).op == IR.Op.BRANCH ||
                                    cur.instrs.get(cur.instrs.size() - 1).op == IR.Op.RET);
                    if (!endsWithJump)
                        cfg.addEdge(cur, b);
                }
                cur = b;
            } else if (in.op == IR.Op.JUMP || in.op == IR.Op.BRANCH || in.op == IR.Op.RET) {
                //last instruction
                cur.instrs.add(in);
                pending.add(in);
            } else {
                cur.instrs.add(in);
            }
        }
        // bind transitions
        for (int i = 0; i < cfg.blocks.size(); i++) {
            Block b = cfg.blocks.get(i);
            for (IR.Instr in : b.instrs) {
                if (in.op == IR.Op.JUMP) {
                    Block t = labelToBlock.get(in.label);
                    if (t != null)
                        cfg.addEdge(b, t);
                } else if (in.op == IR.Op.BRANCH) {
                    Block t = labelToBlock.get(in.label);
                    if (t != null)
                        cfg.addEdge(b, t);
                    // next block
                    if (i + 1 < cfg.blocks.size())
                        cfg.addEdge(b, cfg.blocks.get(i + 1));
                }
            }
            // if here is no transition in current block then go to next block
            if (b.succ.isEmpty() && i + 1 < cfg.blocks.size() && !b.instrs.isEmpty()) {
                IR.Op last = b.instrs.get(b.instrs.size() - 1).op;
                if (last != IR.Op.JUMP && last != IR.Op.RET) {
                    cfg.addEdge(b, cfg.blocks.get(i + 1));
                }
            }
        }
        return cfg;
    }

    private void addEdge(Block from, Block to) {
        if (!from.succ.contains(to))
            from.succ.add(to);
        if (!to.pred.contains(from))
            to.pred.add(from);
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder("digraph CFG {\n");
        for (Block b : blocks) {
            sb.append("  ").append(b.label).append(" [label=\"").append(b.label).append("\\n");
            for (int i = 0; i < Math.min(b.instrs.size(), 6); i++)
                sb.append(b.instrs.get(i).toString().replace("\"", "'")).append("\\l");
            sb.append("\"];\n");
        }
        for (Block b : blocks) for (Block s : b.succ)
            sb.append("  ").append(b.label).append(" -> ").append(s.label).append(";\n");
        sb.append("}\n");
        return sb.toString();
    }
}