package cs224n.wordaligner;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import cs224n.util.Counter;
import cs224n.util.CounterMap;

public class IBMModel2 extends IBMModel1 implements WordAligner {

	private List<SentencePair> trainingData;
	private CounterMap<String, String> alignmentMatrix;
	private CounterMap<Integer, String> distortionMatrix;
	private Random random = new Random();
	
	@Override
	public Alignment align(SentencePair sentencePair) {
		Alignment alignment = new Alignment();
		int numSrcWords = sentencePair.getSourceWords().size();
		int numTargetWords = sentencePair.getTargetWords().size();
		for (int targetIndex = 0; targetIndex < numTargetWords; targetIndex++) {
			
		}
		return alignment;
	}

	@Override
	public void train(List<SentencePair> trainingData) {
		this.trainingData = trainingData;
		
		buildModel2Matricies();
		alignmentMatrix = new CounterMap<String, String>();
		distortionMatrix = new CounterMap<Integer, String>();
		int limit = 20;
		for (int s = 0; s < limit; s++) {
			CounterMap<String, String> alignmentCountMap = new CounterMap<String, String>();
			Counter<String> alignmentNormalizationMap = new Counter<String>();
			CounterMap<Integer, String> distortionCountMap = new CounterMap<Integer, String>();
			Counter<String> distortionNormalizationMap = new Counter<String>();
			for (SentencePair sentencePair : trainingData) {					// for k = 1...n
				List<String> srcList = sentencePair.getSourceWords();
				List<String> targetList = sentencePair.getTargetWords();
				Counter<String> scoreMap = new Counter<String>();
				int l = srcList.size();
				int m = targetList.size();
				for (int i = 0; i < l; i++) {
					for (int j = 0; j < m; j++) {
						String srcWord = srcList.get(i);
						String targetWord = targetList.get(j);
						scoreMap.incrementCount(targetWord, distortionMatrix.getCount(j, triple(i, l, m)) *
								alignmentMatrix.getCount(srcWord, targetWord));
					}
				}
				
				for (int i = 0; i < l; i++) {
					for (int j = 0; j < m; j++) {
						String srcWord = srcList.get(i);
						String targetWord = targetList.get(j);
						double d = distortionMatrix.getCount(j, triple(i, l, m)) * alignmentMatrix.getCount(srcWord, targetWord) /
								scoreMap.getCount(targetWord);
						alignmentCountMap.incrementCount(srcWord, targetWord, d);
						alignmentNormalizationMap.incrementCount(targetWord, d);
						distortionCountMap.incrementCount(j, triple(i, l, m), d);
						distortionNormalizationMap.incrementCount(triple(i, l, m), d);
					}
				}
			}
			
			// M-step: update t(f|e) and q(j|i,l,m)
			for (String srcWord : alignmentMatrix.keySet()) {
				for (String targetWord : alignmentMatrix.getCounter(srcWord).keySet()) {
					double currentProbability = alignmentMatrix.getCount(srcWord, targetWord);
					alignmentMatrix.setCount(srcWord, targetWord, currentProbability / alignmentNormalizationMap.getCount(srcWord));
				}
			}
			
			for (Integer j : distortionMatrix.keySet()) {
				for (String triple : distortionMatrix.getCounter(j).keySet()) {
					double currentProbability = distortionMatrix.getCount(j, triple);
					distortionMatrix.setCount(j, triple, currentProbability / distortionNormalizationMap.getCount(triple));
				}
			}
		}
	}
	
	public void buildModel2Matricies() {
		// calculate initial probabilities for alignment matrix
		Set<String> srcWords = new HashSet<String>();
		for (SentencePair sentencePair : trainingData) {
			List<String> srcList = sentencePair.getSourceWords();
			for (String srcWord : srcList) {
				if (!srcWords.contains(srcWord)) {
					srcWords.add(srcWord);
				}
			}
		}
		double p = 1.0/srcWords.size();
		
		// build  alignment matrix and distortion matrix
		for (SentencePair sentencePair : trainingData) {
			List<String> srcList = sentencePair.getSourceWords();
			List<String> targetList = sentencePair.getTargetWords();
			int srcListLength = srcList.size();				// l
			int targetListLength = targetList.size();		// m
			for (int i = 0; i < srcListLength; i++) {
				for (int j = 0; j < targetListLength; j++) {
					alignmentMatrix.setCount(srcList.get(i), targetList.get(j), p);
					distortionMatrix.setCount(j, triple(i, srcListLength, targetListLength), random.nextDouble());
				}
			}
		}
	}
	
	/*
	 * Helper function to return a string representation of a triple
	 * to be used as the second key (V) in the distortion counterMap
	 */
	private String triple(int i, int l, int m) {
		return "" + i + " " + l + " " + m;
	}
	
	private double getIncrement() {
		return 0;
	}


}
