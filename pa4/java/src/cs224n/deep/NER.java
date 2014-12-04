package cs224n.deep;

import java.util.*;
import java.io.*;

import org.ejml.simple.SimpleMatrix;


public class NER {
    
    public static void main(String[] args) throws IOException {
		if (args.length < 2) {
		    System.out.println("USAGE: java -cp classes NER [check] ../data/train ../data/dev");
		    return;
		} if (args.length >= 3 && args[0].equals("check")) {
			// this reads in the train and test datasets
			List<Datum> trainData = FeatureFactory.readTrainData(args[1]);
			List<Datum> testData = FeatureFactory.readTestData(args[2]);
			
			//	read the train and test data
			//TODO: Implement this function (just reads in vocab and word vectors)
			FeatureFactory.initializeVocab("../data/vocab.txt");
			SimpleMatrix allVecs= FeatureFactory.readWordVectors("../data/wordVectors.txt");
		
			// initialize model 
			WindowModel model = new WindowModel(5, 100,0.001, allVecs, 0.001);
			
			model.gradientCheck(trainData);
		} else {
			// this reads in the train and test datasets
			List<Datum> trainData = FeatureFactory.readTrainData(args[0]);
			List<Datum> testData = FeatureFactory.readTestData(args[1]);	
			
			//	read the train and test data
			//TODO: Implement this function (just reads in vocab and word vectors)
			FeatureFactory.initializeVocab("../data/vocab.txt");
			SimpleMatrix allVecs= FeatureFactory.readWordVectors("../data/wordVectors.txt");
		
			// initialize model 
			WindowModel model = new WindowModel(5, 100,0.001, allVecs, 0.1);
			model.initWeights();
		
			//TODO: Implement those two functions
			model.train(trainData);
			model.test(testData);
		}
    }
}