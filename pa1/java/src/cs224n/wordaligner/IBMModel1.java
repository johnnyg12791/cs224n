package cs224n.wordaligner;

import java.util.*;

import cs224n.util.Counter;
import cs224n.util.CounterMap;

public class IBMModel1 implements WordAligner {
	
	private double[][] model1Matrix;
	private HashMap<String, Integer> srcMap;
	private HashMap<String, Integer> targetMap;

	@Override
	public Alignment align(SentencePair sentencePair) {
		System.out.println(sentencePair);
		Alignment alignment = new Alignment();
		int numSourceWords = sentencePair.getSourceWords().size();
		int numTargetWords = sentencePair.getTargetWords().size();
		for (int sourceIndex = 0; sourceIndex < numSourceWords; sourceIndex++) {
			for (int targetIndex = 0; targetIndex < numTargetWords; targetIndex++) {
				alignment.addPredictedAlignment(targetIndex, sourceIndex);
			}
		}
		
		return alignment;
	}

	@Override
	public void train(List<SentencePair> trainingData) {
		buildModel1Matrix(trainingData);
		
		
		int limit = 20;
		for (int i = 0; i < limit; i++) {
			for (SentencePair sentencePair : trainingData) {
				CounterMap<String, String> countMap = new CounterMap<String, String>();
				Counter<String> foreignMap = new Counter<String>();
				List<String> srcList = sentencePair.getSourceWords();
				List<String> targetList = sentencePair.getTargetWords();
				//for (int targetIndex = 0; targetIndex < targetList.size(); targetIndex++) {
				for(String targetWord : targetList){	
					double score = 0;
					//for (int srcIndex = 0; srcIndex < srcList.size(); srcIndex++) {
					for(String srcWord : srcList){	
						score += getProbability(srcWord, targetWord);
						
						
					}
				}
			}
		}
	}
	
	
	
	public void buildModel1Matrix(List<SentencePair> trainingData){
		Set<String> srcWords = new HashSet<String>();
		Set<String> targetWords = new HashSet<String>();
		srcMap = new HashMap<String, Integer>();
		targetMap = new HashMap<String, Integer>();
		
		for (SentencePair sentencePair : trainingData) {
			List<String> srcList = sentencePair.getSourceWords();
			List<String> targetList = sentencePair.getTargetWords();
			for (String srcWord : srcList) {
				if (!srcWords.contains(srcWord)) {
					srcWords.add(srcWord);
				}
			}
			for (String targetWord : targetList) {
				if (!targetWords.contains(targetWord)) {
					targetWords.add(targetWord);
				}
			}
		}
		model1Matrix = new double[srcWords.size()][targetWords.size()];
		double p = 1.0/model1Matrix.length;
		for (int i = 0; i < model1Matrix.length; i++) {
			Arrays.fill(model1Matrix[i], p);
		}
		//I am also going to build the src and target maps
		int index = 0;
		for (String srcWord : srcWords){
			srcMap.put(srcWord, index);
			index++;
		}
		index = 0;
		for (String targetWord : targetWords){
			targetMap.put(targetWord, index);
			index++;
		}
	}
	
	private void printmodel1Matrix() {
		for (int i = 0; i < model1Matrix.length; i++) {
			for (int j = 0; j < model1Matrix[i].length; j++) {
				System.out.print(model1Matrix[i][j] + " ");
			}
			System.out.println("\n");
		}
	}
	
	private double getProbability(String srcWord, String targetWord) {
		int srcIndex = srcMap.get(srcWord);
		int targetIndex = targetMap.get(targetWord);
		return model1Matrix[targetIndex][srcIndex];
	}
}
