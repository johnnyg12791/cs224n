package cs224n.wordaligner;

import java.util.HashSet;

import java.util.List;
import java.util.Random;
import java.util.Set;
import cs224n.wordaligner.*;
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
		List<String> srcWords = sentencePair.getSourceWords();
		List<String> targetWords = sentencePair.getTargetWords();
		int numSrcWords = srcWords.size();
		int numTargetWords = targetWords.size();
		for (int targetIndex = 0; targetIndex < numTargetWords; targetIndex++) {
			String targetWord = targetWords.get(targetIndex);
			double bestProbability = 0;
			int bestIndex = 0;
			for (int srcIndex = 0; srcIndex < numSrcWords; srcIndex++) {
				String srcWord = srcWords.get(srcIndex);
				double probability = alignmentMatrix.getCount(srcWord, targetWord);
				if (probability > bestProbability) {
					bestProbability = probability;
					bestIndex = srcIndex;
				}
			}
			//double nullProbability = alignmentMatrix.getCount(NULL_WORD, targetWord);
			//if (bestProbability > nullProbability) {
				alignment.addPredictedAlignment(targetIndex, bestIndex);
			//s}
		}
		return alignment;
	}

	@Override
	public void train(List<SentencePair> trainingData) {
		this.trainingData = trainingData;
		alignmentMatrix = new CounterMap<String, String>();
		distortionMatrix = new CounterMap<Integer, String>();
		buildModel2Matricies();
		//System.out.println(alignmentMatrix);
		//System.out.println(distortionMatrix);

		int limit = 15;
		for (int s = 0; s < limit; s++) {
			System.out.println("Beginning Model2 iteration " + s);
			CounterMap<String, String> alignmentCountMap = new CounterMap<String, String>();
			Counter<String> alignmentNormalizationMap = new Counter<String>();
			CounterMap<Integer, String> distortionCountMap = new CounterMap<Integer, String>();
			Counter<String> distortionNormalizationMap = new Counter<String>();
			for (SentencePair sentencePair : trainingData) {					// for k = 1...n
				List<String> srcList = sentencePair.getSourceWords();
				//srcList.add(NULL_WORD);
				List<String> targetList = sentencePair.getTargetWords();
				Counter<String> scoreMap = new Counter<String>();
				int l = srcList.size();
				int m = targetList.size();
				for (int j = 0; j < m; j++) {
					for (int i = 0; i < l; i++) {
						String srcWord = srcList.get(i);
						String targetWord = targetList.get(j);
						scoreMap.incrementCount(targetWord, distortionMatrix.getCount(j, triple(i, l, m)) *
								alignmentMatrix.getCount(srcWord, targetWord));
					}
				}

				//System.out.println(alignmentMatrix);
				//System.out.println(distortionMatrix);

				for (int j = 0; j < m; j++) {
					for (int i = 0; i < l; i++) {
						String srcWord = srcList.get(i);
						String targetWord = targetList.get(j);
						double d = distortionMatrix.getCount(j, triple(i, l, m)) * alignmentMatrix.getCount(srcWord, targetWord) /
								scoreMap.getCount(targetWord);
						alignmentCountMap.incrementCount(srcWord, targetWord, d);
						alignmentNormalizationMap.incrementCount(srcWord, d);
						distortionCountMap.incrementCount(j, triple(i, l, m), d);
						distortionNormalizationMap.incrementCount(triple(i, l, m), d);
					}
				}

				//System.out.println(alignmentMatrix);
				//System.out.println(distortionMatrix);
			}

			// M-step: update t(f|e) and q(j|i,l,m)
			for (String srcWord : alignmentMatrix.keySet()) {
				for (String targetWord : alignmentMatrix.getCounter(srcWord).keySet()) {
					//System.out.println(alignmentMatrix.totalCount());
					//System.out.println(alignmentNormalizationMap.totalCount());
					double currentProbability = alignmentCountMap.getCount(srcWord, targetWord);
					alignmentMatrix.setCount(srcWord, targetWord, currentProbability / alignmentNormalizationMap.getCount(srcWord));
				}
			}

			for (Integer j : distortionMatrix.keySet()) {
				for (String triple : distortionMatrix.getCounter(j).keySet()) {
					double currentProbability = distortionCountMap.getCount(j, triple);
					distortionMatrix.setCount(j, triple, currentProbability / distortionNormalizationMap.getCount(triple));
				}
			}
			//System.out.println(alignmentMatrix);
			//System.out.println(distortionMatrix);
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

		IBMModel1 model1 = new IBMModel1();
		model1.train(trainingData);//This will take some time...
		
		//model1.model1Matrix[][]
		
		//double p = 1.0/(srcWords.size() + 1);

		// build  alignment matrix and distortion matrix
		for (SentencePair sentencePair : trainingData) {
			List<String> srcList = sentencePair.getSourceWords();
			List<String> targetList = sentencePair.getTargetWords();
			//srcList.add(NULL_WORD);
			int srcListLength = srcList.size();				// l
			int targetListLength = targetList.size();		// m
			for (int j = 0; j < targetListLength; j++) {
				String targetWord = targetList.get(j);
				for (int i = 0; i < srcListLength; i++) {
					String srcWord = srcList.get(i);
					alignmentMatrix.setCount(srcWord, targetWord, model1.getProbability(srcWord, targetWord));
					distortionMatrix.setCount(j, triple(i, srcListLength, targetListLength), random.nextDouble());
				}
				//alignmentMatrix.setCount(NULL_WORD, targetList.get(j), p);
				
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
