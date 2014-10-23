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
    private int numWords;
    private Set<String> lexiconTags;
    private Set<String> allNonTerms;
    
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
        lexiconTags = lexicon.getAllTags();
        //allNonTerms = new HashSet<String>();
    	//allNonTerms.addAll(grammar.unaryRulesByChild.keySet());
    	//allNonTerms.addAll(grammar.binaryRulesByLeftChild.keySet());
    	//allNonTerms.addAll(grammar.binaryRulesByRightChild.keySet());
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
    	for (int i = 0; i < numWords; i++) {
    		for (String terminal : lexiconTags) {
    			double d = lexicon.scoreTagging(sentence.get(i), terminal);
    			//scoreMap.get(i).get(i + 1).setCount(terminal, d);
    			setScore(i, i+1, terminal, d);
    			addToBackMap(i, i+1, -1, terminal, sentence.get(i), null);
    		}
    		long startTime = System.currentTimeMillis();
    		HandleUnaries(i, i+1);
    		long endTime = System.currentTimeMillis();
    		System.out.println("Unaries 1: " + (endTime - startTime) + " milliseconds");    		

    	}
    	
    	//Now we have added the first "level" (0,0), (1,1), (2,2)... of our dynamic pyramid
    	//Time to move on up...
    	for(int span = 2; span <= numWords; span++){
    		for(int begin = 0; begin <= numWords - span; begin++){
    			int end = begin + span; //double check indicies on these
    			for (int split = begin+1; split <= end-1; split++){
    				Set<String> leftLabels = scoreMap.get(begin).get(split).keySet();
    				for (String B : leftLabels){
    					double BScore = getScore(begin, split, B);
    					List<BinaryRule> leftRules = grammar.getBinaryRulesByLeftChild(B);
    					//System.out.println("B: " + B + " lefts: " + leftRules);
    					for (BinaryRule rule : leftRules) {
    						// Left is just the name of a binary rule
    						double ruleScore = rule.getScore();
    						double parentScore = getScore(begin, end, rule.parent);
    						Set<String> rightLabels = scoreMap.get(split).get(end).keySet();
    						for (String C : rightLabels) {
    							if (rule.rightChild.equals(C)) {
    								// Update the scoreMap
    								double probability = BScore*getScore(split, end, C)*ruleScore;
    	    						if(probability > parentScore){
    	    							setScore(begin, end, rule.parent, probability);
    	    							addToBackMap(begin, end, split, rule.parent, B, C);
    	    						}//End of if probability > loop
    							}
    						}
    					}
    				}
    			}//End of splits
        		long startTime = System.currentTimeMillis();
    			HandleUnaries(begin, end);
        		long endTime = System.currentTimeMillis();
        		System.out.println("Unaries 2 " + (endTime - startTime) + " milliseconds");   
    		}
    	}
    	printScoreMap();
    	printBackMap();
        return buildTree();
    	//return null;
    }//end of function
    
    
    //Handles unaries according to slide psuedocode (pg 39, 40)
    //https://class.stanford.edu/c4x/Engineering/CS-224N/asset/SLoSP-2012-2.pdf
    private void HandleUnaries(int begin, int end){
    	boolean added = true;
    	Set<String> nonTerms = new HashSet<String>(grammar.unaryRulesByChild.keySet());
		while (added) {
			added = false;
			Set<String> leftSides = scoreMap.get(begin).get(end).keySet();
			Set<String> newNonTerms = new HashSet<String>();
			//System.out.println(nonTerms.size()); Look how the size of the nonTerms gets smaller

			for (String A : nonTerms){
				List<UnaryRule> rules = grammar.getUnaryRulesByChild(A);
				for (UnaryRule rule: rules){
					if(leftSides.contains(rule.child)){ //only go with ones that are in the left side
						String B = rule.getParent(); //NP
						//NP -> N (parent(B) -> child(A))
						double probability = rule.score*scoreMap.get(begin).get(end).getCount(A);//It's okay if = 0
						if(probability > scoreMap.get(begin).get(end).getCount(B)){
							scoreMap.get(begin).get(end).setCount(B, probability);
							addToBackMap(begin, end, 0, B, A, null);//**Look at how this method is built
							added = true;
							newNonTerms.add(A);//Just add the new child-nonTerms
						}
					}//End of if statement
				}//for unary rules end
			}//for String A (nonterms)*/
			nonTerms = newNonTerms;
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
    	
    	/*double bestScore = 0;
    	Set<String> bestScoreKeys = scoreMap.get(0).get(numWords).keySet();
    	for(String key : bestScoreKeys){
    		double tempScore = getScore(0, numWords, key);
    		if(tempScore > bestScore)
    			bestScore = tempScore;
    	}
    	String initialLabel = getInitialLabel(bestScore);*/
    	//Above doesn't matter if we know to start at root every time
    	Tree<String> parseTree = new Tree<String>("ROOT");
    	recursivelyBuildTree(parseTree, "ROOT", 0, numWords);
    	
    	return TreeAnnotations.unAnnotateTree(parseTree);
    }
    
    
    //Given the first label, we want to build the rest of our tree
    private void recursivelyBuildTree(Tree<String> tree, String label, int start, int end){
    	Triplet<Integer, Integer, String> curTriple = new Triplet<Integer, Integer, String>(start, end, label);
    	Pair<backTraceData, backTraceData> curPair = back.get(curTriple);
    	//Base case
    	if(curPair == null){
    		return;
    	}
    	//This means we have a terminal
    	if(curTriple.getSecond() == -1){
    		System.out.println("Hit terminal");
    		System.out.println(curTriple.toString() + "..." + curPair.toString());
    		return;
    	}
    	//Get the sides of each of our tree
    	backTraceData leftSide = curPair.getFirst();
    	backTraceData rightSide = curPair.getSecond();
    	
    	Tree<String> leftTree = new Tree<String>(leftSide.label);
    	List<Tree<String>> children = new ArrayList<Tree<String>>();
    	//Left tree
    	System.out.println(leftSide.toString());
    	System.out.println("label: " + label + " leftSideLabel :" + leftSide.label);
    	/*if(label.equals(leftSide.label)){
    		if(start == numWords -1){
    			children.add(leftTree);
    			tree.setChildren(children);
    		}
    		return;
    	}*/
    	recursivelyBuildTree(leftTree, leftSide.label, leftSide.leftFence, leftSide.rightFence);
    	children.add(leftTree);
    	
    	//Then right tree
    	if(rightSide != null){
    		Tree<String> rightTree = new Tree<String>(rightSide.label);
        	recursivelyBuildTree(rightTree, rightSide.label, rightSide.leftFence, rightSide.rightFence);
        	children.add(rightTree);
    	}
    	
    	tree.setChildren(children);
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
    	
    	public String toString(){
    		return label + "->[" + leftFence + ", " + rightFence + "]";
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
