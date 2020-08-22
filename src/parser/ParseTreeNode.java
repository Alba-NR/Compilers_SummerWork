package parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParseTreeNode {
    private String symbol;
    private List<ParseTreeNode> children = new ArrayList<>();

    ParseTreeNode(String symbol, List<ParseTreeNode> childNodes){
        this.symbol = symbol;
        if(childNodes != null) children.addAll(childNodes);
    }

    public String getSymbol() {
        return symbol;
    }

    public List<ParseTreeNode> getChildren() {
        List<ParseTreeNode> copy = new ArrayList<>();
        for(ParseTreeNode node : children){
            ParseTreeNode nodeCopy = new ParseTreeNode(node.getSymbol(), node.getChildren());
            copy.add(nodeCopy);
        }
        return copy;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        getStringVersion(buffer, "", "");
        return buffer.toString();
    }

    /**
     * Build a string representation of the tree rooted at this node.
     * Representation stored/built in buffer. ('horizontal' tree representation is produced)
     * @param buffer {@link StringBuilder} where the string repr of the tree is stored/built
     * @param prefix {@link String} prefix to add to str repr just before this node's symbol
     * @param childrenPrefix {@link String} prefix to add to str repr of this node's children
     */
    private void getStringVersion(StringBuilder buffer, String prefix, String childrenPrefix) {
        // build string version of this node in "buffer"
        buffer.append(prefix)
                .append(symbol)
                .append('\n');

        // iterate through node's children & add their str repr to buffer
        Iterator<ParseTreeNode> iter = children.iterator();

        while(iter.hasNext()) {
            ParseTreeNode nextNode = iter.next();
            if (iter.hasNext()) {
                nextNode.getStringVersion(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
            } else { // last child
                nextNode.getStringVersion(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
            }
        }
    }
}
