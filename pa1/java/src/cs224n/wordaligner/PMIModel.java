package cs224n.wordaligner;
import java.util.List;

import cs224n.util.*;


public class PMIModel implements WordAligner {

	private Counter<String> sourceCounts;
	private Counter<String> targetCounts;
	private CounterMap<String, String> sourceTargetCounts;
	
	@Override
	public Alignment align(SentencePair sentencePair) {
		System.out.println(sentencePair);
	    Alignment alignment = new Alignment();
	    int numSourceWords = sentencePair.getSourceWords().size();
	    int numTargetWords = sentencePair.getTargetWords().size();
	    //For each source word, find the best target word
	    for (int srcIndex = 0; srcIndex < numSourceWords; srcIndex++) {
	    	int bestTargetIndex = 0;
	    	double bestTargetScore = 0;
    		String sourceWord = sentencePair.sourceWords.get(srcIndex);
	    	for(int tgtIndex = 0; tgtIndex < numTargetWords; tgtIndex++){
	    		String targetWord = sentencePair.targetWords.get(tgtIndex);
	            double tmpTargetScore = sourceTargetCounts.getCount(sourceWord, targetWord);
	            tmpTargetScore = tmpTargetScore/(sourceCounts.getCount(sourceWord)*targetCounts.getCount(targetWord));
	            if(tmpTargetScore > bestTargetScore){		
	            	bestTargetScore = tmpTargetScore;
	            	bestTargetIndex = tgtIndex;
	            	//also want to compare against null...
	            }
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
	  }

}

