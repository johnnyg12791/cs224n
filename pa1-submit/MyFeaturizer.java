package edu.stanford.nlp.mt.decoder.feat;

import java.util.*;

import edu.stanford.nlp.mt.util.FeatureValue;
import edu.stanford.nlp.mt.util.Featurizable;
import edu.stanford.nlp.mt.util.IString;
import edu.stanford.nlp.mt.decoder.feat.RuleFeaturizer;
import edu.stanford.nlp.util.Generics;

/**
 * A rule featurizer.
 */
public class MyFeaturizer implements RuleFeaturizer<IString, String> {
 
  @Override
  public void initialize() {
    // Do any setup here.
  }

  @Override
  public List<FeatureValue<String>> ruleFeaturize(
      Featurizable<IString, String> f) {

    // TODO: Return a list of features for the rule. Replace these lines
    // with your own feature.
    List<FeatureValue<String>> features = new LinkedList<FeatureValue<String>>();

    int numSourceWordsEndInS = 0;
    int numTargetWordsEndInS = 0;
    //This puts the words into buckets of length 1-3, 4-7, 8-12...
    if(f.targetPhrase != null && f.targetPhrase.size() > 0){
	int numTokens = f.targetPhrase.size();
        features.add(new FeatureValue<String>(String.format(
          "%s:%d","NumTokens",numTokens/2),1.0));
    	int containsHas = 0;
	int containsDash = 0;
 	int containsWill = 0;	
	for(int i = 0; i < f.targetPhrase.size(); i++){
	   String curWord = f.targetPhrase.get(i).toString();
	   if(curWord.equals("has")) containsHas = 1;
	   if(curWord.equals("-")) containsDash = 1;
	   if(curWord.equals("will")) containsWill += 1;
	}
	
	features.add(new FeatureValue<String>(String.format(
	   "%s:%d", "ContainsHas",containsHas),1.0));
	features.add(new FeatureValue<String>(String.format(
	   "%s:%d", "ContainsDash",containsDash),1.0));
	features.add(new FeatureValue<String>(String.format(
	   "%s:%d", "ContainsWill",containsWill),1.0));
	features.add(new FeatureValue<String>(String.format(
	   "%s:%d", "SDifference",numSourceWordsEndInS-numTargetWordsEndInS),1.0));
     }



    //This feature...	

    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
