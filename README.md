# :computer: Compilers IA to IB summer work: Lexical Analyser and SLR parser in Java.
This is my implementation of a lexer/lexical analyser and an SLR parser in Java for my IA to IB compilers summer work task. (*Note: "Part IA" is the 1st year of the Computer Science Tripos at Cambridge. "Part IB" corresponds to the second year.*)

**:pushpin: Note:** This repository is set to public to showcase this project; however, this doesn't mean the code can be freely copied and used, please see the [Copyright Notice](#grey_exclamation-copyright-notice) below.

---

## :clipboard: Task Description
> "You are to write a lexer and a parser for a new calculator application. The calculator is to read in ASCII strings containing arithmetic expressions (e.g. “2.3+4”) and you
> should emit a parse tree for the calculation. Accepted input numbers are to be signed floating point numbers. The operators, in order of increasing precedence are:*

| Operator   | Description                           |
| --- | -------------------------------------------- |
| +   |  A diadic infix operator, left associative   |
| -   |  A diadic infix operator, left associative   |
| *   |  A diadic infix operator, right associative  |
| cos |  A monadic prefix operator                   |
| !   |  A monadic postfix operator                  |

> 1. Define production rules for a grammar to accept signed floating point numbers.
> 1. An expression is a valid use of one of the operators above. Give production rules to show how each of the above operators can be used, noting the associativity and precedence.
> 1. List the terminal and non-terminal symbols of the language, and define an entry point for the grammar of calculations: a statement is a number or a valid application of one of the operators.
> 1. Write a program to lex an input ASCII string into tokens accepted by this language.
> 1. Write a parser based on an LR(0) technique to convert the token stream into a parse tree.

---

## :thought_balloon: My Solution
I organised my compiler front-end (the lexical analyser & the parser) into 2 Java packages:
```
src
 |__ lexer
 |__ parser
 ...
```
The `LexicalAnalyser.java` class in the `lexer` package implements the lexer itself. Its static method `scan` scanse the given `.txt` file (its 1st line) and produces a corresponding list of tokens. Its implementation is specific to the expression language being considered in this exercise.

The `Parser.java` class in the `parser` package implements the parser itself. I decided to implement an SLR parser, which is based on a LR(0) technique, by following the guidelines given in the reference book *"Compilers: principles, techniques and tools (2nd edition)" -- Aho, A.V., Sethi, R. & Ullman, J.D.*. The most interesting parts of the project are here! :)

The `Main.java` class contains the main program which:
1. 'Lexes' the specified input file (`input.txt`) to produce a token list. (Lexical Analyser)
1. Parses the input using the stream of tokens produced by the lexer. (Parser)
1. Outputs: the contents of the `input.txt` & `grammar.txt` files, the stream of tokens produced in step 1, the productions output by the parser and a visual representation of the final parse tree created.

### Example output:
```
------------------
Input from .\src\input.txt : 
---
-4+0.5

------------------
Token stream produced by lexer: 
---
[< MINUS, - >, < INT, 4 >, < PLUS, + >, < FLOAT, 0.5 >]

------------------
Grammar specification (in ./src/grammar.txt) : 
---
Non-terminals: stmt,N,E,S,F,T,E',sign
Terminals: INT,FLOAT,PLUS,MINUS,MULT,COS,FACTORIAL,ε
Productions:
stmt -> N | E
N -> sign INT | sign FLOAT
sign -> MINUS | ε
E -> S E'
E' -> PLUS S E' | MINUS S E' | ε
S -> F MULT S | F
F -> COS F | T
T -> INT FACTORIAL | N

------------------
Reductions output by parser: 
---
sign -> MINUS
N -> sign INT
T -> N
F -> T
S -> F
sign -> ε
N -> sign FLOAT
T -> N
F -> T
S -> F
E' -> ε
E' -> PLUS S E'
E -> S E'
stmt -> E

------------------
Parse tree produced: 
---
stmt
└── E
    ├── E'
    │   ├── E'
    │   │   └── ε
    │   ├── S
    │   │   └── F
    │   │       └── T
    │   │           └── N
    │   │               ├── FLOAT
    │   │               │   └── 0.5
    │   │               └── sign
    │   │                   └── ε
    │   └── PLUS
    └── S
        └── F
            └── T
                └── N
                    ├── INT
                    │   └── 4
                    └── sign
                        └── MINUS
```

---

## :grey_exclamation: Copyright Notice

Copyright &copy; 2020 Alba Navarro Rosales. All rights reserved. Please do not copy or modify the design or software in this repository for any purpose other than with the express written permission of the author, neither claim it as your own. Do check [this](https://choosealicense.com/no-permission/) out, thanks! :)
<br>:point_up: And remember- plagiarism is bad!
