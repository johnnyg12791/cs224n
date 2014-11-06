package cs224n.corefsystems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.coref.Sentence;
import cs224n.coref.Sentence.Token;
import cs224n.util.Pair;

public class OneCluster implements CoreferenceSystem {

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
	    for(Pair<Document, List<Entity>> pair : trainingData){
	        //--Get Variables
	        Document doc = pair.getFirst();
	        List<Entity> clusters = pair.getSecond();
	        List<Mention> mentions = doc.getMentions();
	        //--Print the Document
//	        System.out.println(doc.prettyPrint(clusters));
	        //--Iterate over mentions
	        for(Mention m : mentions){
//	          System.out.println(m);
	        }
	        //--Iterate Over Coreferent Mention Pairs
	        for(Entity e : clusters){
	        	for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
//	            System.out.println(""+mentionPair.getFirst() + " and " + mentionPair.getSecond() + " are coreferent");
	        	}
	        }
	    }
	}

	@Override
	//We want to assign all mentions to one cluster
	//This one cluster has all the entities
	public List<ClusteredMention> runCoreference(Document doc) {
		List<Mention> mentions = new ArrayList<Mention>(doc.getMentions());
		Entity entity = new Entity(mentions);
		List<ClusteredMention> allMentionsCluster = new ArrayList<ClusteredMention>();
		for(Mention mention : doc.getMentions()){
			ClusteredMention tempCluster = mention.markCoreferent(entity);
			allMentionsCluster.add(tempCluster);
		}
		System.out.println(allMentionsCluster);
		return allMentionsCluster;
	}

}

