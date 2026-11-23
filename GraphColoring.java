package org.example;

import java.util.*;

public class GraphColoring {
    public Map<String, Integer> color = new HashMap<>();
    public int colors = 0;

    public void greedy(Map<String, Set<String>> graph) {
        List<String> nodes = new ArrayList<>(graph.keySet());
        nodes.sort((a, b) -> graph.get(b).size() - graph.get(a).size());
        for (String n : nodes) {
            Set<Integer> used = new HashSet<>();
            for (String nbr : graph.getOrDefault(n, Collections.emptySet()))
                if (color.containsKey(nbr)) used.add(color.get(nbr));
            int c = 0;
            while (used.contains(c)) c++;
            color.put(n, c);
            colors = Math.max(colors, c + 1);
        }
    }

    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("Временные/переменные и цвета регистров (").append(colors).append(" цветов):\n");
        List<String> keys = new ArrayList<>(color.keySet());
        Collections.sort(keys);
        for (String k : keys) sb.append("  ").append(k).append(" -> r").append(color.get(k)).append("\n");
        return sb.toString();
    }
}