package cs224n.assignment;

import cs224n.ling.*;
import cs224n.util.Counter;
import cs224n.util.Pair;
import cs224n.util.Triplet;

import java.util.*;
import cs224n.assignment.*;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;

    public void train(List<Tree<String>> trainTrees) {
        // TODO: before you generate your grammar, the training trees
        // need to be binarized so that rules are at most binary
    	for (int i = 0; i < trainTrees.size(); i++) {
    		//Trees.PennTreeRenderer.render(trainTrees.get(i));
    		trainTrees.set(i, TreeAnnotations.annotateTree(trainTrees.get(i)));
    		//Trees.PennTreeRenderer.render(trainTrees.get(i));
    	}
        lexicon = new Lexicon(trainTrees);
        grammar = new Grammar(trainTrees);
    }

    public Tree<String> getBestParse(List<String> sentence) {
        // TODO: implement this method
    	System.out.println(sentence);
    	
    	// Initialization
    	ArrayList<ArrayList<Counter<String>>> score = new ArrayList<ArrayList<Counter<String>>>();
    	for (int i = 0; i < sentence.size() + 1; i ++) {
    		score.add(new ArrayList<Counter<String>> ());
    		for (int j = 0; j < sentence.size() + 1; j++) {
    			score.get(i).add(new Counter<String>());
    		}
    	}
    	Map<Triplet<Integer, Integer, String>, Pair<Map<String, Integer[]>, Map<String, Integer[]> >> back;
    	back = new HashMap<Triplet<Integer, Integer, String>, Pair<Map<String, Integer[]>, Map<String, Integer[]> >>();
    	for (int i = 0; i < sentence.size(); i++) {
    		for (String nonterm : lexicon.getAllTags()) {
    			double d = lexicon.scoreTagging(sentence.get(i), nonterm);
    			score.get(i).get(i + 1).setCount(nonterm, d);
    		}
    		boolean added = true;
    		while (added) {
    			added = false;
    		}
    	}
    	
        return null;
    }
}
