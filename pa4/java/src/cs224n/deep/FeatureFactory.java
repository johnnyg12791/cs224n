package cs224n.deep;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.ejml.simple.*;


public class FeatureFactory {


	private FeatureFactory() {

	}

	 
	static List<Datum> trainData;
	/** Do not modify this method **/
	public static List<Datum> readTrainData(String filename) throws IOException {
        if (trainData==null) trainData= read(filename);
        return trainData;
	}
	
	static List<Datum> testData;
	/** Do not modify this method **/
	public static List<Datum> readTestData(String filename) throws IOException {
        if (testData==null) testData= read(filename);
        return testData;
	}
	
	private static List<Datum> read(String filename)
			throws FileNotFoundException, IOException {
	    // TODO: you'd want to handle sentence boundaries
		List<Datum> data = new ArrayList<Datum>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (line.trim().length() == 0) {
				continue;
			}
			String[] bits = line.split("\\s+");
			String word = bits[0];
			String label = bits[1];

			Datum datum = new Datum(word, label);
			data.add(datum);
		}

		return data;
	}
 
 
	// Look up table matrix with all word vectors as defined in lecture with dimensionality n x |V|
	static SimpleMatrix allVecs; //access it directly in WindowModel
	public static SimpleMatrix readWordVectors(String vecFilename) throws IOException {
		ArrayList<ArrayList<Double>> arrayListMatrix = new ArrayList<ArrayList<Double>>();

		//TODO Written by John, NEEDS TO BE TESTED TODO
		File vecFile = new File(vecFilename);
		try {
			Scanner fileScanner = new Scanner(vecFile);
			while(fileScanner.hasNextLine()){
				String nextLine = fileScanner.nextLine();
				Scanner lineScanner = new Scanner(nextLine);
				ArrayList<Double> wordVector = new ArrayList<Double>();
				while(lineScanner.hasNextDouble()){
					wordVector.add(lineScanner.nextDouble());
				}
				arrayListMatrix.add(wordVector);
				lineScanner.close();
			}
			fileScanner.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}
		
		//Now we convert the list of lists to our simple matrix
		allVecs = new SimpleMatrix(arrayListMatrix.size(), arrayListMatrix.get(0).size());
		for(int i = 0; i < arrayListMatrix.size(); i++){
			ArrayList<Double> row = arrayListMatrix.get(i);
			for(int j = 0; j < row.size(); j++){
				allVecs.set(i, j, row.get(j));
			}
		}
		
		//allVecs = new SimpleMatrix(matrix);
		
		if (allVecs!=null) return allVecs;
		return null;
	}
	
	// might be useful for word to number lookups, just access them directly in WindowModel
	public static HashMap<String, Integer> wordToNum = new HashMap<String, Integer>(); 
	public static HashMap<Integer, String> numToWord = new HashMap<Integer, String>();

	public static HashMap<String, Integer> initializeVocab(String vocabFilename) throws IOException {
		//Created by John, it reads in the vocab file and sets up the 2 maps from above
		//TODO: Has not been tested
		BufferedReader reader = new BufferedReader(new FileReader(vocabFilename));
		String line = null;
		int index = 0;
		while ((line = reader.readLine()) != null) {
			String[] words = line.split("\\s");
			for (String word : words) {
				wordToNum.put(word, index);
				numToWord.put(index, word);
			}
			
		index++;
		}

		return wordToNum;
	}
 

}
