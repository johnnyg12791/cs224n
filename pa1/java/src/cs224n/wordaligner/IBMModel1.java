package cs224n.wordaligner;

import java.util.*;

import cs224n.util.Counter;
import cs224n.util.CounterMap;

public class IBMModel1 implements WordAligner {

	private double[][] model1Matrix;
	private HashMap<String, Integer> srcMap;
	private HashMap<String, Integer> targetMap;
	private static final double CONVERGENCE_FACTOR = 0.05;

	@Override
	public Alignment align(SentencePair sentencePair) {		
		Alignment alignment = new Alignment();
		int numSourceWords = sentencePair.getSourceWords().size();
		int numTargetWords = sentencePair.getTargetWords().size();
		for (int targetIndex = 0; targetIndex < numTargetWords; targetIndex++) {
			String targetWord = sentencePair.getTargetWords().get(targetIndex);
			double bestProbability = 0;
			int bestIndex = 0;
			for (int srcIndex = 0; srcIndex < numSourceWords; srcIndex++) {
				//I want to find the highest scoring target word for each source word
				String srcWord = sentencePair.getSourceWords().get(srcIndex);
				double curProbability = getProbability(srcWord, targetWord);
				if(curProbability > bestProbability){
					bestProbability = curProbability;
					bestIndex = srcIndex;
				}
			}
			//Check if null is better than the best
			double nullProbability = getProbability(NULL_WORD, targetWord);
			if(nullProbability < bestProbability)
				alignment.addPredictedAlignment(targetIndex, bestIndex);
			//Else I don't add anything because the NULL is the best match for our target
		}

		return alignment;
	}


	@Override
	public void train(List<SentencePair> trainingData) {
		buildModel1Matrix(trainingData);

		//int limit = 20;

		int count = 0;
		boolean convergence = false;
		while (!convergence) {
			System.out.println("Beginning iteration " + count);
			//Psuedocode from http://www.inf.ed.ac.uk/teaching/courses/mt/lectures/ibm-model1.pdf
			CounterMap<String, String> countMap = new CounterMap<String, String>();
			Counter<String> normalizationMap = new Counter<String>();
			for (SentencePair sentencePair : trainingData) {
				Counter<String> scoreMap = new Counter<String>();
				List<String> srcList = sentencePair.getSourceWords();
				srcList.add(NULL_WORD);
				List<String> targetList = sentencePair.getTargetWords();
				for(String targetWord : targetList){	
					for(String srcWord : srcList){	
						scoreMap.incrementCount(targetWord, getProbability(srcWord, targetWord));
					}
				}
				for(String targetWord : targetList){
					for(String srcWord : srcList){
						double probability = getProbability(srcWord, targetWord);
						double score = scoreMap.getCount(targetWord);
						countMap.incrementCount(srcWord, targetWord, probability/score);
						normalizationMap.incrementCount(srcWord, probability/score);
					}
				}
			}
			//Update t(e|f)'s (our matrix) M-Step
			Set<String> srcKeySet = srcMap.keySet();
			Set<String> targetKeySet = targetMap.keySet();
			convergence = true;
			System.out.println(normalizationMap.totalCount());
			printModel1Matrix();
			for(String srcWord : srcKeySet){
				for(String targetWord : targetKeySet){
					double newProbability = countMap.getCount(srcWord, targetWord)/normalizationMap.getCount(srcWord);
					//System.out.printf("%s, %s: %.2f ", srcWord, targetWord, newProbability);
					double currentProbability = getProbability(srcWord, targetWord);
					//System.out.println(currentProbability + " " + newProbability);
					setProbability(srcWord, targetWord, newProbability);
					if (Math.abs(newProbability - currentProbability) > CONVERGENCE_FACTOR) {
						convergence = false;
					}
				}
				//System.out.println();
			}
			//System.out.println();
			count++;
		}	
	}



	public void buildModel1Matrix(List<SentencePair> trainingData){
		Set<String> srcWords = new HashSet<String>();
		Set<String> targetWords = new HashSet<String>();
		srcMap = new HashMap<String, Integer>();
		targetMap = new HashMap<String, Integer>();
		srcWords.add(NULL_WORD);//Add the null word

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
		//System.out.println(srcMap);
		//System.out.println(targetMap);
		//printModel1Matrix();
	}

	private void printModel1Matrix() {
		double count = 0.0;
		for (int i = 0; i < model1Matrix.length; i++) {
			for (int j = 0; j < model1Matrix[i].length; j++) {
				System.out.print(model1Matrix[i][j] + " ");
				count += model1Matrix[i][j];
			}
			System.out.println("\n");
		}
		System.out.println(count);
	}

	//Helper functions for our matrix (so we can get/set with words not indicies)
	private double getProbability(String srcWord, String targetWord) {
		int srcIndex = srcMap.get(srcWord);
		int targetIndex = targetMap.get(targetWord);
		return model1Matrix[srcIndex][targetIndex];
	}
	private void setProbability(String srcWord, String targetWord, double probability){
		int srcIndex = srcMap.get(srcWord);
		int targetIndex = targetMap.get(targetWord);
		model1Matrix[srcIndex][targetIndex] = probability;
	}
}
