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

    // using String to repr items atm
    /*class Item{
        Class representing an item for the grammar
        String head;
        String body;
        int dotPosInBody;

        Item(Production prod, int dotPos){
            this.head = prod.getHead();
            this.body = prod.getBody();
            dotPosInBody = dotPos;
        }
    }
    */

    class Grammar {
        private Set<Production> productionsSet; // note: dif prod bodies for same nonterm stored as separate elements in set
        private Map<String, Set<String>> productionsMap;  // stores prod bodies assoc w/each nonterm (--head)
        private Set<String> nonterminals;
        private Set<String> terminals;
        private String startSymbol;

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
    }

    private Grammar grammar;
    private Map<Set<String>, Map<String, Set<String>>> gotoTable;

    public Parser(File gramSpecification) throws IOException {
        this.grammar = new Grammar(gramSpecification);
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
    private Set<String> calcGoto(Set<String> itemSet, String gramSymb){
        // check if already calc & stored in gotoTable
        if(gotoTable.get(itemSet).containsKey(gramSymb)) return gotoTable.get(itemSet).get(gramSymb);


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
        gotoTable.get(itemSet).put(gramSymb, result);  // store result in gotoTable

        return result;
    }

    private Set<String> calcCanonicalCollection(){
        Set<String> newNonterm = new HashSet<>();
        newNonterm.add("S'");
        Set<Production> newProd = new HashSet<>();
        newProd.add(new Production("S'", "S"));

        Grammar augmGram = new Grammar(grammar, newNonterm, null, newProd, "S'");

        return new HashSet<>(); // just to compile, must change
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
