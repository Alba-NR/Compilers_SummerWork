import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
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
                    String[] nontermSplit = nextLine.split("\\s,\\s");
                    nonterminals = new HashSet<>(Arrays.asList(nontermSplit));  // 1st line has non-terminals
                    startSymbol = nontermSplit[0]; // 1st non-term is the start symbol
                    nextLine = reader.readLine();
                    terminals = new HashSet<>(Arrays.asList(nextLine.split("\\s,\\s")));  // 2nd line has terminals
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
        public String getStartSymbol() {
            return startSymbol;
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
                // only check productions which contain nonterm in their body
                if (!elementsInBody.contains(nonterm)) continue;

                // get string of elements after nonterm in the body of this prod
                StringBuilder strAfterElement = new StringBuilder();
                for(String s : elementsInBody.subList(elementsInBody.indexOf(nonterm), elementsInBody.size()-1)){
                    strAfterElement.append(s);
                }
                // case/step 1) in follow calc algorithm
                followSet.addAll(calcFirstForString(strAfterElement.toString()));
                // case/step 2) in follow calc algorithm
                if(strAfterElement.toString().equals("") || calcFirstForString(strAfterElement.toString()).contains("ε")){
                    followSet.addAll(
                            calcFollow(prod.getHead())
                    );
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
    class Shift implements ParserAction{
        Integer stateToShift;
        Shift(Integer stateToShift) {
            this.stateToShift = stateToShift;
        }

        @Override
        public String toString() {
            return "shift " + stateToShift;
        }
    }
    class Reduce implements ParserAction{
        Production prodToReduceBy;
        Reduce(Production prodToReduceBy){
            this.prodToReduceBy = prodToReduceBy;
        }

        @Override
        public String toString() {
            return "reduce " + prodToReduceBy;
        }
    }
    class Accept implements ParserAction{
        Accept(){}

        @Override
        public String toString() {
            return "accept";
        }
    }
    class Error implements ParserAction{
        Error(){}

        @Override
        public String toString() {
            return "error";
        }
    }

    private Grammar grammar;
    private Grammar augmentedGrammar;
    private Map<Set<String>, Map<String, Set<String>>> gotoTable = new HashMap<Set<String>, Map<String, Set<String>>>();
    private Map<Set<String>, Map<String, Set<String>>> SLRgotoTable;
    private Map<Integer, Map<String, ParserAction>> SLRactionTable;
    private List<Set<String>> mapIntStToSetOfItems;

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

            // "for each item A -> α · B β in j"
            for (String item : j) {
                String afterDot = item.split("\\s·\\s")[1];
                String element = afterDot.split("\\s")[0]; // get 1st element after dot (bc elements in items & prod separated by space)
                // check it's a non-term
                if (!grammar.getNonterminals().contains(element)) continue;
                Set<String> bodies = grammar.getProductionsMap().get(element);  // get all prod bodies for nonterm after dot in item

                // "for each prod B -> γ of grammar"
                for (String body : bodies) {
                    String itemToAdd = element + " -> · " + body;  // item B -> · γ
                    if (!added.get(element)) {  // if B -> · γ not in j
                        j.add(itemToAdd);
                        added.put(element, true);
                        itemAddedFlag = true;
                    }
                }
            }
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
    private Set<String> calcGoto(Set<String> itemSet, String gramSymb, boolean useSLRtable){
        Map<Set<String>, Map<String, Set<String>>> tableToUse;
        if(useSLRtable) tableToUse = SLRgotoTable;
        else tableToUse = gotoTable;

        // check if already calc & stored in gotoTable
        if(tableToUse.get(itemSet).containsKey(gramSymb)) return tableToUse.get(itemSet).get(gramSymb);


        // if not in gotoTable, calculate it
        Set<String> itemSetForClosure = new HashSet<>();

        // "for each item A -> α · gramSymb β in itemSet"
        for(String item : itemSet){

            // check if item has · directly to the left of gramSymb
            String[] splitAtDot = item.split("\\s·\\s");
            if(!gramSymb.equals(splitAtDot[1].split("\\s")[0])) continue;

            // add item A -> α gramSymb · β to set
            itemSetForClosure.add(splitAtDot[0] + " " + gramSymb + " · " + splitAtDot[1]);
        }
        // calc closure of set of all items A -> α gramSymb · β
        Set<String> result = closure(itemSetForClosure);
        tableToUse.get(itemSet).put(gramSymb, result);  // store result in gotoTable

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
            for(Set<String> itemSet : c){
                for(String gramSymb : grammar.getTerminals()){
                    Set<String> gotoResult = calcGoto(itemSet, gramSymb, false);
                    if(gotoResult == null || c.contains(gotoResult)) continue;  // goto is empty or goto in c
                    c.add(gotoResult);
                    added = true;
                }
            }
        }

        return c;
    }

    /**
     * Construct the action & goto tables for the SLR parsing table.
     * (stored as fields)
     */
    private void constructSLRparsingTable(){
        Set<Set<String>> canonCollection = calcCanonicalCollection();
        mapIntStToSetOfItems = new ArrayList<>(canonCollection.size()); // map of int states to corresponding set of items
        SLRgotoTable = gotoTable; // must check !

        // build states & determine their parsing actions
        for(Set<String> itemSet : canonCollection){
            mapIntStToSetOfItems.add(itemSet); // include this set in the map int (i.e. repr by index) to Set of items (Strings)
            Map<String, ParserAction> thisSetSLRActionTableEntry = new HashMap<>();  // init action table entry for current state

            for(String item : itemSet){
                String[] elements = item.split("\\s");
                // case c)
                if(item.equals(augmentedGrammar.getStartSymbol() + " -> " + grammar.getStartSymbol() + " ·")){
                    thisSetSLRActionTableEntry.put("$", new Accept());
                }
                // case b)
                else if(elements[elements.length - 1].equals("·")){
                    // find string alpha (using StringBuilder...)
                    StringBuilder alpha = new StringBuilder();
                    for(int i = 2; i < elements.length - 1; i++){
                        if(i != elements.length - 2) alpha.append(elements[i]).append(" ");
                        else alpha.append(elements[i]);
                    }
                    // set the reduce actions
                    for(String term : augmentedGrammar.calcFollow(elements[0])){
                        thisSetSLRActionTableEntry.put(term, new Reduce(new Production(elements[0], alpha.toString())));
                    }
                }
                else{
                    // shift actions
                    String terminalA = elements[Arrays.asList(elements).indexOf("·") + 1];
                    Set<String> setItemsJ = gotoTable.get(itemSet).get(terminalA);
                    if(setItemsJ != null) thisSetSLRActionTableEntry.put(terminalA, new Shift(mapIntStToSetOfItems.indexOf(setItemsJ)));
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            File specificationFile = new File("./src/grammar.txt");
            Parser parser = new Parser(specificationFile);

        }catch (IOException e){
            throw new IOException("Can't access given file.", e);
        }
    }
}
