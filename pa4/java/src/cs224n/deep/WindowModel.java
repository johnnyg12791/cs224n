package cs224n.deep;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.*;
import java.util.*;

import org.ejml.data.*;
import org.ejml.simple.*;


import java.text.*;

public class WindowModel {

	protected SimpleMatrix L, W, Wout;
	private double learningRate;
	public int windowSize,wordSize, hiddenSize;
	public HashMap<String, String> exactMatchMap;
	public HashMap<String, String> unambiguousMatchMap ;
	private static final double ALPHA = 0.001;
	
	//private int 
	
	public static String OUTPUT_FILENAME = "example1.out";
	//Initial defaults are: (5, 100,0.001)
	public WindowModel(int _windowSize, int _hiddenSize, double _lr){
		//TODO
		windowSize = _windowSize; //C
		hiddenSize = _hiddenSize; //H
		learningRate = _lr; //alpha
		wordSize = 50; //As specified in the assignment handout (n)
		exactMatchMap = new HashMap<String, String>();
		unambiguousMatchMap = new HashMap<String, String>();
	}

	/**
	 * Initializes the weights randomly. 
	 */
	public void initWeights(){
		//TODO
		// initialize with bias inside as the last column
		// W = SimpleMatrix...
		// U for the score
		// U = SimpleMatrix...
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
		
		
	// JUSTIN'S AWESOME STUFF ----------------------------------------------------------------------------------------
		runSGD(_trainData);
	// END OF JUSTIN'S AWESOME STUFF ---------------------------------------------------------------------------------
		
		//	TODO Feedforward function
	}
	
	
	/*
	 * This function returns the gradient of U
	 * It is a simple matrix with the same dimentions as U: k x H
	 * In our project, k is 5 and H is 100
	 */
	private SimpleMatrix gradientU(int pos, List<Datum> trainingData) {
		// Get h: Hx1 (column vector)
		// Get hT (1xH)
		// Get (p - y) (5x1)
		// Return 1/m * (p-y) * hT = 5xH 
		return null;
	}
	
	
	/*
	 * This function gets the gradient of b2.
	 * It returns a simple matrix with the same dimensions as b1: k x 1
	 * In our project, k is 5.
	 */
	private SimpleMatrix gradientB2(int pos, List<Datum> trainingData) {
		// Get y
		// Get p (5x1)
		// Return 1/m * (p - y)
		return null;
	}
	
	
	/*
	 * 
	 */
	private SimpleMatrix gradientW(int pos, List<Datum> trainingData) {
		// Get UT
		// Get xT
		// Get y
		// Get p
		// Compute v = UT*y*xT - UT*px*T
		// Compute z
		// Update each position with 1 - tanh^2(zi)
		// For each position i
		// Multiply zi by vi
		// Return z
		return null;
	}
	
	private SimpleMatrix gradientB1(int pos, List<Datum> trainingData) {
		// Get UT
		// Get z
		// For each position make 1 - tanh^2(zi)
		// Compute x = UT(y - p)
		// For each position i, multiply xi by zi
		// Return zi
		return null;
	}
	
	private SimpleMatrix gradientL(int pos, List<Datum> trainingData) {
		return null;
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
				m.set(row, col, m.get(row, col) - ALPHA * gradient_m.get(row, col));
			}
		}
	}
	// END OF JUSTIN'S AWESOME STUFF -----------------------------------------------------------------------------------
	
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
