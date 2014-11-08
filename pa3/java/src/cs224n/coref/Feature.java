package cs224n.coref;

import cs224n.util.Pair;

import java.util.Set;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public interface Feature {

  //-----------------------------------------------------------
  // TEMPLATE FEATURE TEMPLATES
  //-----------------------------------------------------------
  public static class PairFeature implements Feature {
    public final Pair<Feature,Feature> content;
    public PairFeature(Feature a, Feature b){ this.content = Pair.make(a, b); }
    public String toString(){ return content.toString(); }
    public boolean equals(Object o){ return o instanceof PairFeature && ((PairFeature) o).content.equals(content); }
    public int hashCode(){ return content.hashCode(); }
  }

  public static abstract class Indicator implements Feature {
    public final boolean value;
    public Indicator(boolean value){ this.value = value; }
    public boolean equals(Object o){ return o instanceof Indicator && o.getClass().equals(this.getClass()) && ((Indicator) o).value == value; }
    public int hashCode(){ 
    	return this.getClass().hashCode() ^ Boolean.valueOf(value).hashCode(); }
    public String toString(){ 
    	return this.getClass().getSimpleName() + "(" + value + ")"; }
  }

  public static abstract class IntIndicator implements Feature {
    public final int value;
    public IntIndicator(int value){ this.value = value; }
    public boolean equals(Object o){ return o instanceof IntIndicator && o.getClass().equals(this.getClass()) && ((IntIndicator) o).value == value; }
    public int hashCode(){ 
    	return this.getClass().hashCode() ^ value; 
    }
    public String toString(){ return this.getClass().getSimpleName() + "(" + value + ")"; }
  }

  public static abstract class BucketIndicator implements Feature {
    public final int bucket;
    public final int numBuckets;
    public BucketIndicator(int value, int max, int numBuckets){
      this.numBuckets = numBuckets;
      bucket = value * numBuckets / max;
      if(bucket < 0 || bucket >= numBuckets){ throw new IllegalStateException("Bucket out of range: " + value + " max="+max+" numbuckets="+numBuckets); }
    }
    public boolean equals(Object o){ return o instanceof BucketIndicator && o.getClass().equals(this.getClass()) && ((BucketIndicator) o).bucket == bucket; }
    public int hashCode(){ return this.getClass().hashCode() ^ bucket; }
    public String toString(){ return this.getClass().getSimpleName() + "(" + bucket + "/" + numBuckets + ")"; }
  }

  public static abstract class Placeholder implements Feature {
    public Placeholder(){ }
    public boolean equals(Object o){ return o instanceof Placeholder && o.getClass().equals(this.getClass()); }
    public int hashCode(){ return this.getClass().hashCode(); }
    public String toString(){ return this.getClass().getSimpleName(); }
  }

  public static abstract class StringIndicator implements Feature {
    public final String str;
    public StringIndicator(String str){ this.str = str; }
    public boolean equals(Object o){ return o instanceof StringIndicator && o.getClass().equals(this.getClass()) && ((StringIndicator) o).str.equals(this.str); }
    public int hashCode(){ return this.getClass().hashCode() ^ str.hashCode(); }
    public String toString(){ return this.getClass().getSimpleName() + "(" + str + ")"; }
  }

  public static abstract class SetIndicator implements Feature {
    public final Set<String> set;
    public SetIndicator(Set<String> set){ this.set = set; }
    public boolean equals(Object o){ return o instanceof SetIndicator && o.getClass().equals(this.getClass()) && ((SetIndicator) o).set.equals(this.set); }
    public int hashCode(){ return this.getClass().hashCode() ^ set.hashCode(); }
    public String toString(){
      StringBuilder b = new StringBuilder();
      b.append(this.getClass().getSimpleName());
      b.append("( ");
      for(String s : set){
        b.append(s).append(" ");
      }
      b.append(")");
      return b.toString();
    }
  }
  
  /*
   * TODO: If necessary, add new feature types
   */

  //-----------------------------------------------------------
  // REAL FEATURE TEMPLATES
  //-----------------------------------------------------------

  public static class CoreferentIndicator extends Indicator {
    public CoreferentIndicator(boolean coreferent){ super(coreferent); }
  }

  public static class ExactMatch extends Indicator {
    public ExactMatch(boolean exactMatch){ super(exactMatch); }
  }
  
  public static class WordsBetweenMention extends IntIndicator {
	  public WordsBetweenMention(int indicator) { super(indicator); }
  } // No change
  
  public static class IsSameGender extends Indicator {
	  public IsSameGender(boolean isSameGender){ super(isSameGender); }
  } // Makes it worse
  
  public static class NerTag1 extends StringIndicator {
	  public NerTag1(String string) { super(string); }
  }
  
  public static class GenderTag1 extends StringIndicator {
	  public GenderTag1(String string) {super(string);}
  }
  public static class NerTag2 extends StringIndicator {
	  public NerTag2(String string) { super(string); }
  }
  
  public static class GenderTag2 extends StringIndicator {
	  public GenderTag2(String string) {super(string);}
  }
  public static class SameNerTag extends Indicator {
	  public SameNerTag(boolean isSameNer){ super(isSameNer); }
  }
  
  public static class inSameSentence extends Indicator {
	public inSameSentence(boolean sameSentence){ super(sameSentence); }
  }
  
  public static class MeAndMy extends Indicator {
	public MeAndMy(boolean meAndMy) {super(meAndMy);}  
  }
  
  public static class areBothNumbers extends Indicator{
	  public areBothNumbers(boolean bothNumbers) {super(bothNumbers);}
  }
  
  public static class partOfSpeech extends Indicator{
	  public partOfSpeech(boolean POS) {super(POS);}
  }
  
  public static class partialMatch extends Indicator{
	  public partialMatch(boolean partialMatch) {super(partialMatch);}
  }//makes it worse
  
  public static class inEntity extends Indicator{
	  public inEntity(boolean bool) {super(bool);}
  }
  
  public static class lengthDifference extends IntIndicator{
	  public lengthDifference(int diff) {super(diff);}
  }
  
  public static class lengthOfFirstMention extends IntIndicator{
	  public lengthOfFirstMention(int len) {super(len);}
  }
  public static class lengthOfSecondMention extends IntIndicator{
	  public lengthOfSecondMention(int len) {super(len);}
  }
  
  public static class nameAndPronoun extends Indicator{
	  public nameAndPronoun(boolean bothTrue) {super(bothTrue);}
  }
  
  public static class bucketDistance extends BucketIndicator{
	public bucketDistance(int value, int max, int numBuckets) {
		super(value, max, numBuckets);
	}
  }
  public static class bucketLengthDiff extends BucketIndicator{
	public bucketLengthDiff(int value, int max, int numBuckets) {
		super(value, max, numBuckets);
	}
  }
  
  public static class firstWordInSecondCluster extends Indicator{
	  public firstWordInSecondCluster(boolean value) {super(value);}
  }
  
  public static class PluralityMatch extends Indicator{
	  public PluralityMatch(boolean match) {super(match);}
  }
  
  public static class headMatch extends Indicator{
	  public headMatch(boolean match) {super(match);}
  }
  
  public static class POS1 extends StringIndicator{
	  public POS1(String pos) {super(pos);}
  }
  public static class POS2 extends StringIndicator{
	  public POS2(String pos) {super(pos);}
  }
  
  public static class LemmaMatch extends Indicator{
	  public LemmaMatch(boolean match) {super(match);}
  }
  
  /*
   * TODO: Add values to the indicators here.
   */
  
  

}
