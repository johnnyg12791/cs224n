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
		        	headWordMatches.incrementCount(mentionPair.getFirst().headWord(), mentionPair.getSecond(), 1.0);
//		          System.out.println(""+mentionPair.getFirst() + " and " + mentionPair.getSecond() + " are coreferent");
		        }
		      }
		    }

	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		//System.out.println(headWordMatches);
		//First run an exact match
		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		//(for each mention...)
		for(Mention m : doc.getMentions()){
			String mentionString = m.gloss();
			//(...if we've seen this text before...)
			if(clusters.containsKey(mentionString)){
				//(...add it to the cluster)
				//System.out.println(m.sentence);
				//System.out.println(m + " : " + mentionString);
				mentions.add(m.markCoreferent(clusters.get(mentionString)));
			} else {
				//It's not an exact match, so what do we do now
				//Look at what the head of this mention mapped to in our training examples
				Set<Mention> previousCorefMentions = headWordMatches.getCounter(m.headWord()).keySet();
				for(Mention curMention : previousCorefMentions){
					System.out.println(curMention.gloss());
					//if our curMention is in the doc.getMentions, mark it coref with what
					System.out.println("");
					
					
					if(doc.getMentions().contains(curMention)){
						System.out.println("Want to mark: " + curMention.gloss() + " and " + mentionString);
						//mentions.add(m.markCoreferent(otherMention));
					}
				}
				
				//(...else create a new singleton cluster)
				ClusteredMention newCluster = m.markSingleton();
				mentions.add(newCluster);
				clusters.put(mentionString,newCluster.entity);
			}
		
		//Then we run a less exact match, with he/his/him and she/her/hers
		/*for(Mention m : doc.getMentions()){
			String mentionString = m.gloss();
			
		}*/
		}
		for(Mention m : doc.getMentions()){
			
		}
		
		return mentions;
	}
}
