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

	CounterMap<Pair<Mention, Mention>, Entity> coreferentPairs;
	CounterMap<List<String>, Entity> mentionEntityMap;
	Map<List<String>, Set<Entity>> entityListMap;

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		coreferentPairs = new CounterMap<Pair<Mention, Mention>, Entity>();
		mentionEntityMap = new CounterMap<List<String>, Entity>();
		entityListMap = new HashMap<List<String>, Set<Entity>>();
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
			//--Iterate Over Coreferent Mention Pairs
			for(Entity e : clusters){
				for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
					coreferentPairs.incrementCount(mentionPair, e, 1);
					mentionEntityMap.incrementCount(mentionPair.getFirst().text(), e, 1);
					mentionEntityMap.incrementCount(mentionPair.getSecond().text(), e, 1);
					if (entityListMap.containsKey(mentionPair.getFirst())) {
						Set<Entity> entities = entityListMap.get(mentionPair.getFirst());
						entities.add(e);
						//System.out.println(entities);
						entityListMap.put(mentionPair.getFirst().text(), entities);
						//System.out.println("EXISTING");
					} else {
						Set<Entity> entities = new HashSet<Entity>();
						entities.add(e);
						entityListMap.put(mentionPair.getFirst().text(), entities);
						//System.out.println("NEW");
					}

					if (entityListMap.containsKey(mentionPair.getSecond())) {
						Set<Entity> entities = entityListMap.get(mentionPair.getSecond());
						entities.add(e);
						entityListMap.put(mentionPair.getSecond().text(), entities);
					} else {
						Set<Entity> entities = new HashSet<Entity>();
						entities.add(e);
						entityListMap.put(mentionPair.getSecond().text(), entities);
					}

					//System.out.println(""+mentionPair.getFirst() + " and " + mentionPair.getSecond() + " are coreferent");
				}
			}
			//System.out.println(mentionEntityMap);
			//break;
		}
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		// TODO Auto-generated method stub

		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		Set<Entity> usedEntities = new HashSet<Entity>();

		for (Mention m : doc.getMentions()) {
			//			if (m.gloss().equals("God the Protector")) {
			//				System.out.println(m.sentence.parse);
			//				System.out.println(m.parse);
			//				System.out.println(m.beginIndexInclusive + " "+m.endIndexExclusive);
			//				System.out.println(m.gloss());
			//			}

			Set<Entity> entities = entityListMap.get(m.text());
			int max = Integer.MIN_VALUE;
			Entity maxEntity = null;
			//System.out.println(entities);
			String mentionString = m.gloss();
			
			if (clusters.containsKey(mentionString)) {
				mentions.add(m.markCoreferent(clusters.get(mentionString)));
				//System.out.println("FOUND");
			} else {
				ClusteredMention newCluster;
				if (entities != null) {
					for (Entity e : entities) {
						if (mentionEntityMap.getCount(m.text(), e) > max) {
							max = (int) mentionEntityMap.getCount(m.text(), e);
							maxEntity = e;
						}
						//System.out.println(mentionEntityMap.getCount(m.text(), e));
					}
					if (maxEntity != null) {
						if (usedEntities.contains(maxEntity)) {
							newCluster = m.markSingleton();
							System.out.println("Already used Entity!");
						} else {
							newCluster = m.markCoreferent(maxEntity);
						}
						usedEntities.add(maxEntity);
						//mentions.add(m.markCoreferent(maxEntity));
					} else { 
						newCluster = m.markSingleton();
						//mentions.add(m.markSingleton());
					}
				} else {
					newCluster = m.markSingleton();
					//mentions.add(m.markSingleton());
				}
				mentions.add(newCluster);
				clusters.put(mentionString, newCluster.entity);
			}
			//break;
		}
		//System.out.println(mentions);
		return mentions;
	}

	
	
}
