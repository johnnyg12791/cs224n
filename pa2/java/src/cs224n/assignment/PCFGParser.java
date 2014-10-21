package cs224n.assignment;

import cs224n.ling.*;
import cs224n.util.Counter;
import cs224n.util.Pair;
import cs224n.util.Triplet;

import java.util.*;
import cs224n.assignment.*;
import cs224n.assignment.Grammar.UnaryRule;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
	private Map<Triplet<Integer, Integer, String>, Pair<Map<String, Integer[]>, Map<String, Integer[]> >> back;


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
    	back = new HashMap<Triplet<Integer, Integer, String>, Pair<Map<String, Integer[]>, Map<String, Integer[]> >>();
    	for (int i = 0; i < sentence.size(); i++) {
    		for (String nonterm : lexicon.getAllTags()) {
    			double d = lexicon.scoreTagging(sentence.get(i), nonterm);
    			score.get(i).get(i + 1).setCount(nonterm, d);//WHY do we need to split this up??
    		}
    		boolean added = true;
    		while (added) {
    			added = false;
    			for (String A : lexicon.getAllTags()){//NP a nonterm??
    				List<UnaryRule> rules = grammar.getUnaryRulesByChild(A);
    				for (UnaryRule rule: rules){
    					String B = rule.parent;
    					double probability = rule.score*score.get(i).get(i+1).getCount(B);//It's okay if = 0
    					if(probability > score.get(i).get(i+1).getCount(A)){
    						score.get(i).get(i+1).setCount(A, probability);
    						addToBackMap(i, A, B);
    						added = true;
    					}
    				}
    			}
    		}
    	}
    	
        return null;
    }
    
    //Create the new datastructures necessary for our backtrace map, then add it
    private void addToBackMap(int i, String A, String B){
    	Triplet<Integer, Integer, String> triple = new Triplet<Integer, Integer, String>(i, i+1, A);
    	Map<String, Integer[]> AMap = new HashMap<String, Integer[]>();
    	AMap.put(A, new Integer[]{i, i+1});
    	Pair<Map<String, Integer[]>, Map<String, Integer[]>> pair = new Pair<Map<String, Integer[]>, Map<String, Integer[]>>(AMap, null);

    	back.put(triple, pair);
    	
    }
}
