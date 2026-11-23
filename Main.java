package org.example;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        String[] tests = {
                "main() returns r:int {\n  x = 10;\n  y = x + 5;\n  r = y * 2;\n}",
                "main() returns r:int {\n  x = 3;\n  y = 4;\n  r = x * y + 0;\n  assert r >= 12;\n}",
                "add(a:int, b:int) returns r:int { r = a + b; }\n" +
                        "main() returns r:int {\n  r = add(1, 2);\n}",
                "loop(n:int) returns r:int {\n  i = 0;\n  s = 0;\n" +
                        "  while (i < n) invariant true {\n    s = s + i;\n    i = i + 1;\n  }\n  r = s;\n}"
        };

        for (int ti = 0; ti < tests.length; ti++) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("=== HW4 Тест " + (ti + 1) + " ===");
            System.out.println("=".repeat(60));
            FunnyLexerGenerator.DFAState minStart = FunnyLexerGenerator.buildMinimizedDFA();
            List<Token> tokens = FunnyLexerGenerator.tokenizeToList(tests[ti], minStart);
            FunnyParser parser = new FunnyParser(tokens);
            Map<String, Object> ast = parser.parseProgram();
            if (!parser.errors.isEmpty()) {
                System.out.println("Синтаксические ошибки, пропускаем: " + parser.errors);
                continue;
            }

            IRGenerator gen = new IRGenerator();
            List<IR.Program> programs = gen.generate(ast);
            for (IR.Program p : programs) {
                System.out.println("\n--- Функция " + p.name + " ---");
                CFG cfg = CFG.build(p);
                int instrsBefore = p.code.size();
                int tempsBefore = p.tmpCounter;

                System.out.println("\nIR до оптимизаций:");
                for (IR.Instr i : p.code) System.out.println("  " + i);

                Optimizer opt = new Optimizer();
                opt.optimize(p, cfg);

                // Пересобираем CFG после оптимизаций для корректных метрик
                CFG cfgOpt = CFG.build(p);
                System.out.println("\nIR после оптимизаций:");
                for (IR.Instr i : p.code) System.out.println("  " + i);

                System.out.println("\nCFG (DOT):");
                System.out.println(cfgOpt.toDot());

                Liveness lv = new Liveness();
                lv.compute(cfgOpt);
                Map<String, Set<String>> conflicts = lv.conflictGraph(cfgOpt);
                System.out.println("Граф конфликтов (DOT):");
                System.out.println(lv.graphToDot(conflicts));

                GraphColoring gc = new GraphColoring();
                gc.greedy(conflicts);
                System.out.println(gc.report());

                System.out.println("Метрики:");
                System.out.println("  Инструкций до: " + instrsBefore + ", после: " + p.code.size());
                System.out.println("  Временных до: " + tempsBefore + ", после: " + p.tmpCounter);
                System.out.println("  Свёрнуто констант: " + opt.constFolded);
                System.out.println("  Упрощений: " + opt.simplified);
                System.out.println("  Удалено DCE: " + opt.eliminated);
                System.out.println("  CSE замен: " + opt.cseReplaced);
                System.out.println("  Цветов регистров: " + gc.colors);
            }
        }
    }
}