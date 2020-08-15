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
        private Map<String, Set<String>> followTable = new HashMap<String, Set<String>>(); // stores follow(A) for non-term A (note: lazy calc)

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

                    // put input right endmarker into follow(start symbol)
                    Set<String> followStartSymb = new HashSet<>();
                    followStartSymb.add("$");
                    followTable.put(startSymbol, followStartSymb);

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

            // put input right endmarker into follow(start symbol)
            Set<String> followStartSymb = new HashSet<>();
            followStartSymb.add("$");
            followTable.put(startSymbol, followStartSymb);
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
         * Calculates the set of terminals that can appear immediately to the right of
         * nonterm (the param) in some sentential form.
         * @param nonterm nonterminal for which to calculate set of terminals
         */
        Set<String> calcFollow(String nonterm){
            // check if already calc & stored in followTable
            if(followTable.containsKey(nonterm)) return followTable.get(nonterm);
            // else calc it
            Set<String> followSet = new HashSet<>();

            for(Production prod : productionsSet){
                List<String> elementsInBody = Arrays.asList(prod.getBody().split("\\s"));
                // only check productions which contain nonterm in their body & those which don't have it as the last element of body
                if (!elementsInBody.contains(nonterm) || nonterm.equals(elementsInBody.get(elementsInBody.size() - 1))) continue;

                // construct string of elements after nonterm in the body of this prod
                StringBuilder strAfterElement = new StringBuilder();
                int start = elementsInBody.indexOf(nonterm) + 1;
                for(int i = start ; i < elementsInBody.size(); i++){
                    if(i == start) strAfterElement.append(elementsInBody.get(i));
                    else strAfterElement.append(" ").append(elementsInBody.get(i));
                }

                // case/step 3) in follow calc algorithm
                if(strAfterElement.toString().equals("") || calcFirstForString(strAfterElement.toString()).contains("ε")){
                    followSet.addAll(
                            calcFollow(prod.getHead())
                    );

                // case/step 2) in follow calc algorithm
                }else {
                    followSet.addAll(calcFirstForString(strAfterElement.toString()));
                }
            }
            return followSet;
        }

        /**
         * Calculates set of items that begin a string derived from the
         * given grammar symbol, gramSymb. That is, FIRST(gramSymb)
         * @param gramSymb grammar symbol for which to calculate FIRST set
         */
        private Set<String> calcFirstForGramSymb(String gramSymb){
            Set<String> result = new HashSet<>();
            if(gramSymb.equals("ε")) result.add("ε");
            else if(terminals.contains(gramSymb)) result.add(gramSymb); // if terminal, then first(gramSymb) = {gramSymb}
            else{
                // calc first(gramSymb) set for gramSymb a non-terminal
                Set<String> bodies = productionsMap.get(gramSymb);

                for(String body : bodies){
                    // prod gramSymb —> Y1 Y2 ... Yk, a in FIRST(gramSymb) if:
                    // for some i, a in FIRST(Yi) & ε in all of FIRST(Y1),..., FIRST(Yi-1);

                    String[] elements = body.split("\\s"); // get the elements (Yi)
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

                    // ε in FIRST(gramSymb) only if all elements can be reduced to ε
                    if(!allElReducedToEmpty) result.remove("ε");
                }
            }
            return result;
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
    private Map<Set<String>, Map<String, Set<String>>> gotoTable = new HashMap<>();
    private Map<Integer, Map<String, Integer>> SLRgotoTable;
    private Map<Integer, Map<String, ParserAction>> SLRactionTable;
    private List<Set<String>> mapIntStToSetOfItems;
    private Integer startState;

    public Parser(File gramSpecification) throws IOException {
        grammar = new Grammar(gramSpecification);

        // create augmented grammar
        Set<String> newNonterm = new HashSet<>();
        newNonterm.add("S'");
        Set<Production> newProd = new HashSet<>();
        newProd.add(new Production("S'", "S"));

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
                String afterDot = item.split("\\s·\\s")[1];
                String element = afterDot.split("\\s")[0]; // get 1st element after dot (bc elements in items & prod separated by space)

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
     * Gets value from gotoTable if previously calculated; otherwise, calculates it & stores it too.
     *
     * @param itemSet set of items
     * @return set of items forming the closure of the given set of items itemSet
     */
    private Set<String> calcGoto(Set<String> itemSet, String gramSymb){

        // check if already calc & stored in gotoTable
        if(gotoTable.containsKey(itemSet)) if (gotoTable.get(itemSet).containsKey(gramSymb)) return gotoTable.get(itemSet).get(gramSymb);

        // if not in gotoTable, calculate it
        Map<String, Set<String>> newEntry = new HashMap<>(); // in case no entry for this itemSet is yet present in table

        Set<String> itemSetForClosure = new HashSet<>();

        // "for each item A -> α · gramSymb β in itemSet"
        for(String item : itemSet){

            // check if item has · directly to the left of gramSymb
            if(item.indexOf('·') == item.length() - 1) continue;  // do not consider items w/dot at end
            String[] splitAtDot = item.split("\\s·\\s");
            String[] elementsAfterDot = splitAtDot[1].split("\\s");
            if(!gramSymb.equals(elementsAfterDot[0])) continue;

            // get string (β) that appears after the gramSymb in the item
            StringBuilder betaStr = new StringBuilder();
            for(int i = 1; i < elementsAfterDot.length; i++){
                betaStr.append(" ").append(elementsAfterDot[i]);
            }

            // add item A -> α gramSymb · β to set
            itemSetForClosure.add(splitAtDot[0] + " " + gramSymb + " ·" + betaStr.toString());
        }
        // calc closure of set of all items A -> α gramSymb · β
        Set<String> result = closure(itemSetForClosure);
        if(gotoTable.containsKey(itemSet)) gotoTable.get(itemSet).put(gramSymb, result);  // store result in gotoTable
        else{
            newEntry.put(gramSymb, result);
            gotoTable.put(itemSet, newEntry);
        }

        return result;
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
        mapIntStToSetOfItems = new ArrayList<>(canonCollection.size()); // map of int states to corresponding set of items
        SLRactionTable = new HashMap<>();  // init tables
        SLRgotoTable = new HashMap<>();

        // init map int to item sets
        mapIntStToSetOfItems.addAll(canonCollection);

        // build states & determine their parsing actions
        for(Set<String> itemSet : canonCollection){
            Map<String, ParserAction> thisSetSLRActionTableEntry = new HashMap<>();  // init action table entry for current state

            for(String item : itemSet){
                String[] elements = item.split("\\s");

                // case c)
                if(item.equals(augmentedGrammar.getStartSymbol() + " -> " + grammar.getStartSymbol() + " ·")){
                    thisSetSLRActionTableEntry.put("$", new AcceptAction());
                }
                // case b)
                else if(elements[elements.length - 1].equals("·")){
                    // find string alpha (using StringBuilder...) (i.e. item is [A -> α · ])
                    StringBuilder alpha = new StringBuilder();
                    for(int i = 2; i < elements.length - 1; i++){
                        if(i != elements.length - 2) alpha.append(elements[i]).append(" ");
                        else alpha.append(elements[i]);
                    }

                    // set the reduce actions
                    for(String term : augmentedGrammar.calcFollow(elements[0])){
                        thisSetSLRActionTableEntry.put(term, new ReduceAction(new Production(elements[0], alpha.toString())));
                    }
                }
                else{
                    // case a)
                    // shift actions
                    String terminalA = elements[Arrays.asList(elements).indexOf("·") + 1];
                    Set<String> setItemsJ = calcGoto(itemSet, terminalA);

                    if(canonCollection.contains(setItemsJ)) thisSetSLRActionTableEntry.put(terminalA, new ShiftAction(mapIntStToSetOfItems.indexOf(setItemsJ)));
                }
            }
            SLRactionTable.put(mapIntStToSetOfItems.indexOf(itemSet), thisSetSLRActionTableEntry);  // put entry in SLR action table
        }

        // construct SLRgotoTable from existent entries in gotoTable
        for(Map.Entry<Set<String>, Map<String, Set<String>>> entryOuter : gotoTable.entrySet()){
            Map<String, Integer> newValueOuter = new HashMap<>();
            for(Map.Entry<String, Set<String>> entryInner : entryOuter.getValue().entrySet()){
                newValueOuter.put(entryInner.getKey(), mapIntStToSetOfItems.indexOf(entryInner.getValue()));
            }
            SLRgotoTable.put(mapIntStToSetOfItems.indexOf(entryOuter.getKey()), newValueOuter);
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

    private Integer calcSLRGoto(Integer state, String gramSymb){

        // check if already calc & stored in SLRgotoTable
        if(SLRgotoTable.containsKey(state)) if (SLRgotoTable.get(state).containsKey(gramSymb)) return SLRgotoTable.get(state).get(gramSymb);

        // if not in SLRgotoTable, calculate it by calling calcGoto
        Map<String, Integer> newEntry = new HashMap<>(); // in case no entry for this itemSet is yet present in table

        Integer result = mapIntStToSetOfItems.indexOf(calcGoto(mapIntStToSetOfItems.get(state), gramSymb));

        if(SLRgotoTable.containsKey(state)) SLRgotoTable.get(state).put(gramSymb, result);  // store result in gotoTable
        else{
            newEntry.put(gramSymb, result);
            SLRgotoTable.put(state, newEntry);
        }

        return result;
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

        System.out.println("action table: " + SLRactionTable); // TODO remove
        while(true){
            Integer topState = stack.peek();
            System.out.println("action table entry for topState: "+SLRactionTable.get(topState)); // TODO remove
            System.out.println(nextToken.getStrName());
            ParserAction action = SLRactionTable.get(topState).getOrDefault(nextToken.getStrName(), new ErrorAction());  // NOT SURE -- MUST MODIFY

            if(action instanceof ShiftAction){
                stack.push(((ShiftAction) action).stateToShift);
                nextToken = iterator.next();

            }else if(action instanceof ReduceAction){
                Production prod = ((ReduceAction) action).prodToReduceBy;
                for(int i = 0; i < prod.getBody().length(); i++){
                    stack.pop();
                }
                stack.push(mapIntStToSetOfItems.indexOf(calcSLRGoto(stack.peek(), prod.getHead())));

            }else if(action instanceof AcceptAction) break;
            else throw new ParsingError(stack);
        }
    }
}
