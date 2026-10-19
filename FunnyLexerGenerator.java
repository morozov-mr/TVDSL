package org.example;

import java.util.*;

public class FunnyLexerGenerator {
    static class NFAState {
        static int globalId = 0;
        int id = globalId++;
        List<NFAState>[] trans = new List[128];
        List<NFAState> epsilons = new ArrayList<>();
        TokenType accept = null;

        void addTransition(char c, NFAState next) {
            if (trans[c] == null)
                trans[c] = new ArrayList<>();
            trans[c].add(next);
        }
        void addEpsilon(NFAState next) { epsilons.add(next); }
    }

    static class NFA {
        NFAState start, end;
        NFA(NFAState start, NFAState end) {
            this.start = start;
            this.end = end;
        }
    }

    public static class DFAState {
        public static int globalId = 0;
        public int id = globalId++;
        public DFAState[] trans = new DFAState[128];
        public TokenType accept = null;
        public Set<NFAState> nfaStates = new HashSet<>();
    }

    static NFA fromChar(char c) {
        NFAState s = new NFAState(), e = new NFAState();
        s.addTransition(c, e);
        return new NFA(s, e);
    }

    static NFA fromRange(char from, char to) {
        NFAState s = new NFAState(), e = new NFAState();
        for (char c = from; c <= to; c++)
            s.addTransition(c, e);
        return new NFA(s, e);
    }

    static NFA fromString(String str) {
        NFA cur = fromChar(str.charAt(0));
        for (int i = 1; i < str.length(); i++)
            cur = concat(cur, fromChar(str.charAt(i)));
        return cur;
    }

    static NFA notChar(char exclude) {
        NFAState s = new NFAState(), e = new NFAState();
        for (char c = 0; c < 128; c++) {
            if (c != exclude)
                s.addTransition(c, e);
        }
        return new NFA(s, e);
    }

    static NFA concat(NFA a, NFA b) {
        a.end.addEpsilon(b.start);
        return new NFA(a.start, b.end);
    }

    static NFA union(NFA... nfas) {
        NFAState s = new NFAState(), e = new NFAState();
        for (NFA nfa : nfas) { s.addEpsilon(nfa.start); nfa.end.addEpsilon(e); } return new NFA(s, e);
    }

    static NFA star(NFA a) {
        NFAState s = new NFAState(), e = new NFAState();
        s.addEpsilon(a.start); s.addEpsilon(e); a.end.addEpsilon(a.start); a.end.addEpsilon(e); return new NFA(s, e);
    }

    static NFA plus(NFA a) {
        NFAState s = new NFAState(), e = new NFAState();
        s.addEpsilon(a.start); a.end.addEpsilon(a.start); a.end.addEpsilon(e); return new NFA(s, e);
    }

    static NFA buildFunnyLexerNFA() {
        NFAState rootStart = new NFAState();
        List<NFA> tokens = new ArrayList<>();

        tokens.add(bind(fromString("returns"), TokenType.KW_RETURNS));
        tokens.add(bind(fromString("while"), TokenType.KW_WHILE));
        tokens.add(bind(fromString("if"), TokenType.KW_IF));
        tokens.add(bind(fromString("else"), TokenType.KW_ELSE));
        tokens.add(bind(fromString("assert"), TokenType.KW_ASSERT));
        tokens.add(bind(fromString("assume"), TokenType.KW_ASSUME));
        tokens.add(bind(fromString("invariant"), TokenType.KW_INVARIANT));
        tokens.add(bind(fromString("length"), TokenType.KW_LENGTH));
        tokens.add(bind(fromString("requires"), TokenType.KW_REQUIRES));
        tokens.add(bind(fromString("ensures"), TokenType.KW_ENSURES));
        tokens.add(bind(fromString("uses"), TokenType.KW_USES));
        tokens.add(bind(fromString("int"), TokenType.KW_INT));
        tokens.add(bind(fromString("true"), TokenType.KW_TRUE));
        tokens.add(bind(fromString("false"), TokenType.KW_FALSE));
        tokens.add(bind(fromString("not"), TokenType.KW_NOT));
        tokens.add(bind(fromString("and"), TokenType.KW_AND));
        tokens.add(bind(fromString("or"), TokenType.KW_OR));
        tokens.add(bind(fromString("forall"), TokenType.KW_FORALL));
        tokens.add(bind(fromString("exists"), TokenType.KW_EXISTS));

        tokens.add(bind(fromString("=="), TokenType.EQ_EQ));
        tokens.add(bind(fromString("!="), TokenType.NOT_EQ));
        tokens.add(bind(fromString("<="), TokenType.LESS_EQ));
        tokens.add(bind(fromString(">="), TokenType.GREATER_EQ));
        tokens.add(bind(fromString("->"), TokenType.IMPLIES));
        tokens.add(bind(fromString("=>"), TokenType.DEF_ARROW));

        tokens.add(bind(fromString("("), TokenType.LPAREN));
        tokens.add(bind(fromString(")"), TokenType.RPAREN));
        tokens.add(bind(fromString("["), TokenType.LBRACK));
        tokens.add(bind(fromString("]"), TokenType.RBRACK));
        tokens.add(bind(fromString("{"), TokenType.LBRACE));
        tokens.add(bind(fromString("}"), TokenType.RBRACE));
        tokens.add(bind(fromString(","), TokenType.COMMA));
        tokens.add(bind(fromString(";"), TokenType.SEMICOLON));
        tokens.add(bind(fromString(":"), TokenType.COLON));
        tokens.add(bind(fromString("|"), TokenType.PIPE));
        tokens.add(bind(fromString("+"), TokenType.PLUS));
        tokens.add(bind(fromString("-"), TokenType.MINUS));
        tokens.add(bind(fromString("*"), TokenType.STAR));
        tokens.add(bind(fromString("/"), TokenType.SLASH));
        tokens.add(bind(fromString("<"), TokenType.LESS));
        tokens.add(bind(fromString(">"), TokenType.GREATER));
        tokens.add(bind(fromString("="), TokenType.ASSIGN));

        NFA comment = concat(fromString("//"), star(notChar('\n')));
        tokens.add(bind(comment, TokenType.COMMENT));
        NFA ws = plus(union(fromChar(' '), fromChar('\t'), fromChar('\r'), fromChar('\n')));
        tokens.add(bind(ws, TokenType.WS));

        NFA alpha = union(fromRange('a', 'z'), fromRange('A', 'Z'), fromChar('_'));
        NFA alnum = union(fromRange('a', 'z'), fromRange('A', 'Z'), fromRange('0', '9'), fromChar('_'));
        tokens.add(bind(concat(alpha, star(alnum)), TokenType.IDENT));

        NFA nonzero = concat(fromRange('1', '9'), star(fromRange('0', '9')));
        tokens.add(bind(union(fromChar('0'), nonzero), TokenType.INT_LIT));

        for (NFA t : tokens)
            rootStart.addEpsilon(t.start);
        return new NFA(rootStart, new NFAState());
    }

    static NFA bind(NFA nfa, TokenType type) {
        nfa.end.accept = type;
        return nfa;
    }

    static Set<NFAState> epsilonClosure(Set<NFAState> states) {
        Set<NFAState> closure = new HashSet<>(states);
        Stack<NFAState> stack = new Stack<>();
        stack.addAll(states);
        while (!stack.isEmpty()) {
            NFAState s = stack.pop();
            for (NFAState eps : s.epsilons) {
                if (closure.add(eps))
                    stack.push(eps);
            }
        }
        return closure;
    }

    static DFAState convertToDFA(NFA nfa, List<DFAState> dfaStates) {
        DFAState.globalId = 0;
        Map<Set<NFAState>, DFAState> map = new HashMap<>();
        Set<NFAState> startSet = epsilonClosure(Collections.singleton(nfa.start));
        DFAState startDFA = new DFAState();
        startDFA.nfaStates = startSet;
        resolveAccept(startDFA);
        map.put(startSet, startDFA);
        dfaStates.add(startDFA);
        Queue<DFAState> queue = new LinkedList<>();
        queue.add(startDFA);
        while (!queue.isEmpty()) {
            DFAState current = queue.poll();
            for (char c = 0; c < 128; c++) {
                Set<NFAState> moveSet = new HashSet<>();
                for (NFAState ns : current.nfaStates) {
                    if (ns.trans[c] != null)
                        moveSet.addAll(ns.trans[c]);
                }
                if (moveSet.isEmpty())
                    continue;
                Set<NFAState> closure = epsilonClosure(moveSet);
                DFAState nextDFA = map.get(closure);
                if (nextDFA == null) {
                    nextDFA = new DFAState();
                    nextDFA.nfaStates = closure;
                    resolveAccept(nextDFA);
                    map.put(closure, nextDFA);
                    dfaStates.add(nextDFA);
                    queue.add(nextDFA);
                }
                current.trans[c] = nextDFA;
            }
        }
        return startDFA;
    }

    static void resolveAccept(DFAState dfa) {
        for (NFAState ns : dfa.nfaStates) {
            if (ns.accept != null) {
                if (dfa.accept == null || ns.accept.ordinal() < dfa.accept.ordinal())
                    dfa.accept = ns.accept;
            }
        }
    }

    static DFAState minimizeDFA(List<DFAState> dfaStates, DFAState startState, List<DFAState> minDfaStatesOut) {
        DFAState trap = new DFAState();
        trap.accept = TokenType.ERROR;
        for (char c = 0; c < 128; c++)
            trap.trans[c] = trap;
        dfaStates.add(trap);
        for (DFAState d : dfaStates) {
            for (char c = 0; c < 128; c++) {
                if (d.trans[c] == null)
                    d.trans[c] = trap;
            }
        }

        Map<TokenType, Set<DFAState>> partitionsMap = new HashMap<>();
        Set<DFAState> nonAccepting = new HashSet<>();
        for (DFAState d : dfaStates) {
            if (d.accept == null)
                nonAccepting.add(d);
            else
                partitionsMap.computeIfAbsent(d.accept, k -> new HashSet<>()).add(d);
        }
        List<Set<DFAState>> P = new ArrayList<>(partitionsMap.values());
        if (!nonAccepting.isEmpty()) P.add(nonAccepting);
        Queue<Set<DFAState>> W = new LinkedList<>(P);
        while (!W.isEmpty()) {
            Set<DFAState> A = W.poll();
            for (char c = 0; c < 128; c++) {
                Set<DFAState> X = new HashSet<>();
                for (DFAState d : dfaStates) {
                    if (A.contains(d.trans[c]))
                        X.add(d);
                }
                if (X.isEmpty())
                    continue;
                List<Set<DFAState>> nextP = new ArrayList<>();
                for (Set<DFAState> Y : P) {
                    Set<DFAState> intersect = new HashSet<>(Y);
                    intersect.retainAll(X);
                    Set<DFAState> diff = new HashSet<>(Y);
                    diff.removeAll(X);
                    if (!intersect.isEmpty() && !diff.isEmpty()) {
                        nextP.add(intersect);
                        nextP.add(diff);
                        if (W.contains(Y)) { W.remove(Y);
                            W.add(intersect);
                            W.add(diff);
                        }
                        else
                            W.add(intersect.size() <= diff.size() ? intersect : diff);
                    } else
                        nextP.add(Y);
                }
                P = nextP;
            }
        }
        DFAState.globalId = 0;
        Map<Set<DFAState>, DFAState> pToMin = new HashMap<>();
        DFAState minStart = null;
        for (Set<DFAState> partition : P) {
            DFAState m = new DFAState();
            m.accept = partition.iterator().next().accept;
            pToMin.put(partition, m);
            minDfaStatesOut.add(m);
            if (partition.contains(startState))
                minStart = m;
        }
        for (Set<DFAState> partition : P) {
            DFAState m = pToMin.get(partition);
            DFAState oldRep = partition.iterator().next();
            for (char c = 0; c < 128; c++) {
                for (Set<DFAState> pTarget : P) {
                    if (pTarget.contains(oldRep.trans[c])) {
                        m.trans[c] = pToMin.get(pTarget);
                        break;
                    }
                }
            }
        }
        return minStart;
    }

    public static DFAState buildMinimizedDFA() {
        NFA nfa = buildFunnyLexerNFA();
        List<DFAState> dfaStates = new ArrayList<>();
        DFAState dfaStart = convertToDFA(nfa, dfaStates);
        List<DFAState> minDfaStates = new ArrayList<>();
        return minimizeDFA(dfaStates, dfaStart, minDfaStates);
    }

    public static List<Token> tokenizeToList(String input, DFAState start) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0, line = 1, col = 1;
        while (pos < input.length()) {
            DFAState curr = start;
            int lastAcceptPos = -1;
            TokenType lastAcceptToken = null;
            int i = pos;
            while (i < input.length()) {
                char c = input.charAt(i);
                if (c > 127)
                    break;
                curr = curr.trans[c];
                if (curr.accept == TokenType.ERROR)
                    break;
                if (curr.accept != null) {
                    lastAcceptPos = i;
                    lastAcceptToken = curr.accept;
                }
                i++;
            }
            if (lastAcceptToken != null) {
                String lexeme = input.substring(pos, lastAcceptPos + 1);
                if (lastAcceptToken != TokenType.WS && lastAcceptToken != TokenType.COMMENT) {
                    tokens.add(new Token(lastAcceptToken, lexeme, line, col));
                }
                for (int k = pos; k <= lastAcceptPos; k++) {
                    if (input.charAt(k) == '\n') { line++; col = 1; } else col++;
                }
                pos = lastAcceptPos + 1;
            } else {
                char c = input.charAt(pos);
                tokens.add(new Token(TokenType.ERROR, String.valueOf(c), line, col));
                if (c == '\n') { line++; col = 1; } else col++;
                pos++;
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }
}