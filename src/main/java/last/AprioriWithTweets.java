package last;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.BasicConfigurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

import last.Combination;

public class AprioriWithTweets<T> {
	/**
	 * Sort all Set<T> of list
	 * 
	 * @param frequentItemSets a list of set
	 * @return a list of sorted item
	 */
	MongoClient client;
	DB database;
	private String topic;
	
	public AprioriWithTweets(MongoClient clientInput, DB databaseInput, String topicInput) {
		client = clientInput;
		database = databaseInput;
		topic = topicInput;
	}
	Map<Set<T>, Set<Integer>> idReferenceForTweets = new HashMap<Set<T>, Set<Integer>>();
	Map<Set<String>, Set<Integer>> transMap = new HashMap<Set<String>, Set<Integer>>();
	JSONObject tweetsObject = null;
	JSONObject pairObject = null;
	public List<List<T>> sortList(List<Set<T>> frequentItemSets) {

		List<List<T>> list = new ArrayList<List<T>>();
		Set<T> treeSet = null;
		for (Set<T> item : frequentItemSets) {
			treeSet = new TreeSet<T>(item);
			list.add(new ArrayList<T>(treeSet));
		}
		return list;
	}

	/**
	 * Find frequent items at the first generated
	 * 
	 * @param transactions a list of transaction from database
	 * @param minSupport   min support
	 * @return a map contains candidate and its support count
	 */

	public Map<Set<T>, Integer> findFrequent1Itemsets(List<Set<T>> transactions, int minSupport) {
		Map<Set<T>, Integer> supportMap = new HashMap<Set<T>, Integer>();
		
		for (Set<T> transaction : transactions) {
			// Using Set collection to avoid duplicate items per transaction
			Set<Integer> valueId = transMap.get(transaction);
			for (T item : transaction) {
				Set<T> temp = new HashSet<T>();
				temp.add(item);
				// Count support for each item
				if (supportMap.containsKey(temp)) {
					supportMap.put(temp, supportMap.get(temp) + 1);

					Set<Integer> valueSetId = idReferenceForTweets.get(temp);

					valueSetId.addAll(valueId);
					idReferenceForTweets.put(temp, valueSetId);
				} else {
					supportMap.put(temp, 1);
					Set<Integer> value = new HashSet<Integer>(valueId);

					idReferenceForTweets.put(temp, value);
				}
			}
		}
		// Remove non-frequent candidates basing on support count threshold.
		return eliminateNonFrequentCandidate(supportMap, minSupport);
	}

	/**
	 * Eliminate candidates that are infrequent, leaving only those that are
	 * frequent
	 * 
	 * @param candidates a map that contains candidates and its support count
	 * @param minSupport
	 * @return candidates and it support count >= minSupport
	 */
	private Map<Set<T>, Integer> eliminateNonFrequentCandidate(Map<Set<T>, Integer> candidates, int minSupport) {
		Map<Set<T>, Integer> frequentCandidates = new HashMap<Set<T>, Integer>();

		for (Map.Entry<Set<T>, Integer> candidate : candidates.entrySet()) {
			if (candidate.getValue() >= minSupport) {
				frequentCandidates.put(candidate.getKey(), candidate.getValue());

			} else {

				idReferenceForTweets.remove(candidate.getKey());
			}
		}
		return frequentCandidates;
	}

	/**
	 * Generate frequent item sets
	 * 
	 * @param transactionList a list of transactions from database
	 * @param minSupport      minimum support
	 * @return candidates satisfy minimum support
	 */

	public Map<Set<T>, Integer> generateFrequentItemSets(List<Set<T>> transactionList, int minSupport) {

		Map<Set<T>, Integer> supportCountMap = new HashMap<Set<T>, Integer>();
		// Find all frequent 1-item sets
		Map<Set<T>, Integer> frequent1ItemMap = findFrequent1Itemsets(transactionList, minSupport);
		List<Set<T>> frequentItemList = new ArrayList<Set<T>>(frequent1ItemMap.keySet());

		Map<Integer, List<Set<T>>> map = new HashMap<Integer, List<Set<T>>>();
		map.put(1, frequentItemList);

		int k = 1;
		for (k = 2; !map.get(k - 1).isEmpty(); k++) {

			// First generate the candidates.
			List<Set<T>> candidateList = aprioriGenerate(map.get(k - 1));
			// Scan D for counts
			for (Set<T> transaction : transactionList) {
				// Get the subsets of t that are present in transaction
				List<Set<T>> candidateList2 = subSets(candidateList, transaction);

				for (Set<T> itemset : candidateList2) {
					// Increase support count
					int count = supportCountMap.get(itemset) == null ? 1 : supportCountMap.get(itemset) + 1;
					supportCountMap.put(itemset, count);

				}
			}
			// checking support of each item in supportCountMap an if greater than
			// minSupport then adding
			map.put(k, extractNextFrequentCandidates(candidateList, supportCountMap, minSupport));
		}
		return getFrequentItemsets(map, supportCountMap, frequent1ItemMap);
	}

	/**
	 * Generate rules with minimum confidence
	 * 
	 * @param frequentItemCounts candidates and its support counts
	 * @param minConf            minimum confidence
	 */
	int combinate(int n, int r) {
		int num = factorial(n);
		int deno = (factorial(n - r) * factorial(r));

		return num / deno;
	}

	int factorial(int n) {
		for (int i = n - 1; i > 1; i--)
			n = n * i;
		return n;
	}


	public JSONArray wordCloudCalculator(Map<Set<String>, Integer> frequentItemCounts) {
		Map<String, Integer> count = new HashMap<String, Integer>();
		JSONArray wordCloud = new JSONArray();

		String str = null;
		int value = 0, ssize = 0, comb = 0;
		Float per = (float) 0.0;
		for (Set<String> itemsets : frequentItemCounts.keySet()) {
			// Generate for frequent k-itemset >= 2
			ssize = itemsets.size();
			itemsets.remove("");

			if (ssize >= 2) {
				value = 0;
				comb = 0;
				for (int j = 1; j < ssize; j++)
					comb = comb + combinate(ssize, j);
				per += comb;
				for (String key : itemsets) {
					if (!count.containsKey(key))
						count.put(key, 0);

					value = count.get(key) + comb;
					count.put(key, value);
				}

				}

			// System.out.println(count);
			// writing to file
			// fl.println(count);
		}
		for(Map.Entry<String, Integer> k : count.entrySet()) {
			String score = k.getValue().toString();
			JSONObject wordRep = new JSONObject();
			String word = k.getKey();
			wordRep.put("word", word);
			wordRep.put("weight", score);
			wordCloud.add(wordRep);
		}
		System.out.println(wordCloud);
		DBCollection collection = database.getCollection(topic);
		DBObject object = new BasicDBObject("_id", "wordCloudNe").append("desc", wordCloud);

		collection.insert(object);
		return wordCloud;
	}
	/**
	 * Get frequent items set from the first generated and support count map
	 * 
	 * @param map              contains iterator index and its list of items
	 * @param supportCountMap  support count map
	 * @param frequent1ItemMap frequent item set getting from the first generated
	 * @return a map that key is set of items and value is support countmap
	 */
	private Map<Set<T>, Integer> getFrequentItemsets(Map<Integer, List<Set<T>>> map,
			Map<Set<T>, Integer> supportCountMap, Map<Set<T>, Integer> frequent1ItemMap) {

		Map<Set<T>, Integer> temp = new HashMap<Set<T>, Integer>();
		temp.putAll(frequent1ItemMap);
		for (List<Set<T>> itemsetList : map.values()) {
			for (Set<T> itemset : itemsetList) {
				if (supportCountMap.containsKey(itemset)) {
					temp.put(itemset, supportCountMap.get(itemset));
					additionToReference(itemset);
				}
			}
		}
		return temp;
	}

	// Tweets id analysis......
	private void additionToReference(Set<T> itemset) {
		Set<Integer> Ids = new HashSet<Integer>();

		for (Set<String> s : transMap.keySet()) {
			if (s.containsAll(itemset)) {

				Ids.addAll(transMap.get(s));
			}

		}

		idReferenceForTweets.put(itemset, Ids);

	}

	/**
	 * Extract next frequent candidates
	 * 
	 * @param candidateList
	 * @param supportCountMap support count map
	 * @param support         a minimum support
	 * @return a list of unique items
	 */
	private List<Set<T>> extractNextFrequentCandidates(List<Set<T>> candidateList, Map<Set<T>, Integer> supportCountMap,
			int support) {

		List<Set<T>> rs = new ArrayList<Set<T>>();

		for (Set<T> itemset : candidateList) {
			if (supportCountMap.containsKey(itemset)) {
				int supportCount = supportCountMap.get(itemset);
				if (supportCount >= support) {
					rs.add(itemset);
				}
			}
		}
		return rs;
	}

	/**
	 * Get subset that contains in transaction
	 * 
	 * @param candidateList
	 * @param transaction   a set of transaction from database
	 * @return List<Set<T>> a subset
	 */
	private List<Set<T>> subSets(List<Set<T>> candidateList, Set<T> transaction) {
		List<Set<T>> rs = new ArrayList<Set<T>>();
		for (Set<T> candidate : candidateList) {
			List<T> temp = new ArrayList<T>(candidate);
			if (transaction.containsAll(temp)) {
				rs.add(candidate);
			}
		}
		return rs;
	}

	/**
	 * Main process of apriori generated candidate
	 * 
	 * @param frequentItemSets a list of items
	 * @return A list of item without duplicated
	 */
	public List<Set<T>> aprioriGenerate(List<Set<T>> frequentItemSets) {

		List<Set<T>> candidatesGen = new ArrayList<Set<T>>();
		// Make sure that items within a transaction or itemset are sorted in
		// lexicographic order
		List<List<T>> sortedList = sortList(frequentItemSets);
		// Generate itemSet from L(k-1)
		for (int i = 0; i < sortedList.size(); ++i) {
			for (int j = i + 1; j < sortedList.size(); ++j) {
				// Check condition L(k-1) joining with itself
				if (isJoinable(sortedList.get(i), sortedList.get(j))) {
					// join step: generate candidates
					Set<T> candidate = tryJoinItemSets(sortedList.get(i), sortedList.get(j));
					if (hasFrequentSubSet(candidate, frequentItemSets)) {
						// Add this candidate to C(k)
						candidatesGen.add(candidate);

					}
				}
			}
		}
		return candidatesGen;
	}

	/**
	 * 
	 * @param candidate        a list of item
	 * @param frequentItemSets set of frequent items
	 * @return true if candidate has subset of frequent Item set, whereas false
	 */
	public boolean hasFrequentSubSet(Set<T> candidate, List<Set<T>> frequentItemSets) {
		Combination<T> c = new Combination<T>();
		List<T> list = new ArrayList<T>(candidate);
		int k = candidate.size() - 1;
		boolean whatAboutIt = true;
		// Generate subset s of c candidate
		Set<List<T>> subsets = c.combination(list, k);
		for (List<T> s : subsets) {
			Set<T> temp = new HashSet<T>(s);
			if (!frequentItemSets.contains(temp)) {
				whatAboutIt = false;
				break;
			}
		}
		return whatAboutIt;
	}

	/**
	 * Try to join two list of items
	 * 
	 * @param itemSet1 a list of items
	 * @param itemSet2 a list of items
	 * @return Set<T> a set of item (no duplicated)
	 */
	public Set<T> tryJoinItemSets(List<T> itemSet1, List<T> itemSet2) {

		Set<T> joinItemSets = new TreeSet<T>();
		int size = itemSet1.size();
		for (int i = 0; i < size - 1; ++i) {
			joinItemSets.add(itemSet1.get(i));
		}
		joinItemSets.add(itemSet1.get(size - 1));
		joinItemSets.add(itemSet2.get(size - 1));

		return joinItemSets;

	}

	/**
	 * Check condition to join two list of items
	 * 
	 * @param list1 a list of item
	 * @param list2 a list of item
	 * @return true if being able to join, otherwise false
	 */
	public boolean isJoinable(List<T> list1, List<T> list2) {
		int length = list1.size();
		// Make sure that size of two lists are equal
		if (list1.size() != list2.size())
			return false;
		// Check condition list1[k-1] < list2[k-1] simply ensures that no
		// duplicates are generated
		if (list1.get(length - 1).equals(list2.get(length - 1))) {
			return false;
		}
		// Check members of list1 and list2 are joined if condition list1[k-2] =
		// list2[k-2]
		for (int k = 0; k < length - 1; k++) {
			if (!list1.get(k).equals(list2.get(k))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Print out screen all frequent items
	 * 
	 * @param frequentItemSets a set of frequent items
	 * @throws ParseException
	 */
	public JSONArray mappingMaker(Set<Set<String>> selectedOne, Set<String> overOne) {
		Iterator<Set<String>> sameEvaluate = selectedOne.iterator();
		JSONArray childReturn = new JSONArray();
		System.out.println("new call" +selectedOne+overOne);
		while(sameEvaluate.hasNext())
		{
			if(sameEvaluate.next().equals(overOne)) {
				sameEvaluate.remove();
				System.out.println("complete" + overOne);
				JSONObject objChild = new JSONObject();
				objChild.put("name", "Tweets");
				objChild.put("children", tweetsReturner(overOne));
				System.out.println("complete2" + objChild);

				childReturn.add(objChild);
				break;
			}
		}
		sameEvaluate = selectedOne.iterator();
		
		while(!selectedOne.isEmpty()) {
			String child = null;
			System.out.println("set ---" +selectedOne );
			System.out.println("overOne = "+overOne);

			Set<String> temp = selectedOne.iterator().next();
			System.out.println(temp);
			Iterator<String> it = temp.iterator();
			
			//find the string not in overOnes
			while(it.hasNext()) {
				String itValue = it.next();
				System.out.println("itvalue "+itValue);

				if(!overOne.contains(itValue)) {
					child = itValue;
					break;
					}
			}
			System.out.println("child = "+child);
			overOne.add(child);
			Iterator<Set<String>> sameEvaluate2 = selectedOne.iterator();
			Set<Set<String>> selectedOnes2 = new HashSet<Set<String>>();
		    System.out.println(child);
		    
			while(sameEvaluate2.hasNext()) {
				Set<String> se2Value = sameEvaluate2.next();
			    System.out.println("se2value" + se2Value);

				if(se2Value.contains(child))
				{
				    selectedOnes2.add(se2Value);
				    System.out.println("9999" + se2Value);
					sameEvaluate2.remove();
					//selectedOne.remove(sameEvaluate2.next());
				}
			}
			
			JSONObject objChild = new JSONObject();
			objChild.put("name", child);
			Set<String> overOne2 = new HashSet<String>(overOne);
			objChild.put("children", mappingMaker(selectedOnes2, overOne2));
		    System.out.println("out of call" + selectedOne+"\n+"+overOne);
		    overOne.remove(child);
			childReturn.add(objChild);	
			System.out.println(childReturn);
		}
		return childReturn;
	}
	private JSONArray tweetsReturner(Set<String> overOne) {
		Set<Integer> TweetIds = idReferenceForTweets.get(overOne);
		JSONArray tweetsArray = new JSONArray();
		for(Integer id : TweetIds)
		{
			String key = String.valueOf(id);
			String tweet = (String) tweetsObject.get(key);
			JSONObject o = new JSONObject();
			o.put("name", tweet);
			JSONArray children = new JSONArray();
			JSONArray array = (JSONArray) pairObject.get(key);
			String alt = null;
			for (String ne : overOne) {
				for (int i = 0; i < array.size(); i++) {
					JSONObject ob = (JSONObject) array.get(i);
					if (ob.containsKey(ne)) {
						alt = (String) ob.get(ne);
					}
				}
				JSONObject neJson = new JSONObject();
				neJson.put("name", ne);
				JSONObject altJson = new JSONObject();
				altJson.put("name", alt);
				JSONArray childne = new JSONArray();
				childne.add(altJson);
				neJson.put("children", childne);
				children.add(neJson);
			}
			o.put("children", children);
			
			tweetsArray.add(o);
		}
		// TODO Auto-generated method stub
		return tweetsArray;
	}

	public JSONObject printCandidates(Map<Set<String>, Integer> frequentItemSets) throws ParseException {

		// Accessing tweets json
		DBCollection collection = database.getCollection("tweets");
		Object input = collection.findOne(topic).get("desc");
		String inputText = new JSON().serialize(input);
		JSONParser parser = new JSONParser();
		tweetsObject = (JSONObject) parser.parse(inputText);

		// Accessing NE-word pair json
		collection = database.getCollection(topic);
		input = collection.findOne("nePairing").get("desc");
		inputText = new JSON().serialize(input);
		pairObject = (JSONObject) parser.parse(inputText);

		Set<String> EntitiesKeysPresent = new HashSet<String>();

		JSONObject graph = new JSONObject();
		graph.put("name", "Everything");
		JSONArray children = new JSONArray();
		String str;
		boolean flag = false;
		boolean flag1 = true;
		boolean tweetFlag = false;
		boolean neFlag = false;
		
		//graph preparation code for tree mapping tree
		JSONObject graph2 = new JSONObject();
		graph2.put("name", "Everything");
		JSONArray childrenNe = new JSONArray();
		Map<Set<String>, Integer> frequentSetsRemove = new HashMap<Set<String>, Integer>(frequentItemSets);
		Iterator<Map.Entry<Set<String>, Integer>> iterator = frequentSetsRemove.entrySet().iterator();
		while(!frequentSetsRemove.isEmpty()) {
			Map.Entry<Set<String>, Integer> nextEntry = frequentSetsRemove.entrySet().iterator().next();
		    System.out.println("it1--"+nextEntry);

			String firstItem = nextEntry.getKey().iterator().next();
			Set<Set<String>> selectedOnes = new HashSet<Set<String>>();
			Set<String> overOnes = new HashSet<String>();
			overOnes.add(firstItem);
		    System.out.println(firstItem);

			Iterator<Map.Entry<Set<String>, Integer>> iterator2 = frequentSetsRemove.entrySet().iterator();

			while(iterator2.hasNext()) {
				Map.Entry<Set<String>, Integer> nextEntry2 = iterator2.next();

				boolean k = nextEntry2.getKey().contains(firstItem);
				   System.out.println("k=="+k);

				   if(k) {
					   
					   System.out.println(nextEntry2.getKey().contains(firstItem));
				    System.out.println("it2--"+nextEntry2.getKey());
					selectedOnes.add(nextEntry2.getKey());
				iterator2.remove();
					//	frequentSetsRemove.remove(iterator2.next().getKey());
				}
			}
		    System.out.println(selectedOnes);

			JSONArray child = mappingMaker(selectedOnes, overOnes);
			JSONObject firstJson = new JSONObject();
			firstJson.put("name", firstItem);
			firstJson.put("children", child);
			childrenNe.add(firstJson);
		}
		graph2.put("children", childrenNe);
		System.out.println(graph2);
		
		
		for (Map.Entry<Set<String>, Integer> candidate : frequentItemSets.entrySet()) {
			Set<String> EntitiesKeysNew = new HashSet<String>();
			Set<Integer> TweetIds = idReferenceForTweets.get(candidate.getKey());
			for (String eachNE : candidate.getKey()) {
				if (!EntitiesKeysPresent.contains(eachNE)) {
					EntitiesKeysNew.add(eachNE);
				}

			}

			for (String newNE : EntitiesKeysNew) {
				JSONObject e = new JSONObject();
				e.put("name", newNE);
				JSONArray falsy = new JSONArray();
				JSONObject f = new JSONObject();
				f.put("false", "null");
				falsy.add(f);
				e.put("children", falsy);
				children.add(e);
				EntitiesKeysPresent.add(newNE);
			}

			for (Integer temp : TweetIds) {
				// finding tweet based on id
				String key = String.valueOf(temp);
				String Tweet = (String) tweetsObject.get(key);
				tweetFlag = false;
				// finding the exact name of the entities
				JSONArray array = (JSONArray) pairObject.get(key);
				Set<String> NamedEntities = candidate.getKey();

				for (String ne : NamedEntities) {
					neFlag = false;
					String alt = null;
					for (int i = 0; i < array.size(); i++) {
						JSONObject ob = (JSONObject) array.get(i);
						if (ob.containsKey(ne)) {
							alt = (String) ob.get(ne);
							break;
						}
					}
					flag = false;

					for (int i = 0; (i < children.size()) || flag1; i++) {
						flag1 = false;
						JSONObject nep = (JSONObject) children.get(i);
						if (nep.containsValue(ne)) {
							JSONArray childIn = (JSONArray) nep.get("children");
							int g = 0, j = 0;
							for (j = 0; (j < childIn.size() || childIn.isEmpty()); j++) {
								JSONObject exactNE = (JSONObject) childIn.get(j);

								if (exactNE.containsValue(alt)) {
									neFlag = true;
									JSONArray childTweets = (JSONArray) exactNE.get("children");
									int u = 0, k = 0;
									for (k = 0; k < childTweets.size(); k++) {
										JSONObject tweetObj = (JSONObject) childTweets.get(k);

										if (tweetObj.containsValue(Tweet)) {
											tweetFlag = true;
											break;

										}

									}

									if (!tweetFlag) {

										JSONObject tweetNew = new JSONObject();
										tweetNew.put("name", Tweet);
										tweetNew.put("tid", key);
										childTweets.add(tweetNew);
										exactNE.remove("children");
										exactNE.put("children", childTweets);
										childIn.remove(j);
										childIn.add(exactNE);

										nep.remove("children");
										nep.put("children", childIn);

										children.remove(i);
										children.add(nep);

										flag = true;

									}

								}

								if (flag)
									break;
							}
							if (!neFlag) {
								JSONObject e = new JSONObject();
								e.put("name", alt);
								JSONArray ChildTweets = new JSONArray();
								JSONObject tweetNew = new JSONObject();
								tweetNew.put("name", Tweet);
								tweetNew.put("tid", key);
								ChildTweets.add(tweetNew);
								e.put("children", ChildTweets);
								childIn.add(e);
								JSONObject ip = new JSONObject();
								ip.put("false", "null");
								Object o = childIn.remove(ip);
								nep.remove("children");
								nep.put("children", childIn);
								children.remove(i);
								children.add(nep);
								flag = true;
							}

						}
						if (flag)
							break;

					}
				}

			}

		}
		graph.put("children", children);
		DBObject object = new BasicDBObject("_id", "treeOutput").append("desc", graph);
		DBObject object2 = new BasicDBObject("_id", "treeOutput2").append("desc", graph2);

		collection.insert(object);
		collection.insert(object2);

		return graph;
		
	}

	/**
	 * Read transaction data from file
	 * 
	 * @param datasource directory file name
	 * @return List<Set<String>> a list of set transaction
	 * @throws ParseException
	 */
	public List<Set<String>> readTransactions() throws ParseException {

		DBCollection collection = database.getCollection(topic);

		Object input1 = collection.findOne("mainTransactions").get("desc");

		String inputText = new JSON().serialize(input1);

		JSONParser parserIn = new JSONParser();
		JSONObject obj = (JSONObject) parserIn.parse(inputText);
		List<Set<String>> transactions = new ArrayList<Set<String>>();
		Set<String> keys = obj.keySet();
		for (String key : keys) {

			String tweet = (String) obj.get(key);
			List<String> list = new ArrayList<String>(Arrays.asList(tweet.split(" ")));
			list.removeAll(Arrays.asList("", null));
			Set<String> temp = new HashSet<String>(list);
			transactions.add(temp);
			if (transMap.containsKey(temp)) {
				Set<Integer> old = transMap.get(temp);
				old.add(Integer.valueOf(key));
				transMap.put(temp, old);
			} else {
				Set<Integer> old = new HashSet<Integer>();
				old.add(Integer.valueOf(key));
				transMap.put(temp, old);
			}

		}

		/*
		 * while ((basket = bufferedReader.readLine()) != null) { String items[] =
		 * basket.split(" "); transactions.add(new
		 * HashSet<String>(Arrays.asList(items))); } // Always close files.
		 * bufferedReader.close();
		 */

		return transactions;
	}
/*public static void main(String[] args) {
	JSONObject tree = null;
	String topic = "Gano";
	MongoClient client = new MongoClient();
	DB database = client.getDB("mzk");
	AprioriWithTweets<String> serviceImpl = new AprioriWithTweets<String>(client, database, topic);
	List<Set<String>> data = null;
	try {
		data = serviceImpl.readTransactions();
	} catch (ParseException e) {
		e.printStackTrace();
	}
	Map<Set<String>, Integer> frequentItemSets = serviceImpl.generateFrequentItemSets(data, 2);
	// add topic at end
	try {
		 tree = serviceImpl.printCandidates(frequentItemSets);
	} catch (ParseException e) {
		e.printStackTrace();
	}
	serviceImpl.wordCloudCalculator(frequentItemSets);
}*/
}