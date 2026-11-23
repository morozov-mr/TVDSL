package org.example;

import java.util.*;

public class Liveness {
    public Map<CFG.Block, Set<String>> liveIn = new HashMap<>();
    public Map<CFG.Block, Set<String>> liveOut = new HashMap<>();
    public Map<CFG.Block, Set<String>> use = new HashMap<>();
    public Map<CFG.Block, Set<String>> def = new HashMap<>();

    public void compute(CFG cfg) {
        //use and def
        for (CFG.Block b : cfg.blocks) {
            use.put(b, new HashSet<>());
            def.put(b, new HashSet<>());
            for (IR.Instr in : b.instrs) {
                if (in.a != null && !def.get(b).contains(in.a))
                    use.get(b).add(in.a);
                if (in.b != null && !def.get(b).contains(in.b))
                    use.get(b).add(in.b);
                if (in.dst != null && isNotReadOnlyTarget(in))
                    def.get(b).add(in.dst);
            }
            liveIn.put(b, new HashSet<>());
            liveOut.put(b, new HashSet<>());
        }
        boolean changed = true;
        //итерация до неподвижной точки
        while (changed) {
            changed = false;
            for (int i = cfg.blocks.size() - 1; i >= 0; i--) {
                CFG.Block b = cfg.blocks.get(i);
                Set<String> out = new HashSet<>();
                for (CFG.Block s : b.succ)
                    out.addAll(liveIn.get(s));
                Set<String> in = new HashSet<>(use.get(b));
                Set<String> outMinusDef = new HashSet<>(out);
                outMinusDef.removeAll(def.get(b));
                in.addAll(outMinusDef);
                if (!out.equals(liveOut.get(b)) || !in.equals(liveIn.get(b))) {
                    liveOut.put(b, out); liveIn.put(b, in);
                    changed = true;
                }
            }
        }
    }

    private boolean isNotReadOnlyTarget(IR.Instr in) {
        return in.op != IR.Op.LABEL && in.op != IR.Op.JUMP && in.op != IR.Op.BRANCH &&
                in.op != IR.Op.RET && in.op != IR.Op.ASSERT && in.op != IR.Op.ASSUME &&
                in.op != IR.Op.PARAM;
    }

    public Map<String, Set<String>> conflictGraph(CFG cfg) {
        Map<String, Set<String>> g = new HashMap<>();
        for (CFG.Block b : cfg.blocks) {
            Set<String> live = new HashSet<>(liveOut.get(b));
            for (int i = b.instrs.size() - 1; i >= 0; i--) {
                IR.Instr in = b.instrs.get(i);
                if (in.dst != null && isNotReadOnlyTarget(in)) {
                    for (String l : live)
                        if (!l.equals(in.dst))
                            addEdge(g, in.dst, l);
                    live.remove(in.dst);
                }
                if (in.a != null)
                    live.add(in.a);
                if (in.b != null)
                    live.add(in.b);
            }
        }
        return g;
    }

    private void addEdge(Map<String, Set<String>> g, String a, String b) {
        g.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        g.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    public String graphToDot(Map<String, Set<String>> g) {
        StringBuilder sb = new StringBuilder("graph Conflicts {\n");
        for (Map.Entry<String, Set<String>> e : g.entrySet())
            for (String n : e.getValue())
                if (e.getKey().compareTo(n) < 0)
                    sb.append("  \"").append(e.getKey()).append("\" -- \"").append(n).append("\";\n");
        sb.append("}\n");
        return sb.toString();
    }
}