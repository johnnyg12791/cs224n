package cs224n.deep;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.*;
import java.util.*;

import org.ejml.data.*;
import org.ejml.simple.*;


import java.text.*;

public class WindowModel {

	protected SimpleMatrix W, U, B1, B2;
	public int windowSize,wordSize, hiddenSize;
	public HashMap<String, String> exactMatchMap;
	public HashMap<String, String> unambiguousMatchMap ;
	private Map<String, Integer> labelMap = new HashMap<String, Integer>() {
		{
			put("O", 0);
			put("LOC", 1);
			put("MISC", 2);
			put("ORG", 3);
			put("PER", 4);
		}
	};
	
	private final static int K = 5;
	private SimpleMatrix lookupTable;
	private double alpha;
	
	
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
		B1 = new SimpleMatrix(hiddenSize, 1); //H x 1
		for(int i = 0; i < hiddenSize; i++){
			B1.set(i, 0, 0.0);
		}
		B2 = new SimpleMatrix(K, 1); //K x 1 (or 1 x K?)
		for(int i = 0; i < K; i++){
			B2.set(i, 0, 0.0);
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
		SimpleMatrix H = U.mult(A).plus(B2); //The sizes should work out correctly, but untested
		return H;
	}
	
	/*
	 * This function returns the gradient of U
	 * It is a simple matrix with the same dimension as U: k x H
	 * In our project, k is 5 and H is 100
	 */
	private SimpleMatrix gradientU(int pos, List<Datum> trainingData) {
		// Get h: Hx1 (column vector)
		SimpleMatrix h = getMatrixA(pos, trainingData);
		// Get hT (1xH)
		SimpleMatrix hT = h.transpose();
		// Get (p - y) (5x1)
		SimpleMatrix y = getTrueY(pos, trainingData);
		SimpleMatrix p = softMax(getMatrixH(pos, trainingData));		
		SimpleMatrix difference = p.minus(y);

		// Return 1/m * (p-y) * hT = 5xH 
		SimpleMatrix result = difference.mult(hT);
		
		return result.scale(1.0/trainingData.size());
	}
	
	private SimpleMatrix getTrueY(int pos, List<Datum> trainingData) {
		Datum d = trainingData.get(pos);
		int yIndex = labelMap.get(d.label);
		SimpleMatrix y = new SimpleMatrix(K, 1);
		y.set(yIndex, 0, 1);
		return y;
	}
	
	
	/*
	 * This function gets the gradient of b2.
	 * It returns a simple matrix with the same dimensions as B1: k x 1
	 * In our project, k is 5.
	 */
	private SimpleMatrix gradientB2(int pos, List<Datum> trainingData) {
		// Get y
		// Get p (5x1)
		// Get (p - y) (5x1)
		SimpleMatrix y = getTrueY(pos, trainingData);
		SimpleMatrix p = softMax(getMatrixH(pos, trainingData));		
		SimpleMatrix difference = p.minus(y);

		// Return 1/m * (p - y)
		return difference.scale(1.0/trainingData.size());
	}
	
	
	/*
	 * This function returns the gradient of W.
	 * W is a matrix with dimensions H x Cn
	 * In our project, C = 5, H = 100 and n = 50
	 */
	private SimpleMatrix gradientW(int pos, List<Datum> trainingData) {
		// Get UT
		SimpleMatrix UT = U.transpose();
		// Get xT
		SimpleMatrix xT = getXi(pos, trainingData).transpose();
		// Get y
		SimpleMatrix y = getTrueY(pos, trainingData);
		// Get p
		SimpleMatrix p = softMax(getMatrixH(pos, trainingData));
		// Compute v = UT*y*xT - UT*p*xT
		SimpleMatrix v1 = (UT.mult(y)).mult(xT);
		SimpleMatrix v2 = (UT.mult(p)).mult(xT);
		
		SimpleMatrix v = v1.minus(v2);
		
		// Compute z
		SimpleMatrix z = getMatrixZ(pos, trainingData);
		// Update each position with 1 - tanh^2(zi)
		// z is a column vector
		for(int i = 0; i < z.numRows(); i++) {
			z.set(i, 0, 1 - Math.tanh(z.get(i, 0))*Math.tanh(z.get(i, 0)));
		}
		
		// Return elementwise multiplication of z and v
		return z.elementMult(v);
	}
	
	private SimpleMatrix gradientB1(int pos, List<Datum> trainingData) {
		// Get UT
		SimpleMatrix UT = U.transpose();
		// Compute z
		SimpleMatrix z = getMatrixZ(pos, trainingData);
		// For each position make 1 - tanh^2(zi)
		for(int i = 0; i < z.numRows(); i++) {
			z.set(i, 0, 1 - Math.tanh(z.get(i, 0))*Math.tanh(z.get(i, 0)));
		}
		// Get y
		SimpleMatrix y = getTrueY(pos, trainingData);
		// Get p
		SimpleMatrix p = softMax(getMatrixH(pos, trainingData));
		
		// Compute x = UT(y - p)
		SimpleMatrix x = UT.mult(y.minus(p));
		
		// Return elementwise multiplication of z and x
		return z.elementMult(x);
	}
	
	private SimpleMatrix gradientL(int pos, List<Datum> trainingData) {
		int[] windows = getWindowNums(pos, trainingData);
		SimpleMatrix gradL = new SimpleMatrix(lookupTable.numRows(), lookupTable.numCols());
		// Get p
		SimpleMatrix p = softMax(getMatrixH(pos, trainingData));
		SimpleMatrix z = getMatrixZ(pos, trainingData);
		// For each position make 1 - tanh^2(zi)
		for(int j = 0; j < z.numRows(); j++) {
			z.set(j, 0, 1 - Math.tanh(z.get(j, 0))*Math.tanh(z.get(j, 0)));
		}
		SimpleMatrix UT = U.transpose();
		SimpleMatrix y = getTrueY(pos, trainingData);
		SimpleMatrix diff = y.minus(p);
		SimpleMatrix prod = UT.mult(diff);
		SimpleMatrix rowFactor = prod.elementMult(z).transpose();
		
		for(int i = 0; i < windows.length; i++) {
			modifyColumn(windows[i], i, gradL, pos, trainingData, rowFactor);
		}
		
		return gradL;
	}
	
	private void modifyColumn(int col, int posInWindow, SimpleMatrix gradL, int pos, 
			List<Datum> trainingData, SimpleMatrix rowFactor) {
		for(int row = 0; row < gradL.numRows(); row++) {
			SimpleMatrix W = getPortionOfW(posInWindow, row);
			double result = rowFactor.mult(W).get(0, 0);
			gradL.setColumn(row, col, result);
		}		
	}
	
	private SimpleMatrix getPortionOfW(int posInWindow, int row) {
		int n = W.numCols() / windowSize;
		return W.extractVector(false, posInWindow * n + row);
	}
	
	

	//Gets matrix Z based on i
	private SimpleMatrix getMatrixZ(int i, List<Datum> trainData){
		SimpleMatrix Xi = getXi(i, trainData);
		SimpleMatrix Z = W.mult(Xi).plus(B1);
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
	
	
	
	// JUSTIN'S AWESOME STUFF ------------------------------------------------------------------------------------------
	private double costFunction(List<Datum> _trainingData) {
		double cost = 0;
		int m = _trainingData.size();
		for (int i = 0; i < m; i++) {
			int index = labelMap.get(_trainingData.get(i).label);
			SimpleMatrix pTheta = getPTheta(i, _trainingData);
			
				// add Log
			cost += Math.log(pTheta.get(index));
		}
		return cost;
	}
	// END OF JUSTIN'S AWESOME STUFF ----------------------------------------------------------------------------------
	
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
