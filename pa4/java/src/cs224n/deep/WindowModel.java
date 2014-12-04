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
	private Map<Integer, String> indexToLabelMap = new HashMap<Integer, String>() {
		{
			put(0, "O");
			put(1, "LOC");
			put(2, "MISC");
			put(3, "ORG");
			put(4, "PER");
		}
	};
	
	private final static int K = 5;
	public final static double EPSILON = 0.0001;
	public final static double THRESHOLD = 0.0000001;
	private SimpleMatrix lookupTable;
	private double alpha;
	private double lambda;
	
	public static String OUTPUT_FILENAME = "example1.out";
	//Initial defaults are: (5, 100,0.001)
	public WindowModel(int _windowSize, int _hiddenSize, double _lr, SimpleMatrix allVecs, double _lambda){
		//TODO
		windowSize = _windowSize; //C
		hiddenSize = _hiddenSize; //H
		alpha = _lr; //alpha
		wordSize = 50; //As specified in the assignment handout (n)
		exactMatchMap = new HashMap<String, String>();
		unambiguousMatchMap = new HashMap<String, String>();
		lambda = _lambda;
		lookupTable = allVecs.transpose();
	}

	/**
	 * Initializes the weights randomly. 
	 */
	public void initWeights(){
		// initialize with bias inside as the last column
		Random rand = new Random();
		double eInit = Math.sqrt(6)/Math.sqrt(windowSize*wordSize + hiddenSize);//nC + H
		W = SimpleMatrix.random(hiddenSize, windowSize*wordSize, -eInit, eInit, rand);
		// U for the score
		U = SimpleMatrix.random(K, hiddenSize, -eInit, eInit, rand);//K x H
		
		//Init biases to 0
		B1 = new SimpleMatrix(hiddenSize, 1); //H x 1	
		B2 = new SimpleMatrix(K, 1); //K x 1 (or 1 x K?)
	}

	
	
	/**
	 * Simplest SGD training 
	 */
	public void train(List<Datum> _trainData ){
		//System.out.println(U.toString());
		//System.out.println(W.toString());
		for(int i = 0; i < 5; i++) {
			System.out.println("iteration: " + i);
			runSGD(_trainData);
		}
	}
	
	public void trainBaseline(List<Datum> _trainData ){
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
	}
	
	public void gradientCheck(List<Datum> _trainingData) {
		initWeights();
		for(int i = 0; i < 10; i++){
			SimpleMatrix p = softMax(getMatrixH(i, _trainingData));		
			SimpleMatrix z = getMatrixZ(i, _trainingData);
			// For each position make 1 - tanh^2(zi)
			for(int j = 0; j < z.numRows(); j++) {
				z.set(j, 0, 1 - Math.pow(Math.tanh(z.get(j, 0)), 2));
			}
			
			System.out.println("CHECKING EXAMPLE " + i);
			SimpleMatrix gradU = gradientU(i, _trainingData, p);
			checkGrad(i, _trainingData, U, gradU, "U");
			SimpleMatrix gradB2 = gradientB2(i, _trainingData, p);
			checkGrad(i, _trainingData, B2, gradB2, "B2");
			SimpleMatrix gradB1 = gradientB1(i, _trainingData, p, z);
			checkGrad(i, _trainingData, B1, gradB1, "B1");
			SimpleMatrix gradW = gradientW(i, _trainingData, gradB1);
			checkGrad(i, _trainingData, W, gradW, "W");
			SimpleMatrix gradL = gradientL(i, _trainingData, false, gradB1);
			checkGrad(i, _trainingData, lookupTable, gradL, "L");
		}	
	}
	
	private void checkGrad(int i, List<Datum> _trainData, SimpleMatrix M, SimpleMatrix gradM, String label) {
		for(int row = 0; row < M.numRows(); row++) {
			for(int col = 0; col < M.numCols(); col++) {
				M.set(row, col, M.get(row, col)+EPSILON);
				double cost1 = costFunction(_trainData, i);
				M.set(row, col, M.get(row, col)-2*EPSILON);
				double cost2 = costFunction(_trainData, i);
				// Readjust our instance variable back to original
				M.set(row, col, M.get(row, col)+EPSILON);
				
				if((gradM.get(row, col) - (cost1-cost2)/(2*EPSILON) > THRESHOLD) || 
						(gradM.get(row, col) - (cost1-cost2)/(2*EPSILON) < -1* THRESHOLD)) {
					System.out.println("OOPS!  Computed " + gradM.get(row, col) + " at position "
							+ "(" + row + ", " + col + ") in matrix " + label);
					System.out.println("Expected " + (cost1-cost2)/(2*EPSILON));
				}
			}
		}
		System.out.println("Finished checking matrix " + label + ".");
		System.out.println("If you don't see any errors, that's a success!");
		
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
	private SimpleMatrix gradientU(int pos, List<Datum> trainingData, SimpleMatrix p) {
		// Return (p-y) * hT = 5xH 
		SimpleMatrix r = getTrueY(pos, trainingData).minus(p).mult(getMatrixA(pos, trainingData).transpose());
		return r.scale(-1);
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
	private SimpleMatrix gradientB2(int pos, List<Datum> trainingData, SimpleMatrix p) {
		// Return (y - p)
		SimpleMatrix r = getTrueY(pos, trainingData).minus(p);	
		return r.scale(-1);
	}
	
	
	/*
	 * This function returns the gradient of W.
	 * W is a matrix with dimensions H x Cn
	 * In our project, C = 5, H = 100 and n = 50
	 */
	private SimpleMatrix gradientW(int pos, List<Datum> trainingData, SimpleMatrix gradB1) {
		SimpleMatrix Xi = getXi(pos, trainingData);
		return gradB1.mult(Xi.transpose());
	}
	
	private SimpleMatrix regGradientW(int pos, List<Datum> trainingData, SimpleMatrix gradB1) {
		SimpleMatrix regTerm = W.scale(lambda/(trainingData.size()));
		SimpleMatrix Xi = getXi(pos, trainingData);
		return (gradB1.mult(Xi.transpose())).plus(regTerm);
	}
	
	private SimpleMatrix regGradientU(int pos, List<Datum> trainingData, SimpleMatrix p) {
		SimpleMatrix regTerm = U.scale(lambda/(trainingData.size()));
		SimpleMatrix unregGrad = gradientU(pos, trainingData, p);
		return regTerm.plus(unregGrad);
	}
	
	private SimpleMatrix gradientB1(int pos, List<Datum> trainingData, SimpleMatrix p, SimpleMatrix z) {
		// Get UT
		SimpleMatrix UT = U.transpose();
		SimpleMatrix y = getTrueY(pos, trainingData);
		SimpleMatrix x = UT.mult(y.minus(p));
		return (z.elementMult(x)).scale(-1);
	}
	
	private SimpleMatrix gradientL(int pos, List<Datum> trainingData, boolean update, SimpleMatrix gradB1) {//SimpleMatrix p, SimpleMatrix z) {
		int[] windows = getWindowNums(pos, trainingData);
		SimpleMatrix gradL = null;
		if(!update) {
			gradL = new SimpleMatrix(lookupTable.numRows(), lookupTable.numCols());
		}
		
		for(int i = 0; i < windows.length; i++) {
			modifyColumn(windows[i], i, gradL, pos, trainingData, gradB1, update);
		}
		
		return gradL;
	}
	
	private void modifyColumn(int col, int posInWindow, SimpleMatrix gradL, int pos, 
			List<Datum> trainingData, SimpleMatrix gradB1, boolean update) {
		SimpleMatrix w = getPortionOfW(posInWindow);
		SimpleMatrix result = w.mult(gradB1);
		for(int row = 0; row < result.numRows(); row++) {
			if(!update) {
				gradL.set(row, col, gradL.get(row, col) + result.get(row, 0)); 
			}
			if(update) {
				lookupTable.set(row, col, lookupTable.get(row, col) - alpha * result.get(row, 0));
			}
		}
	}
	
	private SimpleMatrix getPortionOfW(int posInWindow) {
		int n = W.numCols() / windowSize;
		int colStart = posInWindow * n;
		int numCols = lookupTable.numRows();
		SimpleMatrix portion = W.extractMatrix(0, W.numRows(), colStart, colStart + numCols);
		return portion.transpose();
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
				A.set(row, col, Math.tanh(Z.get(row, col)));
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
			else if(j >= trainData.size()) 
				word = "</s>";
			else 
				word = trainData.get(j).word.toLowerCase();
			//Do digit checking here as well::
			//https://piazza.com/class/hyxho2urgyd6bz?cid=462
			if(word.length() > 2)
				word = convertDigitsToDG(word);
			
			//Then we get the index of that word from wordToNum
			if(wordToNum.containsKey(word)) {
				windowNums[j-middleIndex+windowSize/2] = wordToNum.get(word);
			} else {
				windowNums[j-middleIndex+windowSize/2] = wordToNum.get("UUUNKKK");
			}
		}
		return windowNums;
	}
	
	private String convertDigitsToDG(String word){
		String result = "";
		//for character in word, if its numeric, add "DG" to result, otherwise nothing
		for(int i = 0; i < word.length(); i++){
			if(Character.isDigit(word.charAt(i)))
				result += "DG";
			else
				result += word.charAt(i);
		}
		
		return result;
	}
	
	
	//Written by John
	//Gets a column simple matrix that is a column xi
	private SimpleMatrix getXi (int xposition, List<Datum> trainData){
		int[] windowNums = getWindowNums(xposition, trainData); //working
		SimpleMatrix Xi = new SimpleMatrix(windowSize * wordSize, 1);
		for(int i = 0; i < windowNums.length; i++){
			for(int j = 0; j < wordSize; j++){
				Xi.set(i*wordSize + j, 0, lookupTable.get(j, windowNums[i]));//I think this is correct
			}
		}
		return Xi;
	}
	
	
	//Whats upppp - The guy who's dating a super model
	//Pass in a lamba for regularization and m (the size of training set)
	private double getRegularizationTerm(int m){
		double regularization = 0.0;
		for(int i = 0; i < hiddenSize; i++){
			for(int j = 0; j < wordSize*windowSize; j++){
				regularization += Math.pow(W.get(i, j),2);
			}
		}
		for(int i = 0; i < K; i++){
			for(int j = 0; j < hiddenSize; j++){
				regularization += Math.pow(U.get(i, j), 2);
			}
		}
		
		return (lambda/2*m) * (regularization);
	}
	
	private double getRegularizedCostFunction(List<Datum> _trainingData, int pos) {
		return costFunction(_trainingData, pos) + getRegularizationTerm(_trainingData.size());
	}
	
	/*
	 * Run stochastic gradient descent
	 */
	private void runSGD(List<Datum> _trainingData) {
		int m = _trainingData.size();
		for (int i = 0; i < m; i++) {
			if(i % 10000 == 0) {
				System.out.println("training example " + i);
			}
			// 1. U(t) = U(t-1) - alpha*d/dU Ji(U)
			SimpleMatrix p = softMax(getMatrixH(i, _trainingData));	
			
			SimpleMatrix z = getMatrixZ(i, _trainingData);
			// For each position make 1 - tanh^2(zi)
			for(int j = 0; j < z.numRows(); j++) {
				z.set(j, 0, 1 - Math.pow(Math.tanh(z.get(j, 0)), 2));
			}

			SimpleMatrix gradientU = regGradientU(i, _trainingData, p);
			
			// 2. b(2)(t) = b(2)(t-1) - alpha*d/db Ji(b(2))
			SimpleMatrix gradientB2 = gradientB2(i, _trainingData, p);
			
			// 4. b(1)(t) = b(1)(t-1) - alpha*d/db Ji(b(1))
			SimpleMatrix gradientB1 = gradientB1(i, _trainingData, p, z);
			
			// 3. W(t) = W(t-1) - alpha*d/dW Ji(W)
			SimpleMatrix gradientW = regGradientW(i, _trainingData, gradientB1);
			
			// 5. L(t) = L(t-1) - alpha*d/dL Ji(L)
			SimpleMatrix gradientL = gradientL(i, _trainingData, true, gradientB1);
			U = updateMatrix(U, gradientU);
			B2 = updateMatrix(B2, gradientB2);
			B1 = updateMatrix(B1, gradientB1);
			W = updateMatrix(W, gradientW);
		}
	}
	
	/*
	 * Helper function for runSGD. Updates the specified matrix using 
	 * its current state and the gradient matrix.
	 */
	private SimpleMatrix updateMatrix(SimpleMatrix m, SimpleMatrix gradient_m) {
		return m.minus(gradient_m.scale(alpha));
	}
	
	
	
	// JUSTIN'S AWESOME STUFF ------------------------------------------------------------------------------------------
	private double costFunction(List<Datum> _trainingData, int pos) {
		int index = labelMap.get(_trainingData.get(pos).label);
		SimpleMatrix pTheta =  softMax(getMatrixH(pos, _trainingData));	
		// add Log
		return -1 * Math.log(pTheta.get(index));
	}
	
	public void testBaseline(List<Datum> testData){
		//Baseline function
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
	}
	
	public void test(List<Datum> testData){		
		try {
			FileWriter f0 = new FileWriter(OUTPUT_FILENAME);
			for(int i = 0; i < testData.size(); i++){
				SimpleMatrix p = softMax(getMatrixH(i, testData));
				int maxIndex = 0;
				double maxVal = p.get(0, 0);
				for(int index = 0; index < p.numRows(); index++) {
					if(p.get(index,0) > maxVal) {
						maxVal = p.get(index, 0);
						maxIndex = index;
					}
				}
				String label = indexToLabelMap.get(maxIndex);
				f0.write(testData.get(i).word + "\t" + testData.get(i).label + "\t");
				f0.write(label + "\n");
			}
			f0.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
