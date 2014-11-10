package cs224n.corefsystems;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;


import cs224n.coref.*;
import cs224n.util.CounterMap;
import cs224n.util.Pair;

public class BetterBaseline implements CoreferenceSystem {

	Map<String, Entity> clusterMap;
	Map<Mention, Entity> usedMap;
	CounterMap<String, String> pairMap;

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		clusterMap = new HashMap<String, Entity>();
		usedMap = new HashMap<Mention, Entity>();
		
		pairMap = new CounterMap<String, String>();
		for(Pair<Document, List<Entity>> pair : trainingData){
			//--Get Variables
			Document doc = pair.getFirst();
			List<Entity> clusters = pair.getSecond();
			List<Mention> mentions = doc.getMentions();
			//--Print the Document
			//System.out.println(doc.prettyPrint(clusters));
			//--Iterate over mentions
			for(Mention m : mentions){
				//System.out.println(m);
				
			}
			
			for (Entity e : clusters) {
//				for (Mention m : e.mentions) {
//					clusterMap.put(m.gloss(), e);
//				}
			}
			
			//--Iterate Over Coreferent Mention Pairs
			for(Entity e : clusters){
				for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
					//pairMap.incrementCount(mentionPair.getFirst().headWord(), mentionPair.getSecond().headWord(), 1.0);
					//System.out.println(""+mentionPair.getFirst() + " and " + mentionPair.getSecond() + " are coreferent");
				}
			}
		}
		//System.out.println(clusterMap);
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		//(variables)
		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		
		Map<String, Entity> headMap = new HashMap<String, Entity>();
		//(for each mention...)
		int i = 0;
		for(Mention m : doc.getMentions()){
			//(...get its text)
			//String mentionString = m.gloss();
			String headWord = m.headWord();
			//(...if we've seen this text before...)
			if (headMap.containsKey(headWord)) {
				//System.out.println("Head Word: " + headWord + ", Entity: " + headMap.get(headWord));
				mentions.add(m.markCoreferent(headMap.get(headWord)));
			} else {
				ClusteredMention newCluster = m.markSingleton();
				mentions.add(newCluster);
				headMap.put(headWord, newCluster.entity);
			}

			//System.out.println(mentions);
		}
		//(return the mentions)
		return mentions;
	}

	
	
}
