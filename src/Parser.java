import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Parser {

    class Production {
        /**
         * Class representing a production for the grammar
         */
        private String head;
        private String body;

        Production(String head, String body) {
            this.head = head;
            this.body = body;
        }

        String getHead() {
            return head;
        }

        String getBody() {
            return body;
        }

        @Override
        public String toString() {
            return head + " -> " + body;
        }
    }

    class Grammar {
        private Set<Production> productionsSet; // note: dif prod bodies for same nonterm stored as separate elements in set
        private Map<String, Set<String>> productionsMap;  // stores prod bodies assoc w/each nonterm (--head)
        private Set<String> nonterminals;
        private Set<String> terminals;
        private Set<String> grammarSymbols;
        private String startSymbol;
        private Map<String, Set<String>> followTable = new HashMap<>(); // stores follow(A) for non-term A
        private Map<String, Set<String>> firstTable = new HashMap<>(); // stores first(x) for grammar symbol A

        /**
         * Create Grammar object from grammar specification in given file.
         * Note: the grammarSpecification file must have the following format:
         *  - 1st line: non-terminals, separated by a comma. 1st one is start symbol.
         *  - 2nd line: terminals, separated by a comma
         *  - then, one production per line
         *  - preferably a single production per non-terminal for clarity
         */
        Grammar(File grammarSpecification) throws IOException {
            productionsSet = new HashSet<>();
            productionsMap = new HashMap<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(grammarSpecification))) {
                try {
                    String nextLine = reader.readLine();  // read next line from file
                    // get non-terminals & terminals
                    String[] nontermSplit = nextLine.split(",");
                    nonterminals = new HashSet<>(Arrays.asList(nontermSplit));  // 1st line has non-terminals
                    startSymbol = nontermSplit[0]; // 1st non-term is the start symbol
                    grammarSymbols = new HashSet<>(nonterminals);
                    nextLine = reader.readLine();
                    terminals = new HashSet<>(Arrays.asList(nextLine.split(",")));  // 2nd line has terminals
                    grammarSymbols.addAll(terminals);
                    nextLine = reader.readLine();

                    // get productions
                    while (nextLine != null) {
                        // get head & prod bodies for the prod in this line
                        String[] splitHeadBody = nextLine.split("\\s->\\s");
                        String[] bodyProductions = splitHeadBody[1].split("\\s\\|\\s");

                        for (String body : bodyProductions) {  // add all prod for this non-term to productionsSet
                            Production prod = new Production(splitHeadBody[0], body);
                            productionsSet.add(prod);
                        }
                        // add all prod for this non-term to productionsMap
                        productionsMap.put(splitHeadBody[0], new HashSet<>(Arrays.asList(bodyProductions)));

                        nextLine = reader.readLine();  // read next line
                    }

                    // calc first for all gram symb
                    calcAllFirstSets();

                    // calc follow for all nonterm
                    calcAllFollowSets();

                } catch (NullPointerException e) {
                    System.out.println("Incorrect format of input file.");
                }

            } catch (IOException e) {
                throw new IOException("Can't access file " + grammarSpecification, e);
            }
        }

        /**
         * Create Grammar object from another grammar (i.e. expand this grammar)
         * @param grammar gram to expand
         * @param addNonterm new, additiona non-terminals
         * @param addTerm new, additional terminals
         * @param addProd new, additional productions
         * @param newStartSymb specifies new start symb
         */
        Grammar(Grammar grammar, Set<String> addNonterm, Set<String> addTerm, Set<Production> addProd, String newStartSymb){
            nonterminals = new HashSet<>(grammar.getNonterminals());
            if(addNonterm != null) nonterminals.addAll(addNonterm);

            terminals = new HashSet<>(grammar.getTerminals());
            if(addTerm != null) terminals.addAll(addTerm);

            grammarSymbols = new HashSet<>(nonterminals);
            grammarSymbols.addAll(terminals);

            productionsSet = new HashSet<>(grammar.getProductionsSet());
            productionsMap = new HashMap<>(grammar.getProductionsMap());
            if(addProd != null) {
                productionsSet.addAll(addProd);
                for (Production newProd : addProd) {
                    Set<String> bodies = productionsMap.getOrDefault(newProd.getHead(), new HashSet<>());
                    bodies.add(newProd.body);
                    productionsMap.put(newProd.getHead(), bodies);
                }
            }

            if(newStartSymb != null) startSymbol = newStartSymb;
            else startSymbol = grammar.startSymbol;

            // calc first for all gram symb
            calcAllFirstSets();

            // calc follow for all nonterm
            calcAllFollowSets();
        }

        Set<Production> getProductionsSet() {
            return new HashSet<>(productionsSet);
        }
        Map<String, Set<String>> getProductionsMap() {
            return new HashMap<>(productionsMap);
        }
        Set<String> getNonterminals() {
            return new HashSet<>(nonterminals);
        }
        Set<String> getTerminals() {
            return new HashSet<>(terminals);
        }
        String getStartSymbol() {
            return startSymbol;
        }
        Set<String> getGramSymbols() {
            return new HashSet<>(grammarSymbols);
        }

        /**
         * Calculates FOLLOW() for all nonterminals & stores results in followTable.
         * (FOLLOW(nonterm) repr the set of terminals that can appear immediately to the right of
         * nonterm (the param) in some sentential form.)
         */
        private void calcAllFollowSets(){
            // init followTable
            for(String nonterm : nonterminals){
                followTable.put(nonterm, new HashSet<>());
            }

            // put input right endmarker into follow(start symbol)
            Set<String> followStartSymb = followTable.get(startSymbol);
            followStartSymb.add("INPUTENDMARKER");
            followTable.replace(startSymbol, followStartSymb);

            boolean isUpdated = true;
            while(isUpdated){
                isUpdated = false;

                // for each nonterm A
                for(String nonterm : nonterminals){
                    Set<String> followA = followTable.get(nonterm); // get follow set calc so far for this nonterm
                    Set<String> bodies = productionsMap.get(nonterm); // get productions for this nonterm (A -> α B β or A -> α B)

                    for(String body : bodies){
                        List<String> elementsInBody = Arrays.asList(body.split("\\s"));

                        // for each nonterm B in prod body
                        for(String element : elementsInBody){
                            if(!nonterminals.contains(element)) continue; // only consider elements which are nonterminals (B)

                            // construct string β (str of elements after this element in the body of this prod)
                            StringBuilder betaStr = new StringBuilder();
                            int start = elementsInBody.indexOf(element) + 1;
                            for (int i = start; i < elementsInBody.size(); i++) {
                                if (i == start) betaStr.append(elementsInBody.get(i));
                                else betaStr.append(" ").append(elementsInBody.get(i));
                            }

                            Set<String> newFollowB = new HashSet<>(followTable.get(element));

                            // case 3 in alg: prod A -> α B or A -> α B β where epsilon in FIRST(β)
                            if(betaStr.toString().equals("")) newFollowB.addAll(followA);
                            else{
                                Set<String> firstForBetaStr = calcFirstForString(betaStr.toString());
                                if(firstForBetaStr.contains("ε")) newFollowB.addAll(followA);
                                // case 2
                                firstForBetaStr.remove("ε");
                                newFollowB.addAll(firstForBetaStr);
                            }

                            //if(!followTable.get(element).containsAll(newFollowB)){
                            if(followTable.get(element).size() != newFollowB.size()){
                                isUpdated = true;
                                followTable.replace(element, newFollowB);
                            }
                        }
                    }
                }
            }
        }

        /**
         * Gets FOLLOW(nonterm) from followTable.
         * nonterm (the param) in some sentential form.
         * @param nonterm nonterminal for which to calculate set of terminals
         */
        Set<String> calcFollow(String nonterm){
            return new HashSet<>(followTable.get(nonterm));
        }

        /**
         * Calculates set of items that begin a string derived from the
         * given grammar symbol, gramSymb. That is, FIRST(gramSymb)
         * Note: grammar must NOT be left recursive.
         *
         */
        private void calcAllFirstSets(){
            // init firstTable
            for(String gramSymb : grammarSymbols){
                Set<String> setToAddInit = new HashSet<>();
                if(terminals.contains(gramSymb)) setToAddInit.add(gramSymb); // if terminal, then first(gramSymb) = {gramSymb}
                firstTable.put(gramSymb, setToAddInit);
            }

            boolean updated = true;

            while(updated) {
                updated = false;

                // update first(nonterm) set
                for (String nonterm : nonterminals) {
                    Set<String> newFirstX = new HashSet<>(firstTable.get(nonterm)); // get current FIRST set calc so far
                    Set<String> bodies = productionsMap.get(nonterm);

                    for (String body : bodies) {
                        // prod X (nonterm) —> Y1 Y2 ... Yk, a in FIRST(nonterm X) if:
                        // for some i, a in FIRST(Yi) & ε in all of FIRST(Y1),..., FIRST(Yi-1);
                        String[] elements = body.split("\\s"); // get the elements (Yi)
                        boolean allElReducedToEmpty = true;

                        for (String element : elements) {
                            Set<String> firstSetForElement = firstTable.get(element);
                            newFirstX.addAll(firstSetForElement);
                            // only continue to next element if this one can be reduced to ε
                            if (!firstSetForElement.contains("ε")) {
                                allElReducedToEmpty = false;
                                break;
                            }
                        }
                        // ε in FIRST(gramSymb) only if all elements can be reduced to ε
                        if (!allElReducedToEmpty && !bodies.contains("ε")) newFirstX.remove("ε");
                    }

                    if(!firstTable.get(nonterm).containsAll(newFirstX)){
                        updated = true;
                        firstTable.replace(nonterm, newFirstX);
                    }
                }
            }
        }

        /**
         * Gets FIRST(gramSymb) from firstTable.
         * @param gramSymb grammar symbol for which to calculate set of terminals
         */
        private Set<String> calcFirstForGramSymb(String gramSymb){
            return new HashSet<>(firstTable.get(gramSymb));
        }

        /**
         * Calculates set of items that begin a string derived from the
         * given string, strToCalc. That is, FIRST(strToCalc)
         * @param strToCalc grammar symbol for which to calculate FIRST set
         */
        private Set<String> calcFirstForString(String strToCalc){
            Set<String> result = new HashSet<>();
            String[] elements = strToCalc.split("\\s"); // get the elements of the str (Xi) (str is X1 X2 ... Xk)

            boolean allElReducedToEmpty = true;

            for(String element : elements){
                Set<String> firstSetForElement = calcFirstForGramSymb(element);
                result.addAll(firstSetForElement);
                // only continue to next element if this one can be reduced to ε
                if(!firstSetForElement.contains("ε")){
                    allElReducedToEmpty = false;
                    break;
                }
            }

            // ε in FIRST(strToCalc) only if all elements can be reduced to ε
            if(!allElReducedToEmpty) result.remove("ε");

            return result;
        }
    }

    interface ParserAction {
        String toString();
    }
    class ShiftAction implements ParserAction{
        Integer stateToShift;
        ShiftAction(Integer stateToShift) {
            this.stateToShift = stateToShift;
        }

        @Override
        public String toString() {
            return "shift " + stateToShift;
        }
    }
    class ReduceAction implements ParserAction{
        Production prodToReduceBy;
        ReduceAction(Production prodToReduceBy){
            this.prodToReduceBy = prodToReduceBy;
        }

        @Override
        public String toString() {
            return "reduce " + prodToReduceBy;
        }
    }
    class AcceptAction implements ParserAction{
        AcceptAction(){}

        @Override
        public String toString() {
            return "accept";
        }
    }
    class ErrorAction implements ParserAction{
        ErrorAction(){}

        @Override
        public String toString() {
            return "error";
        }
    }

    private Grammar grammar;
    private Grammar augmentedGrammar;
    private Map<Integer, Map<String, ParserAction>> SLRactionTable;
    private List<Set<String>> mapIntStToSetOfItems;
    private Integer startState;

    public Parser(File gramSpecification) throws IOException {
        grammar = new Grammar(gramSpecification);

        // create augmented grammar
        Set<String> newNonterm = new HashSet<>();
        newNonterm.add("S'");
        Set<Production> newProd = new HashSet<>();
        newProd.add(new Production("S'", grammar.getStartSymbol()));

        augmentedGrammar = new Grammar(grammar, newNonterm, null, newProd, "S'");
    }

    /**
     * Calculates closure of the given set of items
     *
     * @param itemSet set of items
     * @return set of items forming the closure of the given set of items itemSet
     */
    private Set<String> closure(Set<String> itemSet) {
        Set<String> j = new HashSet<>(itemSet);
        boolean itemAddedFlag = true; // true if an item added to j
        HashMap<String, Boolean> added = new HashMap<>();  // indicates if item (key) has been added to J

        while (itemAddedFlag) {
            itemAddedFlag = false;
            Set<String> newJ = new HashSet<>(j);

            // "for each item A -> α · B β in j"
            for (String item : j) {
                if(item.indexOf('·') == item.length() - 1) continue;  // do not consider items w/dot at end
                String[] splitAtDot = item.split("\\s·\\s");
                String afterDot = splitAtDot[1];
                String element = afterDot.split("\\s")[0]; // get 1st element after dot (bc elements in items & prod separated by space)

                // if it's ε, (i.e. A -> · ε), then add A -> ε · to closure set (newJ)
                if(element.equals("ε")) newJ.add(splitAtDot[0] + " ε ·");

                // check it's a non-term
                if (!grammar.getNonterminals().contains(element)) continue;
                Set<String> bodies = grammar.getProductionsMap().get(element);  // get all prod bodies for nonterm after dot in item

                // "for each prod B -> γ of grammar"
                if (!added.getOrDefault(element, false)) {  // if B -> · γ not in j

                    for (String body : bodies) {
                        String itemToAdd = element + " -> · " + body;  // item B -> · γ
                        newJ.add(itemToAdd);
                    }
                    added.put(element, true);
                    itemAddedFlag = true;
                }
            }
            j = newJ;
        }
        return j;
    }

    /**
     * Calculates & returns the value of GOTO(itemSet, gramSymb).
     *
     * @param itemSet set of items
     * @return set of items given by GOTO(itemSet, gramSymb)
     */
    private Set<String> calcGoto(Set<String> itemSet, String gramSymb){
        Set<String> result = new HashSet<>();

        // "for each item A -> α · gramSymb β in itemSet"
        for(String item : itemSet){
            if(item.indexOf('·') == item.length() - 1) continue;  // do not consider items w/dot at end


            String[] splitAtDot = item.split("\\s·\\s");
            String[] elementsAfterDot = splitAtDot[1].split("\\s");
            if(!gramSymb.equals(elementsAfterDot[0])) continue; // check if item has · directly to the left of gramSymb

            // get string (β) that appears after the gramSymb in the item
            StringBuilder betaStr = new StringBuilder();
            for(int i = 1; i < elementsAfterDot.length; i++){
                betaStr.append(" ").append(elementsAfterDot[i]);
            }

            // add item A -> α gramSymb · β to set
            Set<String> itemSetForClosure = new HashSet<>();
            itemSetForClosure.add(splitAtDot[0] + " " + gramSymb + " ·" + betaStr.toString());
            result.addAll(closure(itemSetForClosure)); // calc closure of set of item A -> α gramSymb · β
        }

        return result;
    }

    /**
     * Calculates & returns the value of GOTO(state, gramSymb).
     *
     * @param state state of SLR parser automaton (integer, it repr a set of items)
     * @return state which is given by GOTO(state, gramSymb)
     */
    private Integer calcSLRGoto(Integer state, String gramSymb){
        return mapIntStToSetOfItems.indexOf(calcGoto(mapIntStToSetOfItems.get(state), gramSymb));
    }

    /**
     * Calculate & return the canonical collection of sets of LR(0) items for the augmented
     * grammar G'
     * @return canonical collection of items
     */
    private Set<Set<String>> calcCanonicalCollection(){
        // init c (canonical collection)
        Set<String> initISet = new HashSet<>();
        initISet.add(augmentedGrammar.getStartSymbol() + " -> · " + grammar.getStartSymbol());
        Set<Set<String>> c = new HashSet<>();
        c.add(closure(initISet));

        // build c
        boolean added = true;
        while(added){
            added = false;
            Set<Set<String>> newC = new HashSet<>(c);
            for(Set<String> itemSet : c){
                for(String gramSymb : grammar.getGramSymbols()){
                    Set<String> gotoResult = calcGoto(itemSet, gramSymb);

                    if(gotoResult.isEmpty() || c.contains(gotoResult)) continue;  // goto is empty or goto in c
                    newC.add(gotoResult);
                    added = true;
                }
            }
            c = newC;
        }

        return c;
    }

    /**
     * Construct the action & goto tables for the SLR parsing table.
     * (stored as fields)
     */
    void constructSLRparsingTable(){
        Set<Set<String>> canonCollection = calcCanonicalCollection();
        mapIntStToSetOfItems = new ArrayList<>(canonCollection); // init map of int states to sets of items
        SLRactionTable = new HashMap<>();  // init tables

        // build states & determine their parsing actions
        // for itemSet Ii in canonCollection C = {I0, I1, ..., In}
        for(Set<String> itemSet : canonCollection){
            int i = mapIntStToSetOfItems.indexOf(itemSet);

            Map<String, ParserAction> thisSetSLRActionTableEntry = new HashMap<>();  // init action table entry for current state

            for(String item : itemSet){
                String[] elements = item.split("\\s");

                // case c) [S' -> startSymb] in Ii, then ACTION[i, $] = "accept"
                if(item.equals(augmentedGrammar.getStartSymbol() + " -> " + grammar.getStartSymbol() + " ·")){
                    thisSetSLRActionTableEntry.put("INPUTENDMARKER", new AcceptAction());
                }else {
                    int dotIndex = Arrays.asList(elements).indexOf("·");

                    // case b) [A -> α ·] is in Ii
                    if (dotIndex == elements.length - 1) {
                        // ACTION[i, a] = "reduce A -> α" for all a in FOLLOW(A)
                        // find string alpha (using StringBuilder...) (i.e. item is [A -> α · ])
                        StringBuilder alpha = new StringBuilder();
                        for (int n = 2; n < elements.length - 1; n++) {
                            if (n != elements.length - 2) alpha.append(elements[n]).append(" ");
                            else alpha.append(elements[n]);
                        }

                        // set the reduce actions
                        for (String term : augmentedGrammar.calcFollow(elements[0])) {
                            thisSetSLRActionTableEntry.put(term, new ReduceAction(new Production(elements[0], alpha.toString())));
                        }
                    }else{ // case a) [A -> α · a β] is in Ii and GOTO(Ii, a) = Ij (a must be a terminal)
                        // ACTION[i, a] = "shift j"
                        String terminalA = elements[dotIndex + 1];
                        if(!grammar.getTerminals().contains(terminalA)) continue;  // a must be a terminal

                        Integer j = calcSLRGoto(i, terminalA);
                        if(j >= 0 && j <= mapIntStToSetOfItems.size() - 1) thisSetSLRActionTableEntry.put(terminalA, new ShiftAction(j));
                    }
                }
            }
            SLRactionTable.put(i, thisSetSLRActionTableEntry);  // put entry in SLR action table
        }

        // starting state is one constructed from set of items containing [S' -> · StartSymbol]
        Set<String> startStateItemSet = new HashSet<>();
        String itemToSearchFor = augmentedGrammar.getStartSymbol() + " -> · " + grammar.getStartSymbol();
        int i = 0;
        for(; i < mapIntStToSetOfItems.size(); i++){
            Set<String> currentSet = mapIntStToSetOfItems.get(i);
            if(currentSet.contains(itemToSearchFor)) break;
        }
        this.startState = i;
    }

    /**
     * LR parsing program
     * must call constructSLRparsing table before
     */
    void parse(List<Token> inputStr) throws ParsingError {
        Stack<Integer> stack = new Stack<>();  // create parsing stack
        stack.push(startState);  // initially, starting state is on stack
        inputStr.add(new Token<>(TokenName.INPUTENDMARKER, "$"));  // add input endmarker to input str

        Iterator<Token> iterator = inputStr.iterator(); // to iterate through input in seq
        Token nextToken = iterator.next(); // get 1st input symbol

        while(true){
            Integer topState = stack.peek();
            ParserAction action = SLRactionTable.get(topState).getOrDefault(nextToken.getStrName(), new ErrorAction());
            System.out.println("stack: "+stack);
            System.out.println("nextToken: "+nextToken.getStrName());
            System.out.println("currentstate: "+ topState + "   <=>   " + mapIntStToSetOfItems.get(topState));
            System.out.println("action[currentSt, nextToken]= "+action); // TODO
            System.out.println("__________________");

            if(action instanceof ShiftAction){
                stack.push(((ShiftAction) action).stateToShift);
                nextToken = iterator.next();

            }else if(action instanceof ReduceAction){
                Production prod = ((ReduceAction) action).prodToReduceBy;
                if(!prod.getBody().equals("ε")) {
                    for (int i = 0; i < prod.getBody().split(" ").length; i++) {
                        stack.pop();
                    }
                    stack.push(calcSLRGoto(stack.peek(), prod.getHead()));
                }else stack.push(calcSLRGoto(topState, prod.getHead()));
                System.out.println(prod);

            }else if(action instanceof AcceptAction) break;

            else throw new ParsingError(stack);
        }
    }
}
