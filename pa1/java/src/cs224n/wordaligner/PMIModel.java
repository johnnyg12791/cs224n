package cs224n.wordaligner;
import java.util.List;

import cs224n.util.*;


public class PMIModel implements WordAligner {

	private Counter<String> sourceCounts;
	private Counter<String> targetCounts;
	private Counter<String> sourceProbability;
	private Counter<String> targetProbability;
	private CounterMap<String, String> sourceTargetCounts;
	private double missingWordProbability = 0.000001;
	
	@Override
	public Alignment align(SentencePair sentencePair) {
	    Alignment alignment = new Alignment();
	    int numSourceWords = sentencePair.getSourceWords().size();
	    int numTargetWords = sentencePair.getTargetWords().size();
	    //For each source word, find the best target word
	    for (int srcIndex = 0; srcIndex < numSourceWords; srcIndex++) {
	    	int bestTargetIndex = 0;
	    	double bestTargetScore = 0;
    		String sourceWord = sentencePair.sourceWords.get(srcIndex);
    		//go through all the target words, get P(f,e)/P(f)*p(e)
    		//If that new score is higher than previous, make that best score and index
	    	for(int tgtIndex = 0; tgtIndex < numTargetWords; tgtIndex++){
	    		String targetWord = sentencePair.targetWords.get(tgtIndex);
	            double tmpTargetScore = sourceTargetCounts.getCount(sourceWord, targetWord);
	            //check that the target word is in our dataset
	            if(targetProbability.containsKey(targetWord) && sourceProbability.containsKey(sourceWord)){
	            	tmpTargetScore = tmpTargetScore/(sourceProbability.getCount(sourceWord)*targetProbability.getCount(targetWord));
	            	if(tmpTargetScore > bestTargetScore){		
	            		bestTargetScore = tmpTargetScore;
	            		bestTargetIndex = tgtIndex;
	            	}
	            }else{
	            	//If the source word isnt in our database:
	            	//If the target word isnt in our database:
	            	System.out.println("target or source word not in db");
	            }
	            //also want to compare against null...
	            tmpTargetScore = sourceTargetCounts.getCount(NULL_WORD, targetWord);
            	tmpTargetScore = tmpTargetScore/(sourceProbability.getCount(NULL_WORD)*targetProbability.getCount(targetWord));
	    	}
	    	
	    	//System.out.println(sourceWord + " " + sentencePair.targetWords.get(bestTargetIndex));
	        alignment.addPredictedAlignment(bestTargetIndex, srcIndex);
	        
	    }
	    
		return alignment;
	}

	@Override
	public void train(List<SentencePair> trainingData) {
		sourceCounts = new Counter<String>();
		targetCounts = new Counter<String>();
		sourceProbability = new Counter<String>();
		targetProbability = new Counter<String>();
		sourceTargetCounts = new CounterMap<String,String>();
	    for(SentencePair pair : trainingData){
	      sourceCounts.incrementCount(NULL_WORD, 1.0); //increment the null word every sentence
	      List<String> targetWords = pair.getTargetWords();
	      List<String> sourceWords = pair.getSourceWords();
	      for(String source : sourceWords){
	    	sourceCounts.incrementCount(source, 1.0);
	        for(String target : targetWords){
	          targetCounts.incrementCount(target, 1.0);
	          sourceTargetCounts.incrementCount(source, target, 1.0);
	        }
	      }
	    }
	    
	    //Now I want to divide by the counts of source and target
	    double sourceTotal = sourceCounts.totalCount();
	    //System.out.println(sourceTotal);
	    for(String word : sourceCounts.keySet()){
	    	//System.out.println(probability);
	    	sourceProbability.incrementCount(word, sourceCounts.getCount(word)/sourceTotal);
	    }
	    double targetTotal = targetCounts.totalCount();
	    for(String word : targetCounts.keySet()){
	    	targetProbability.incrementCount(word, targetCounts.getCount(word)/targetTotal); 
	    }
	  }

}

