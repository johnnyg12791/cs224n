package cs224n.deep;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.*;
import java.util.*;

import org.ejml.data.*;
import org.ejml.simple.*;


import java.text.*;

public class WindowModel {

	protected SimpleMatrix L, W, U, b1, b2;
	public int windowSize,wordSize, hiddenSize;
	public HashMap<String, String> exactMatchMap;
	public HashMap<String, String> unambiguousMatchMap;
	private final static int K = 5;
	private SimpleMatrix lookupTable;
	private double alpha = 0.001;
	
	
	public static String OUTPUT_FILENAME = "example1.out";
	//Initial defaults are: (5, 100,0.001)
	public WindowModel(int _windowSize, int _hiddenSize, double _lr, SimpleMatrix allVecs){
		//TODO
		windowSize = _windowSize; //C
		hiddenSize = _hiddenSize; //H
		alpha = _lr; //alpha
		wordSize = 50; //As specified in the assignment handout (n)
		exactMatchMap = new HashMap<String, String>();
		unambiguousMatchMap = new HashMap<String, String>();
		lookupTable = allVecs;
	}

	/**
	 * Initializes the weights randomly. 
	 */
	public void initWeights(){
		//TODO
		// initialize with bias inside as the last column
		Random rand = new Random();
		double eInit = Math.sqrt(6)/Math.sqrt(windowSize*wordSize + hiddenSize);//nC + H
		W = SimpleMatrix.random(hiddenSize, windowSize*wordSize, -eInit, eInit, rand);
		// U for the score
		U = SimpleMatrix.random(K, hiddenSize, -eInit, eInit, rand);//K x H
		
		//Init biases to 0
		b1 = new SimpleMatrix(hiddenSize, 1); //H x 1
		for(int i = 0; i < hiddenSize; i++){
			b1.set(i, 0, 0.0);
		}
		b2 = new SimpleMatrix(K, 1); //K x 1 (or 1 x K?)
		for(int i = 0; i < K; i++){
			b2.set(i, 0, 0.0);
		}
	}

	
	
	/**
	 * Simplest SGD training 
	 */
	public void train(List<Datum> _trainData ){
		//Baseline function: Exact string matching
		for(Datum datum : _trainData){
			exactMatchMap.put(datum.word, datum.label);
			if(unambiguousMatchMap.containsKey(datum.word)){
				if(unambiguousMatchMap.get(datum.word).equals(datum.label)){
					//This means the match is unambiguous and we are okay
				} else {
					//We have an ambiguity, so we replace whatever was there previously with a "O"
					unambiguousMatchMap.put(datum.word, "O");
				}
			} else {
				unambiguousMatchMap.put(datum.word, datum.label);
			}
		}
		//End of baseline function
		runSGD(_trainData);

		//	TODO Feedforward function
		for(int i = 0; i < _trainData.size(); i++){
			//SimpleMatrix Xi = getXi(i, _trainData);
			SimpleMatrix H = getMatrixH(i, _trainData);
			SimpleMatrix P = softMax(H);
			System.out.println(P);
			
			//TODO backprop (with SGD, so only for this window)
		}	
	}
	
	
	//This is the softmax
	private SimpleMatrix softMax(SimpleMatrix H){
		SimpleMatrix P = new SimpleMatrix(H.numRows(), H.numCols());
		double sumOfAllTerms = 0.0;
		for(int i = 0; i < H.numRows(); i++){
			sumOfAllTerms += Math.exp(H.get(i, 0));
		}
		
		for(int i = 0; i < K; i++){
			double pi = Math.exp(H.get(i, 0));
			P.set(i, 0, pi/sumOfAllTerms);
		}
		return P;
	}
	
	
	
	//This gets matrix H by taking Ut*A + b2
	private SimpleMatrix getMatrixH(int i, List<Datum> trainData){
		SimpleMatrix A = getMatrixA(i, trainData);
		SimpleMatrix H = U.mult(A).plus(b2); //The sizes should work out correctly, but untested
		return H;
	}
	
	
	//Gets matrix Z based on i
	private SimpleMatrix getMatrixZ(int i, List<Datum> trainData){
		SimpleMatrix Xi = getXi(i, trainData);
		SimpleMatrix Z = W.mult(Xi).plus(b1);
		return Z;
	}
	
	
	//Call this to get the matrix A (we call tanH on every element of Z)
	private SimpleMatrix getMatrixA(int i, List<Datum> trainData){
		SimpleMatrix Z = getMatrixZ(i, trainData);
		SimpleMatrix A = new SimpleMatrix(Z.numRows(), Z.numCols());
		for(int row = 0; row < Z.numRows(); row++){
			for(int col = 0; col < Z.numCols(); col++){
				A.set(Math.tanh(Z.get(row, col)));
			}
		}
		return A;
	}
	
	
	//This is given the index of a word in the training data, from our lookup table
	public int[] getWindowNums(int middleIndex, List<Datum> trainData){
		int[] windowNums = new int[windowSize];
		HashMap<String, Integer> wordToNum = FeatureFactory.wordToNum;
		
		for(int j = middleIndex - windowSize/2; j <= middleIndex + windowSize/2; j++){
			//First we find the corresponding word
			String word = "";
			if(j < 0) 
				word = "<s>";
			else if(j > trainData.size()) 
				word = "</s>";
			else 
				word = trainData.get(j).word;
			//Then we get the index of that word from wordToNum
			if(wordToNum.containsKey(word))
				windowNums[j-middleIndex+windowSize/2] = wordToNum.get(word);
			else
				windowNums[j-middleIndex+windowSize/2] = wordToNum.get("UUUNKKK");
		}
		return windowNums;	
	}
	
	
	//Written by John
	//Gets a column simple matrix that is a column xi
	private SimpleMatrix getXi (int xposition, List<Datum> trainData){
		int[] windowNums = getWindowNums(xposition, trainData); //working
		SimpleMatrix Xi = new SimpleMatrix(windowSize * wordSize, 1);
		for(int i = 0; i < windowNums.length; i++){
			for(int j = 0; j < wordSize; j++){
				Xi.set(i*wordSize + j, 0, lookupTable.get(windowNums[i], j));//I think this is correct
			}
		}
		return Xi;
	}
	

	
	// JUSTIN'S AWESOME STUFF ---------------------------------------------------------------------------------------
	
	/*
	 * Run stochastic gradient descent
	 */
	private void runSGD(List<Datum> _trainingData) {
		int m = _trainingData.size();
		for (int i = 1; i <= m; i++) {
			// 1. U(t) = U(t-1) - alpha*d/dU Ji(U)
			SimpleMatrix gradientU = gradientU(i, _trainingData);
			updateMatrix(U, gradientU);
			
			// 2. b(2)(t) = b(2)(t-1) - alpha*d/db Ji(b(2))
			SimpleMatrix gradientB2 = gradientB2(i, _trainingData);
			updateMatrix(B2, gradientB2);
			
			// 3. W(t) = W(t-1) - alpha*d/dW Ji(W)
			SimpleMatrix gradientW = gradientW(i, _trainingData);
			updateMatrix(W, gradientW);
			
			// 4. b(1)(t) = b(1)(t-1) - alpha*d/db Ji(b(1))
			SimpleMatrix gradientB1 = gradientB1(i, _trainingData);
			updateMatrix(B1, gradientB1);
			
			// 5. L(t) = L(t-1) - alpha*d/dL Ji(L)
			SimpleMatrix gradientL = gradientL(i, _trainingData);
			updateMatrix(L, gradientL);
		}
	}
	
	/*
	 * Helper function for runSGD. Updates the specified matrix using 
	 * its current state and the gradient matrix.
	 */
	private void updateMatrix(SimpleMatrix m, SimpleMatrix gradient_m) {
		for (int row = 0; row < m.numRows(); row++) {
			for (int col = 0; col < m.numCols(); col++) {
				m.set(row, col, m.get(row, col) - alpha * gradient_m.get(row, col));
			}
		}
	}
	
	
	public void test(List<Datum> testData){
		//Baseline function
		//TODO: Has not been tested (written by John)
		try {
			FileWriter f0 = new FileWriter(OUTPUT_FILENAME);
			for(Datum datum : testData){
				f0.write(datum.word + "\t" + datum.label + "\t");
				if(unambiguousMatchMap.containsKey(datum.word)){
					//f0.write(exactMatchMap.get(datum.word));
					f0.write(unambiguousMatchMap.get(datum.word));
				} else { 
					f0.write("O");
				}
				f0.write("\n");
			}
			f0.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//End of baseline function

		// TODO
		}
	
}
