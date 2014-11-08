package cs224n.corefsystems;

import java.util.*;
import cs224n.coref.*;
import cs224n.util.CounterMap;
import cs224n.util.Pair;

public class RuleBased implements CoreferenceSystem {

	CounterMap<String, Mention> headWordMatches;
	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		headWordMatches = new CounterMap<String, Mention>();
		// TODO Auto-generated method stub
		//Possible that we may want to collect some statistics...?
			//Such as, how often "he" is associated with "him" or something??
			//Or other previous co-references??
		for(Pair<Document, List<Entity>> pair : trainingData){
		      //--Get Variables
		      Document doc = pair.getFirst();
		      List<Entity> clusters = pair.getSecond();
		      List<Mention> mentions = doc.getMentions();
		      //--Print the Document
//		      System.out.println(doc.prettyPrint(clusters));
		      //--Iterate over mentions
		      for(Mention m : mentions){
//		        System.out.println(m);
		      }
		      //--Iterate Over Coreferent Mention Pairs
		      for(Entity e : clusters){
		        for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
		        	//headWordMatches.incrementCount(mentionPair.getFirst().headWord(), mentionPair.getSecond(), 1.0);
//		          System.out.println(""+mentionPair.getFirst() + " and " + mentionPair.getSecond() + " are coreferent");
		        }
		      }
		    }

	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		//First run an exact match
		Map<Integer, Integer> ourMentionMap = new HashMap<Integer, Integer>(); 
		Map<String, Integer> exactMatch = new HashMap<String, Integer>();
		
		//(for each mention...)
		for(int i = 0; i < doc.getMentions().size(); i++){
			Mention m = doc.getMentions().get(i);
			String mentionString = m.gloss();
			if(exactMatch.containsKey(mentionString)){
				ourMentionMap.put(i, exactMatch.get(mentionString));
			} else {
				ourMentionMap.put(i, -1);
				exactMatch.put(mentionString, i);
			}
		}
		
		
		//This is the merge, should come at the very end
		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
		for(int i = 0; i < doc.getMentions().size(); i++){
			Mention m = doc.getMentions().get(i);
			//If it's not associated with anything, mark it as a singleton 
			if(ourMentionMap.get(i) == -1){
				mentions.add(m.markSingleton());
			}else{
				//If it is associated with something, find the index
				int coref = ourMentionMap.get(i);
				ClusteredMention prevCluster = mentions.get(coref);
				mentions.add(m.markCoreferent(prevCluster));
			}
		}
		
		System.out.println(ourMentionMap);
		System.out.println(exactMatch);
		return mentions;
	}
}
