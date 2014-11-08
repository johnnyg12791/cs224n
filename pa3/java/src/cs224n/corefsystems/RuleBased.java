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
		
		/*
		 * First pass: exact string match
		 */
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
		
		/*
		 * Second pass: exact head word match
		 */
		Map<String, Integer> headMatch = new HashMap<String, Integer>();
		for (int i = 0; i < doc.getMentions().size(); i++) {
			Mention m = doc.getMentions().get(i);
			String headWord = m.headWord();
			if(headMatch.containsKey(headWord)) {
				ourMentionMap.put(i, headMatch.get(headWord));
			} else {
				//ourMentionMap.put(i, -1);
				headMatch.put(headWord, i);
			}
		}
		
		/*
		 * Third pass: lemma word match
		 */
		Map<String, Integer> lemmaMatch = new HashMap<String, Integer>();
		for (int i = 0; i < doc.getMentions().size(); i++) {
			Mention m = doc.getMentions().get(i);
			String lemmaWord = m.headToken().lemma();
			if(lemmaMatch.containsKey(lemmaWord)) {
				ourMentionMap.put(i, lemmaMatch.get(lemmaWord));
				//System.out.println("matched lemma");
			} else {
				//ourMentionMap.put(i, -1);
				lemmaMatch.put(lemmaWord, i);
			}
		}
		
		/*
		 * Forth pass: headword substring
		 */
		Map<String, Integer> substringMatch = new HashMap<String, Integer>();
		for (int i = 0; i < doc.getMentions().size(); i++) {
			Mention m = doc.getMentions().get(i);
			String headWord = m.headWord();
			boolean addedToMap = false;
			for (String str : substringMatch.keySet()) {
				if (str.length() > 4 && headWord.length() > 4) {
					if (str.contains(headWord)) {
						ourMentionMap.put(i, substringMatch.get(str));
						addedToMap = true;
					}
					else if (headWord.contains(str)) {
						ourMentionMap.put(i, substringMatch.get(str));
						addedToMap = true;
					}
				}
			}
			if (!addedToMap) {
				substringMatch.put(headWord, i);
			}
		}
		
		/*
		 * Fifth pass: I, me, my, myself, mine etc
		 */
		Set<String> meMySet = new HashSet<String>(Arrays.asList("i", "me", "my", "myself", "mine"));
		checkSimilarThings(meMySet, "i", doc, ourMentionMap);
		
		/*
		 * Sixth pass: us, we, our, ours, ourselves
		 */
		Set<String> usWeSet = new HashSet<String>(Arrays.asList("we", "our", "ours", "ourselves"));
		checkSimilarThings(usWeSet, "we", doc, ourMentionMap);
		
		
//		Map<String, Integer> meMyMatch = new HashMap<String, Integer>();
//		for (int i = 0; i < doc.getMentions().size(); i++) {
//			Mention m = doc.getMentions().get(i);
//			String headWord = m.headToken().lemma().toLowerCase();
//			if (meMySet.contains(headWord)) {
//				System.out.println("We found a meMy word");
//				if (meMyMatch.containsKey("i")) {
//					ourMentionMap.put(i, meMyMatch.get("i"));
//					System.out.println("MEMYMATCH");
//				} else {
//					meMyMatch.put("i", i);
//				}
//			}
//		}
		
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
		
		//System.out.println(ourMentionMap);
		//System.out.println(exactMatch);
		//System.out.println(substringMatch);
		return mentions;
	}
	
	private void checkSimilarThings(Set<String> set, String keyWord, Document doc, Map<Integer, Integer> ourMentionMap) {
		Map<String, Integer> matches = new HashMap<String, Integer>();
		for (int i = 0; i < doc.getMentions().size(); i++) {
			Mention m = doc.getMentions().get(i);
			String headWord = m.headToken().lemma().toLowerCase();
			if (set.contains(headWord)) {
				//System.out.println("We found a meMy word");
				if (matches.containsKey(keyWord)) {
					ourMentionMap.put(i, matches.get(keyWord));
					//System.out.println("MATCH");
				} else {
					matches.put(keyWord, i);
				}
			}
		}
	}
}
