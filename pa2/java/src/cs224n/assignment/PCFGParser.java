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
	private ArrayList<ArrayList<Map<String, Triplet<Integer, String, String>>>> back2;
    private ArrayList<ArrayList<Counter<String>>> scoreMap;
    private int numWords;
    private Set<String> lexiconTags;
    private int numSentences = 0;
    
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
    }

    /*
     * Our Main method where we get the best parse
     */
    public Tree<String> getBestParse(List<String> sentence) {
    	numSentences++;
    	//System.out.println(sentence);
    	numWords = sentence.size();
    	// Initialization
    	initializeScoreMap();
    	//This is stage 1 of our dynamic programming, the "base"
    	initializeBackMap();
    	for (int i = 0; i < numWords; i++) {
    		for (String terminal : lexiconTags) {
    			double d = lexicon.scoreTagging(sentence.get(i), terminal);
    			//scoreMap.get(i).get(i + 1).setCount(terminal, d);
    			setScore(i, i+1, terminal, d);
    			Triplet<Integer, String, String> newTriplet = new Triplet<Integer, String, String>(-2, sentence.get(i), "");
    			back2.get(i).get(i+1).put(terminal, newTriplet);
    		}
    		//long startTime = System.currentTimeMillis();
    		HandleUnaries(i, i+1);
    		//long endTime = System.currentTimeMillis();
    		//System.out.println("Unaries 1: " + (endTime - startTime) + " milliseconds");    		

    	}
    	
    	//Now we have added the first "level" (0,0), (1,1), (2,2)... of our dynamic pyramid
    	//Time to move on up...
    	for(int span = 2; span <= numWords; span++){
    		//long startTime2 = System.currentTimeMillis();
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
    						String C = rule.rightChild;

    						double probability = BScore*getScore(split, end, C)*ruleScore;
    	    				if(probability > parentScore){
    	    					setScore(begin, end, rule.parent, probability);
    	    			    	Triplet<Integer, String, String> newTriplet = new Triplet<Integer, String, String>(split, B, C);
    	    			    	back2.get(begin).get(end).put(rule.parent, newTriplet);
    	    				}
    					}
    				}
    			}//End of splits
    			HandleUnaries(begin, end);
    		}
    		//long endTime2 = System.currentTimeMillis();
    		//System.out.ln("Span Loop: " + (endTime2 - startTime2) + " milliseconds");   
    	}
    	//printScoreMap();
    	System.out.println(numSentences);
        return buildTree();
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
			//System.out.println(nonTerms.size()); //Look how the size of the nonTerms gets smaller

			for (String A : nonTerms){
				List<UnaryRule> rules = grammar.getUnaryRulesByChild(A);
				//System.out.println("numRules: " + rules.size());
				for (UnaryRule rule: rules){
					if(leftSides.contains(rule.child)){ //only go with ones that are in the left side
						String B = rule.getParent(); //NP
						//NP -> N (parent(B) -> child(A))
						double probability = rule.score*scoreMap.get(begin).get(end).getCount(A);//It's okay if = 0
						if(probability > scoreMap.get(begin).get(end).getCount(B)){
							scoreMap.get(begin).get(end).setCount(B, probability);
							//addToBackMap(begin, end, 0, B, A, null);//**Look at how this method is built
			    			Triplet<Integer, String, String> newTriplet = new Triplet<Integer, String, String>(-1, A, "");
			    			back2.get(begin).get(end).put(B, newTriplet);
			    			
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
    
    //Initialize back map
    private void initializeBackMap(){
    	back2 = new ArrayList<ArrayList<Map<String, Triplet<Integer, String, String>>>>();
    	for (int i = 0; i < numWords + 1; i++){
    		back2.add(new ArrayList<Map<String, Triplet<Integer, String, String>>>());
    		for (int j = 0; j < numWords + 1; j++){
    			back2.get(i).add(new HashMap<String, Triplet<Integer, String, String>>());
    		}
    	}
    }
    
    
    //This builds the tree recursively by calling the recursive funtion
    private Tree<String> buildTree(){
    	//Above doesn't matter if we know to start at root every time
    	Tree<String> parseTree = new Tree<String>("ROOT");
    	//System.out.println(back2);
    	recursivelyBuildTree(parseTree, "ROOT", 0, numWords);
    	return TreeAnnotations.unAnnotateTree(parseTree);
    }
    
    
    //Given the first label, we want to build the rest of our tree
    private void recursivelyBuildTree(Tree<String> tree, String label, int start, int end){
    	//Triplet<Integer, Integer, String> curTriple = new Triplet<Integer, Integer, String>(start, end, label);
    	Triplet<Integer, String, String> curTriple = back2.get(start).get(end).get(label);
    	//System.out.println(curTriple);
    	if(curTriple  == null){
    		return;
    	}
    	int split = curTriple.getFirst();
    	//Triplet<Integer, String, String> leftTriple = back2.get(start).get(split)
    	if(split == -2){
    		
    		//System.out.println("Hit terminal");
    		String terminalString = curTriple.getSecond();
    		Tree<String> terminalTree = new Tree<String>(terminalString);
    		List<Tree<String>> child = new ArrayList<Tree<String>>();
    		child.add(terminalTree);
    		tree.setChildren(child);
    		return;
    	}
    	//This is the case of a regular binary rule
    	if(split != -1){
    		String leftString = curTriple.getSecond();
    		String rightString = curTriple.getThird();

    		Tree<String> leftTree = new Tree<String>(leftString);
    		List<Tree<String>> children = new ArrayList<Tree<String>>();
    		recursivelyBuildTree(leftTree, leftString, start, split);
    		children.add(leftTree);
    		
    		Tree<String> rightTree = new Tree<String>(rightString);
    		recursivelyBuildTree(rightTree, rightString, split, end);
    		children.add(rightTree);
    		tree.setChildren(children);
    	}
    	//nonterminal unary condition (only build left tree)
    	if(split == -1){
    		String leftString = curTriple.getSecond();
    		Tree<String> leftTree = new Tree<String>(leftString);
    		List<Tree<String>> children = new ArrayList<Tree<String>>();
    		recursivelyBuildTree(leftTree, leftString, start, end);
    		children.add(leftTree);
    		tree.setChildren(children);
    		
    	}
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
   
    
}
