package org.example;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Инициализация лексера...");
        FunnyLexerGenerator.DFAState minStart = FunnyLexerGenerator.buildMinimizedDFA();

        String[] tests = {
                "main() returns r:int {\n" +
                "   x = 10;\n" +
                "   y = x + 5;\n" +
                "   r = y * 2;\n" +
                "}",

                "main() returns r:int {\n" +
                "    r = true;\n" +
                "}",

                "main() returns r:int {\n" +
                "    r = z + 1;\n" +
                "}",

                "add(a:int, b:int) returns r:int {\n" +
                "    a = 5;\n" +
                "    r = a + b;\n" +
                "}",

                "add(a:int, b:int) returns r:int { r = a + b; }\n" +
                "main() returns r:int {\n" +
                "    r = add(1);\n" +
                "}",

                "divmod(a:int, b:int) returns q:int, r:int { q = a / b; r = a - q * b; }\n" +
                "main() returns r:int {\n" +
                "    r = divmod(10, 3) + 1;\n" +
                "}",

                "divmod(a:int, b:int) returns q:int, r:int { q = a / b; r = a - q * b; }\n" +
                "main() returns res:int {\n" +
                "    q, r = divmod(10, 3);\n" +
                "    res = q + r;\n" +
                "}",

                "main() returns r:int uses a:int[] {\n" +
                "    a[0] = 10;\n" +
                "    r = length(a);\n" +
                "}"
        };

        for (int i = 0; i < tests.length; i++) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("=== Тест " + (i + 1) + " ===");
            System.out.println("=".repeat(60));
            System.out.println("\nИсходный код:");
            System.out.println(tests[i]);

            List<Token> tokens = FunnyLexerGenerator.tokenizeToList(tests[i], minStart);

            FunnyParser parser = new FunnyParser(tokens);
            Map<String, Object> ast = parser.parseProgram();

            //System.out.println("\n Абстрактное синтаксическое дерево (AST):");
            //System.out.println(FunnyParser.toJson(ast));

            List<String> allErrors = new ArrayList<>(parser.errors);
            System.out.println("\n Синтаксические ошибки парсера:");
            if (parser.errors.isEmpty()) {
                System.out.println("   Нет синтаксических ошибок.");
            } else {
                for (String err : parser.errors) {
                    System.out.println("   • " + err);
                }
            }

            TypeChecker checker = new TypeChecker(allErrors);
            checker.check(ast);

            System.out.println("\n️  Семантические ошибки (проверка типов):");
            List<String> semanticErrors = allErrors.subList(parser.errors.size(), allErrors.size());
            if (semanticErrors.isEmpty()) {
                System.out.println("   Нет семантических ошибок.");
            } else {
                for (String err : semanticErrors) {
                    System.out.println("   • " + err);
                }
            }

            System.out.println("\n Итог:");
            if (allErrors.isEmpty()) {
                System.out.println("    Программа успешно прошла синтаксический и семантический анализ.");
            } else {
                System.out.println("    Найдено ошибок: " + allErrors.size() +
                        " (синтаксических: " + parser.errors.size() +
                        ", семантических: " + semanticErrors.size() + ")");
            }
        }
    }
}