package cs224n.assignment;

import cs224n.ling.*;
import cs224n.util.Counter;
import cs224n.util.Pair;
import cs224n.util.Triplet;

import java.util.*;
import cs224n.assignment.Grammar.*;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
	private Map<Triplet<Integer, Integer, String>, Pair<backTraceData, backTraceData>> back;
    private ArrayList<ArrayList<Counter<String>>> scoreMap;
    private Tree<String> parseTree;
    private Integer numWords;
    
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

    /*
     * Our Main method where we get the best parse
     */
    public Tree<String> getBestParse(List<String> sentence) {
    	System.out.println(sentence);
    	numWords = sentence.size();
    	// Initialization
    	initializeScoreMap();
    	//This is stage 1 of our dynamic programming, the "base"
    	back = new HashMap<Triplet<Integer, Integer, String>, Pair<backTraceData, backTraceData>>();
    	for (int i = 0; i < sentence.size(); i++) {
    		for (String nonterm : lexicon.getAllTags()) {
    			double d = lexicon.scoreTagging(sentence.get(i), nonterm);
    			scoreMap.get(i).get(i + 1).setCount(nonterm, d);//WHY do we need to split this up??
    		}
    		HandleUnaries(i, i+1);
    	}
    	
    	//Now we have added the first "level" (0,0), (1,1), (2,2)... of our dynamic pyramid
    	//Time to move on up...
    	for(int span = 1; span < numWords; span++){
    		for(int begin = 0; begin < numWords - span; begin++){
    			int end = begin + span; //double check indicies on these
    			for (int split = begin+1; split < end-1; split++){
    				for(String A : lexicon.getAllTags()){//(should we make this an iVar (lex.getAllTags()?)
    					//Get binary rules for String A (why left child)
    					//Do i need to repeat this for right child as well??
    					//I only have one A, just one child...
    					List<BinaryRule> rules = grammar.getBinaryRulesByLeftChild(A);
    					for(BinaryRule rule: rules){
    						String B = rule.getLeftChild();
    						String C = rule.getRightChild();
    						double probability = getScore(begin, split, B)*getScore(split, end, C)*rule.getScore();
    						if(probability > getScore(begin, end, A)){
    							setScore(begin, end, A, probability);
    							addToBackMap(begin, end, split, A, B, C);
    						}//End of if probability > loop
    					}//End of going through all rules (B,C)
    				}//End of going though all nonterminals (A)
    			}//End of splits
    			HandleUnaries(begin, end);
    		}
    	}
    	printScoreMap();
    	printBackMap();
        //return buildTree();
    	return null;
    }//end of function
    
    
    //Handles unaries according to slide psuedocode (pg 39, 40)
    //https://class.stanford.edu/c4x/Engineering/CS-224N/asset/SLoSP-2012-2.pdf
    private void HandleUnaries(int begin, int end){
    	boolean added = true;
		while (added) {
			added = false;
			for (String A : lexicon.getAllTags()){//Is this really the right way to get all nonterms??
				List<UnaryRule> rules = grammar.getUnaryRulesByChild(A);
				for (UnaryRule rule: rules){//*TODO I'm getting rules by child then rules.parent
					String B = rule.getParent(); //NP
					//NP -> N (parent -> child)
					//A is the child
					//B is the parent
					double probability = rule.score*getScore(begin, end, A);//It's okay if = 0
					if(probability > getScore(begin, end, B)){
						setScore(begin, end, B, probability);
						addToBackMap(begin, end, 0, A, B, null);//**Look at how this method is built
						added = true;
					}//End of if statement
				}//for unary rules end
			}//for String A (nonterms)
		}//End of while loop (unary conditions
    }//End of function
    
    
    //Initializes our score map given the size(of sentence)
    private void initializeScoreMap(){
	   	scoreMap = new ArrayList<ArrayList<Counter<String>>>();
	   	for (int i = 0; i < numWords + 1; i ++) {
	   		scoreMap.add(new ArrayList<Counter<String>> ());
	   		for (int j = 0; j < numWords + 1; j++) {
	   			scoreMap.get(i).add(new Counter<String>());
	   		}
	   	}
    }
    
    
    //Create the new datastructures necessary for our backtrace map, then add it
    private void addToBackMap(int start, int end, int split, String A, String B, String C){
    	Triplet<Integer, Integer, String> triple = new Triplet<Integer, Integer, String>(start, end, A);
    	backTraceData left;
    	backTraceData right;
    	if(C != null){//This is the binary condition
	    	left = new backTraceData(B, start, split);
	    	right = new backTraceData(C, split, end);
    	}else{//This is the unary condition (for our first level)
    		left = new backTraceData(B, start, end);
    		right = null;
    	}
    	Pair<backTraceData, backTraceData> pair = new Pair<backTraceData, backTraceData>(left, right);

    	back.put(triple, pair);
    	
    }
    
    //This builds the tree recursively by calling the recursive funtion
    //Dont know exactly how the tree is structured yet...
    private Tree<String> buildTree(){
    	Tree<String> parseTree = new Tree<String>("ROOT");
    	
    	double bestScore = 0;
    	Set<String> bestScoreKeys = scoreMap.get(numWords).get(numWords).keySet();
    	for(String key : bestScoreKeys){
    		double tempScore = getScore(numWords, numWords, key);
    		if(tempScore > bestScore)
    			bestScore = tempScore;
    	}
    	
    	String initialLabel = getInitialLabel(bestScore);
    	recursivelyBuildTree(initialLabel, 0, numWords);
    	
    	return parseTree;
    }
    
    
    //Given the first label, we want to build the rest of our tree
    private void recursivelyBuildTree(String initialLabel, int start, int end){
    	//base case
    	if(end-start == 1){
    		//This means I'm only 1 away, get ready to finish...
    	}
    	//recursion
    	Triplet<Integer, Integer, String> curTriple = new Triplet<Integer, Integer, String>(start, end, initialLabel);

    	Pair<backTraceData, backTraceData> curPair = back.get(curTriple);
    	//Add to the tree, cur pair
    	backTraceData leftSide = curPair.getFirst();
    	backTraceData rightSide = curPair.getSecond();
    	//How do I add to tree.... pass in the label to tree
    	//then recurse branch left with (start, end, label), right with (start, end, label)
    	String leftLabel = leftSide.label;
    	String rightLabel = rightSide.label;
    }
    
    //A simple class to store our data for backtraces
    static class backTraceData{
    	String label;
    	int leftFence;
    	int rightFence;
    	
    	public backTraceData(String label, int leftFence, int rightFence){
    		this.label = label;
    		this.leftFence = leftFence;
    		this.rightFence = rightFence;
    	}
    }
    
    //Gets the starting label for our backtrace
    private String getInitialLabel(double bestScore){
    	Set<String> finalLabels =  scoreMap.get(0).get(numWords).keySet();
    	for(String label : finalLabels){
    		if(getScore(0, numWords, label) == bestScore)
    			return label;
    	}
    	System.out.println("Error 1, we should not reach here");
    	return null;
    }
    
    //helper function to return the score, returns 0 if there is no count (via getCount in counter)
    private double getScore(int row, int col, String label){
    	return scoreMap.get(row).get(col).getCount(label);
    }
    
    //helper function to set the score in our map
    private void setScore(int row, int col, String label, double score){
    	scoreMap.get(row).get(col).setCount(label, score);
    }
    
    private void printScoreMap() {
    	int i = 0;
    	for (ArrayList<Counter<String>> a : scoreMap) {
    		int j = 0;
    		for (Counter<String> c : a) {
    			System.out.println(i + " " + j + " " + c);
    			j++;
    		}
    		i++;
    	}
    }
    
    private void printBackMap() {
    	//System.out.println(back);
    	for (Triplet<Integer, Integer, String> key : back.keySet()) {
    		Pair<backTraceData, backTraceData> value = back.get(key);
    		System.out.println(key + " " + value);
    	}
    }
    
}
