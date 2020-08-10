import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Parser {

    class Production{
        /** Class representing a production for the grammar */
        private String head;
        private String body;

        Production(String head, String body){
            this.head = head;
            this.body = body;
        }

        String getHead(){
            return head;
        }
        String getBody(){
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

    class Grammar{
        private Set<Production> productions; // note: dif prod bodies for same nonterm stored as separate elements in set
        private Set<String> nonterminals;
        private Set<String> terminals;

        Grammar(String[] nonterm, String[] term, File prodSpecification) throws IOException {
            nonterminals = new HashSet<>(Arrays.asList(nonterm));
            terminals = new HashSet<>(Arrays.asList(term));
            productions = createProdFromFile(prodSpecification);
        }

        private Set<Production> createProdFromFile(File prodSpecification) throws IOException {
            Set<Production> result = new HashSet<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(prodSpecification))) {
                String nextLine = reader.readLine();  // read next line from file

                while(nextLine != null) {
                    // get head & prod bodies for the prod in this line
                    String[] splitHeadBody = nextLine.split("\\s->\\s");
                    String[] bodyProductions = splitHeadBody[1].split("\\s\\|\\s");

                    for (String body : bodyProductions) {  // create set of prod to return
                        Production prod = new Production(splitHeadBody[0], body);
                        result.add(prod);
                    }

                    nextLine = reader.readLine();  // read next line
                }

            } catch (IOException e) {
                throw new IOException("Can't access file " + prodSpecification, e);
            }

            return result;
        }

        Set<Production> getProductions() {
            return new HashSet<>(productions);
        }
        Set<String> getNonterminals(){
            return new HashSet<>(nonterminals);
        }
        Set<String> getTerminals(){
            return new HashSet<>(terminals);
        }


    }

    private Grammar grammar;

    public Parser() throws IOException {
        File prodFile = new  File("./src/grammar.txt");
        String[] nonterm = "stmt,N,F,I,D,sign,E,S,factor,T".split(",");
        String[] term = "0,1,2,3,4,5,6,7,8,9,.,+,-,*,cos,!".split(",");
        this.grammar =  new Grammar(nonterm, term, prodFile);
    }

    private Set<String> closure(Set<String> itemArg){
        Set<String> j = itemArg;
        boolean itemAddedFlag = true; // true if an item added to j

        while (itemAddedFlag){
            itemAddedFlag = false;
            for(String item : j){
                for(Production prod : grammar.productions){
                    String itemToAdd = prod.head + " -> · " + prod.body;
                    if(!j.contains(itemToAdd)){
                        j.add(itemToAdd);
                        itemAddedFlag = true;
                    }
                }
            }
        }
        return j;
    }
}
