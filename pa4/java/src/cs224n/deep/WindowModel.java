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
		
		//	TODO Feedforward function
		
		
	}
	
	
	/*
	 * This function returns the gradient of U
	 * It is a simple matrix with the same dimentions as U: k x H
	 * In our project, k is 5 and H is 100
	 */
	private SimpleMatrix gradientU(List<Datum> trainData, int pos) {
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
	private SimpleMatrix gradientB2(List<Datum> trainData, int pos) {
		// Get y
		// Get p (5x1)
		// Return 1/m * (p - y)
		return null;
	}
	
	
	/*
	 * 
	 */
	private SimpleMatrix gradientW(List<Datum> trainData, int pos) {
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
	
	private SimpleMatrix gradientB1(List<Datum> trainData, int pos) {
		// Get UT
		// Get z
		// For each position make 1 - tanh^2(zi)
		// Compute x = UT(y - p)
		// For each position i, multiply xi by zi
		// Return zi
		return null;
	}
	
	private SimpleMatrix gradientL(List<Datum> trainData, int pos) {
		return null;
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
