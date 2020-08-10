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
        private Set<Production> productions; // note: dif prod bodies for same nonterm stored as separate elements in set
        private Set<String> nonterminals;
        private Set<String> terminals;

        /**
         * Note: the grammarSpecification file must have the following format:
         *  - 1st line: non-terminals, separated by a comma
         *  - 2nd line: terminals, separated by a comma
         *  - then, one production per line
         *  - preferably a single production per non-terminal for clarity
         */
        Grammar(File grammarSpecification) throws IOException {
            productions = new HashSet<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(grammarSpecification))) {
                try {
                    String nextLine = reader.readLine();  // read next line from file
                    nonterminals = new HashSet<>(Arrays.asList(nextLine.split("\\s,\\s")));  // 1st line has non-terminals
                    nextLine = reader.readLine();
                    terminals = new HashSet<>(Arrays.asList(nextLine.split("\\s,\\s")));  // 2nd line has terminals
                    nextLine = reader.readLine();

                    while (nextLine != null) {
                        // get head & prod bodies for the prod in this line
                        String[] splitHeadBody = nextLine.split("\\s->\\s");
                        String[] bodyProductions = splitHeadBody[1].split("\\s\\|\\s");

                        for (String body : bodyProductions) {  // create set of prod to return
                            Production prod = new Production(splitHeadBody[0], body);
                            productions.add(prod);
                        }

                        nextLine = reader.readLine();  // read next line
                    }

                } catch (NullPointerException e) {
                    System.out.println("Incorrect format of input file.");
                }

            } catch (IOException e) {
                throw new IOException("Can't access file " + grammarSpecification, e);
            }
        }

        Set<Production> getProductions() {
            return new HashSet<>(productions);
        }

        Set<String> getNonterminals() {
            return new HashSet<>(nonterminals);
        }

        Set<String> getTerminals() {
            return new HashSet<>(terminals);
        }


    }

    private Grammar grammar;

    public Parser(File gramSpecification) throws IOException {
        this.grammar = new Grammar(gramSpecification);
    }

    private Set<String> closure(Set<String> itemArg) {
        Set<String> j = itemArg;
        boolean itemAddedFlag = true; // true if an item added to j

        while (itemAddedFlag) {
            itemAddedFlag = false;
            for (String item : j) {
                for (Production prod : grammar.productions) {
                    String itemToAdd = prod.head + " -> · " + prod.body;
                    if (!j.contains(itemToAdd)) {
                        j.add(itemToAdd);
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
