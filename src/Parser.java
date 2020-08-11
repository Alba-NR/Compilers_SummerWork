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

    public Parser(File gramSpecification) throws IOException {
        this.grammar = new Grammar(gramSpecification);
    }

    private Set<String> closure(Set<String> itemArg) {
        Set<String> j = new HashSet<>(itemArg);
        boolean itemAddedFlag = true; // true if an item added to j
        HashMap<String, Boolean> added = new HashMap<>();

        while (itemAddedFlag) {
            itemAddedFlag = false;

            for (String item : j) {
                String afterDot = item.split("·")[1];
                String element = afterDot.split("\\s")[0]; // get 1st element after dot (bc elements in items & prod separated by space)
                // check it's a non-term
                if (!grammar.getNonterminals().contains(element)){
                    continue;
                }
                Set<String> bodies = grammar.getProductionsMap().get(element);  // get all prod bodies for nonterm after dot in item

                for (String body : bodies) {
                    String itemToAdd = element + " -> · " + body;
                    if (!added.get(element)) {
                        j.add(itemToAdd);
                        added.put(element, true);
                        itemAddedFlag = true;
                    }
                }
            }
        }
        return j;
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
