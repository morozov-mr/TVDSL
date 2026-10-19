FunnyLexerGenerator - лексер
FunnyParser - парсер со встроенным синтаксическим анализом (пофиксил синтаксическое ошибки)
TypeChecker - семантический анализ

Вывод аннотированного AST с синтаксическом и семантическим анализом для положительных и отрицательных тестов из Main:

============================================================
=== Тест 1 ===
============================================================

Исходный код:
main() returns r:int {
   x = 10;
   y = x + 5;
   r = y * 2;
}

 Абстрактное синтаксическое дерево (AST):
{
  "type": "Program",
  "functions": [
    {
      "type": "Function",
      "line": 1,
      "col": 1,
      "name": "main",
      "params": [],
      "returns": [
        {
          "type": "Param",
          "line": 1,
          "col": 16,
          "name": "r",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 1,
        "col": 22,
        "statements": [
          {
            "type": "Assignment",
            "line": 2,
            "col": 4,
            "targets": [
              {
                "type": "Variable",
                "line": 2,
                "col": 4,
                "name": "x"
              }
            ],
            "value": {
              "type": "IntLiteral",
              "line": 2,
              "col": 8,
              "value": 10
            }
          },
          {
            "type": "Assignment",
            "line": 3,
            "col": 4,
            "targets": [
              {
                "type": "Variable",
                "line": 3,
                "col": 4,
                "name": "y"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 3,
              "col": 10,
              "operator": "+",
              "left": {
                "type": "Variable",
                "line": 3,
                "col": 8,
                "name": "x"
              },
              "right": {
                "type": "IntLiteral",
                "line": 3,
                "col": 12,
                "value": 5
              }
            }
          },
          {
            "type": "Assignment",
            "line": 4,
            "col": 4,
            "targets": [
              {
                "type": "Variable",
                "line": 4,
                "col": 4,
                "name": "r"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 4,
              "col": 10,
              "operator": "*",
              "left": {
                "type": "Variable",
                "line": 4,
                "col": 8,
                "name": "y"
              },
              "right": {
                "type": "IntLiteral",
                "line": 4,
                "col": 12,
                "value": 2
              }
            }
          }
        ]
      }
    }
  ],
  "formulas": []
}

 Синтаксические ошибки парсера:
   Нет синтаксических ошибок.

️  Семантические ошибки (проверка типов):
   Нет семантических ошибок.

 Итог:
    Программа успешно прошла синтаксический и семантический анализ.

============================================================
=== Тест 2 ===
============================================================

Исходный код:
main() returns r:int {
    r = true;
}

 Абстрактное синтаксическое дерево (AST):
{
  "type": "Program",
  "functions": [
    {
      "type": "Function",
      "line": 1,
      "col": 1,
      "name": "main",
      "params": [],
      "returns": [
        {
          "type": "Param",
          "line": 1,
          "col": 16,
          "name": "r",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 1,
        "col": 22,
        "statements": [
          {
            "type": "Assignment",
            "line": 2,
            "col": 5,
            "targets": [
              {
                "type": "Variable",
                "line": 2,
                "col": 5,
                "name": "r"
              }
            ],
            "value": {
              "type": "BoolLiteral",
              "line": 2,
              "col": 9,
              "value": true
            }
          }
        ]
      }
    }
  ],
  "formulas": []
}

 Синтаксические ошибки парсера:
   Нет синтаксических ошибок.

️  Семантические ошибки (проверка типов):
   • Ошибка типизации на 2:5 - Несовпадение типов 'r': int = bool

 Итог:
    Найдено ошибок: 1 (синтаксических: 0, семантических: 1)

============================================================
=== Тест 3 ===
============================================================

Исходный код:
main() returns r:int {
    r = z + 1;
}

 Абстрактное синтаксическое дерево (AST):
{
  "type": "Program",
  "functions": [
    {
      "type": "Function",
      "line": 1,
      "col": 1,
      "name": "main",
      "params": [],
      "returns": [
        {
          "type": "Param",
          "line": 1,
          "col": 16,
          "name": "r",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 1,
        "col": 22,
        "statements": [
          {
            "type": "Assignment",
            "line": 2,
            "col": 5,
            "targets": [
              {
                "type": "Variable",
                "line": 2,
                "col": 5,
                "name": "r"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 2,
              "col": 11,
              "operator": "+",
              "left": {
                "type": "Variable",
                "line": 2,
                "col": 9,
                "name": "z"
              },
              "right": {
                "type": "IntLiteral",
                "line": 2,
                "col": 13,
                "value": 1
              }
            }
          }
        ]
      }
    }
  ],
  "formulas": []
}

 Синтаксические ошибки парсера:
   Нет синтаксических ошибок.

️  Семантические ошибки (проверка типов):
   • Ошибка типизации на 2:9 - Необъявленная переменная 'z'

 Итог:
    Найдено ошибок: 1 (синтаксических: 0, семантических: 1)

============================================================
=== Тест 4 ===
============================================================

Исходный код:
add(a:int, b:int) returns r:int {
    a = 5;
    r = a + b;
}

 Абстрактное синтаксическое дерево (AST):
{
  "type": "Program",
  "functions": [
    {
      "type": "Function",
      "line": 1,
      "col": 1,
      "name": "add",
      "params": [
        {
          "type": "Param",
          "line": 1,
          "col": 5,
          "name": "a",
          "varType": "int"
        },
        {
          "type": "Param",
          "line": 1,
          "col": 12,
          "name": "b",
          "varType": "int"
        }
      ],
      "returns": [
        {
          "type": "Param",
          "line": 1,
          "col": 27,
          "name": "r",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 1,
        "col": 33,
        "statements": [
          {
            "type": "Assignment",
            "line": 2,
            "col": 5,
            "targets": [
              {
                "type": "Variable",
                "line": 2,
                "col": 5,
                "name": "a"
              }
            ],
            "value": {
              "type": "IntLiteral",
              "line": 2,
              "col": 9,
              "value": 5
            }
          },
          {
            "type": "Assignment",
            "line": 3,
            "col": 5,
            "targets": [
              {
                "type": "Variable",
                "line": 3,
                "col": 5,
                "name": "r"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 3,
              "col": 11,
              "operator": "+",
              "left": {
                "type": "Variable",
                "line": 3,
                "col": 9,
                "name": "a"
              },
              "right": {
                "type": "Variable",
                "line": 3,
                "col": 13,
                "name": "b"
              }
            }
          }
        ]
      }
    }
  ],
  "formulas": []
}

 Синтаксические ошибки парсера:
   Нет синтаксических ошибок.

️  Семантические ошибки (проверка типов):
   • Ошибка типизации на 2:5 - Запись в read-only параметр 'a'

 Итог:
    Найдено ошибок: 1 (синтаксических: 0, семантических: 1)

============================================================
=== Тест 5 ===
============================================================

Исходный код:
add(a:int, b:int) returns r:int { r = a + b; }
main() returns r:int {
    r = add(1);
}

 Абстрактное синтаксическое дерево (AST):
{
  "type": "Program",
  "functions": [
    {
      "type": "Function",
      "line": 1,
      "col": 1,
      "name": "add",
      "params": [
        {
          "type": "Param",
          "line": 1,
          "col": 5,
          "name": "a",
          "varType": "int"
        },
        {
          "type": "Param",
          "line": 1,
          "col": 12,
          "name": "b",
          "varType": "int"
        }
      ],
      "returns": [
        {
          "type": "Param",
          "line": 1,
          "col": 27,
          "name": "r",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 1,
        "col": 33,
        "statements": [
          {
            "type": "Assignment",
            "line": 1,
            "col": 35,
            "targets": [
              {
                "type": "Variable",
                "line": 1,
                "col": 35,
                "name": "r"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 1,
              "col": 41,
              "operator": "+",
              "left": {
                "type": "Variable",
                "line": 1,
                "col": 39,
                "name": "a"
              },
              "right": {
                "type": "Variable",
                "line": 1,
                "col": 43,
                "name": "b"
              }
            }
          }
        ]
      }
    },
    {
      "type": "Function",
      "line": 2,
      "col": 1,
      "name": "main",
      "params": [],
      "returns": [
        {
          "type": "Param",
          "line": 2,
          "col": 16,
          "name": "r",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 2,
        "col": 22,
        "statements": [
          {
            "type": "Assignment",
            "line": 3,
            "col": 5,
            "targets": [
              {
                "type": "Variable",
                "line": 3,
                "col": 5,
                "name": "r"
              }
            ],
            "value": {
              "type": "FunctionCall",
              "line": 3,
              "col": 9,
              "function": "add",
              "arguments": [
                {
                  "type": "IntLiteral",
                  "line": 3,
                  "col": 13,
                  "value": 1
                }
              ]
            }
          }
        ]
      }
    }
  ],
  "formulas": []
}

 Синтаксические ошибки парсера:
   Нет синтаксических ошибок.

️  Семантические ошибки (проверка типов):
   • Ошибка типизации на 3:9 - Неверное число аргументов: 2 != 1

 Итог:
    Найдено ошибок: 1 (синтаксических: 0, семантических: 1)

============================================================
=== Тест 6 ===
============================================================

Исходный код:
divmod(a:int, b:int) returns q:int, r:int { q = a / b; r = a - q * b; }
main() returns r:int {
    r = divmod(10, 3) + 1;
}

 Абстрактное синтаксическое дерево (AST):
{
  "type": "Program",
  "functions": [
    {
      "type": "Function",
      "line": 1,
      "col": 1,
      "name": "divmod",
      "params": [
        {
          "type": "Param",
          "line": 1,
          "col": 8,
          "name": "a",
          "varType": "int"
        },
        {
          "type": "Param",
          "line": 1,
          "col": 15,
          "name": "b",
          "varType": "int"
        }
      ],
      "returns": [
        {
          "type": "Param",
          "line": 1,
          "col": 30,
          "name": "q",
          "varType": "int"
        },
        {
          "type": "Param",
          "line": 1,
          "col": 37,
          "name": "r",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 1,
        "col": 43,
        "statements": [
          {
            "type": "Assignment",
            "line": 1,
            "col": 45,
            "targets": [
              {
                "type": "Variable",
                "line": 1,
                "col": 45,
                "name": "q"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 1,
              "col": 51,
              "operator": "/",
              "left": {
                "type": "Variable",
                "line": 1,
                "col": 49,
                "name": "a"
              },
              "right": {
                "type": "Variable",
                "line": 1,
                "col": 53,
                "name": "b"
              }
            }
          },
          {
            "type": "Assignment",
            "line": 1,
            "col": 56,
            "targets": [
              {
                "type": "Variable",
                "line": 1,
                "col": 56,
                "name": "r"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 1,
              "col": 62,
              "operator": "-",
              "left": {
                "type": "Variable",
                "line": 1,
                "col": 60,
                "name": "a"
              },
              "right": {
                "type": "BinaryOp",
                "line": 1,
                "col": 66,
                "operator": "*",
                "left": {
                  "type": "Variable",
                  "line": 1,
                  "col": 64,
                  "name": "q"
                },
                "right": {
                  "type": "Variable",
                  "line": 1,
                  "col": 68,
                  "name": "b"
                }
              }
            }
          }
        ]
      }
    },
    {
      "type": "Function",
      "line": 2,
      "col": 1,
      "name": "main",
      "params": [],
      "returns": [
        {
          "type": "Param",
          "line": 2,
          "col": 16,
          "name": "r",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 2,
        "col": 22,
        "statements": [
          {
            "type": "Assignment",
            "line": 3,
            "col": 5,
            "targets": [
              {
                "type": "Variable",
                "line": 3,
                "col": 5,
                "name": "r"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 3,
              "col": 23,
              "operator": "+",
              "left": {
                "type": "FunctionCall",
                "line": 3,
                "col": 9,
                "function": "divmod",
                "arguments": [
                  {
                    "type": "IntLiteral",
                    "line": 3,
                    "col": 16,
                    "value": 10
                  },
                  {
                    "type": "IntLiteral",
                    "line": 3,
                    "col": 20,
                    "value": 3
                  }
                ]
              },
              "right": {
                "type": "IntLiteral",
                "line": 3,
                "col": 25,
                "value": 1
              }
            }
          }
        ]
      }
    }
  ],
  "formulas": []
}

 Синтаксические ошибки парсера:
   Нет синтаксических ошибок.

️  Семантические ошибки (проверка типов):
   • Ошибка типизации на 3:9 - Множественный возврат недопустим в выражениях

 Итог:
    Найдено ошибок: 1 (синтаксических: 0, семантических: 1)

============================================================
=== Тест 7 ===
============================================================

Исходный код:
divmod(a:int, b:int) returns q:int, r:int { q = a / b; r = a - q * b; }
main() returns res:int {
    q, r = divmod(10, 3);
    res = q + r;
}

 Абстрактное синтаксическое дерево (AST):
{
  "type": "Program",
  "functions": [
    {
      "type": "Function",
      "line": 1,
      "col": 1,
      "name": "divmod",
      "params": [
        {
          "type": "Param",
          "line": 1,
          "col": 8,
          "name": "a",
          "varType": "int"
        },
        {
          "type": "Param",
          "line": 1,
          "col": 15,
          "name": "b",
          "varType": "int"
        }
      ],
      "returns": [
        {
          "type": "Param",
          "line": 1,
          "col": 30,
          "name": "q",
          "varType": "int"
        },
        {
          "type": "Param",
          "line": 1,
          "col": 37,
          "name": "r",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 1,
        "col": 43,
        "statements": [
          {
            "type": "Assignment",
            "line": 1,
            "col": 45,
            "targets": [
              {
                "type": "Variable",
                "line": 1,
                "col": 45,
                "name": "q"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 1,
              "col": 51,
              "operator": "/",
              "left": {
                "type": "Variable",
                "line": 1,
                "col": 49,
                "name": "a"
              },
              "right": {
                "type": "Variable",
                "line": 1,
                "col": 53,
                "name": "b"
              }
            }
          },
          {
            "type": "Assignment",
            "line": 1,
            "col": 56,
            "targets": [
              {
                "type": "Variable",
                "line": 1,
                "col": 56,
                "name": "r"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 1,
              "col": 62,
              "operator": "-",
              "left": {
                "type": "Variable",
                "line": 1,
                "col": 60,
                "name": "a"
              },
              "right": {
                "type": "BinaryOp",
                "line": 1,
                "col": 66,
                "operator": "*",
                "left": {
                  "type": "Variable",
                  "line": 1,
                  "col": 64,
                  "name": "q"
                },
                "right": {
                  "type": "Variable",
                  "line": 1,
                  "col": 68,
                  "name": "b"
                }
              }
            }
          }
        ]
      }
    },
    {
      "type": "Function",
      "line": 2,
      "col": 1,
      "name": "main",
      "params": [],
      "returns": [
        {
          "type": "Param",
          "line": 2,
          "col": 16,
          "name": "res",
          "varType": "int"
        }
      ],
      "body": {
        "type": "Block",
        "line": 2,
        "col": 24,
        "statements": [
          {
            "type": "Assignment",
            "line": 3,
            "col": 5,
            "targets": [
              {
                "type": "Variable",
                "line": 3,
                "col": 5,
                "name": "q"
              },
              {
                "type": "Variable",
                "line": 3,
                "col": 8,
                "name": "r"
              }
            ],
            "value": {
              "type": "FunctionCall",
              "line": 3,
              "col": 12,
              "function": "divmod",
              "arguments": [
                {
                  "type": "IntLiteral",
                  "line": 3,
                  "col": 19,
                  "value": 10
                },
                {
                  "type": "IntLiteral",
                  "line": 3,
                  "col": 23,
                  "value": 3
                }
              ]
            }
          },
          {
            "type": "Assignment",
            "line": 4,
            "col": 5,
            "targets": [
              {
                "type": "Variable",
                "line": 4,
                "col": 5,
                "name": "res"
              }
            ],
            "value": {
              "type": "BinaryOp",
              "line": 4,
              "col": 13,
              "operator": "+",
              "left": {
                "type": "Variable",
                "line": 4,
                "col": 11,
                "name": "q"
              },
              "right": {
                "type": "Variable",
                "line": 4,
                "col": 15,
                "name": "r"
              }
            }
          }
        ]
      }
    }
  ],
  "formulas": []
}

 Синтаксические ошибки парсера:
   Нет синтаксических ошибок.

️  Семантические ошибки (проверка типов):
   Нет семантических ошибок.

 Итог:
    Программа успешно прошла синтаксический и семантический анализ.

============================================================
=== Тест 8 ===
============================================================

Исходный код:
main() returns r:int uses a:int[] {
    a[0] = 10;
    r = length(a);
}

 Абстрактное синтаксическое дерево (AST):
{
  "type": "Program",
  "functions": [
    {
      "type": "Function",
      "line": 1,
      "col": 1,
      "name": "main",
      "params": [],
      "returns": [
        {
          "type": "Param",
          "line": 1,
          "col": 16,
          "name": "r",
          "varType": "int"
        }
      ],
      "uses": [
        {
          "type": "LocalVar",
          "line": 1,
          "col": 27,
          "name": "a",
          "varType": "int[]"
        }
      ],
      "body": {
        "type": "Block",
        "line": 1,
        "col": 35,
        "statements": [
          {
            "type": "Assignment",
            "line": 2,
            "col": 5,
            "targets": [
              {
                "type": "ArrayAccess",
                "line": 2,
                "col": 5,
                "array": {
                  "type": "Variable",
                  "line": 2,
                  "col": 5,
                  "name": "a"
                },
                "index": {
                  "type": "IntLiteral",
                  "line": 2,
                  "col": 7,
                  "value": 0
                }
              }
            ],
            "value": {
              "type": "IntLiteral",
              "line": 2,
              "col": 12,
              "value": 10
            }
          },
          {
            "type": "Assignment",
            "line": 3,
            "col": 5,
            "targets": [
              {
                "type": "Variable",
                "line": 3,
                "col": 5,
                "name": "r"
              }
            ],
            "value": {
              "type": "Length",
              "line": 3,
              "col": 9,
              "expression": {
                "type": "Variable",
                "line": 3,
                "col": 16,
                "name": "a"
              }
            }
          }
        ]
      }
    }
  ],
  "formulas": []
}

 Синтаксические ошибки парсера:
   Нет синтаксических ошибок.

️  Семантические ошибки (проверка типов):
   Нет семантических ошибок.

 Итог:
    Программа успешно прошла синтаксический и семантический анализ.
