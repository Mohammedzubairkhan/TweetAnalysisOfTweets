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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

public class AprioriForWords<T> {
	/**
	 * Sort all Set<T> of list
	 * 
	 * @param frequentItemSets a list of set
	 * @return a list of sorted item
	 */
	MongoClient client;
	DB database;
	private String topic;

	public AprioriForWords(MongoClient clientInput, DB databaseInput, String topicInput) {
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
		JSONObject tweetJson = new JSONObject();
		HashMap<String, Integer> tweetMap = new HashMap<>();
		for (Map.Entry<String, Integer> k : count.entrySet()) {
			String score = k.getValue().toString();
			JSONObject wordRep = new JSONObject();
			String word = k.getKey();
			tweetMap.put(word, k.getValue());
			wordRep.put("word", word);
			wordRep.put("weight", score);
			wordCloud.add(wordRep);
		}
		tweetMap = sortByValue(tweetMap);
		int i = 0;
		for (Map.Entry<String, Integer> k : tweetMap.entrySet()) {
			tweetJson.put(String.valueOf(i), k.getKey());
			i++;
		}
		// tweetJson.putAll(tweetMap);
		System.out.println(tweetJson);
		DBCollection collection = database.getCollection(topic);
		DBObject object = new BasicDBObject("_id", "wordCloudword").append("desc", wordCloud);

		collection.insert(object);
		DBObject object1 = new BasicDBObject("_id", "tweetJson").append("desc", tweetJson);
		collection.insert(object1);

		return wordCloud;
	}

	public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm) {
		// Create a list from elements of HashMap
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(hm.entrySet());

		// Sort the list
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		// put data from sorted list to hashmap
		int i = 0;
		HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
		for (Map.Entry<String, Integer> aa : list) {
			temp.put(aa.getKey(), aa.getValue());
			i++;
			if (i == 10) {
				break;
			}

		}
		return temp;
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

	/**
	 * Read transaction data from file
	 * 
	 * @param datasource directory file name
	 * @return List<Set<String>> a list of set transaction
	 * @throws ParseException
	 */
	public List<Set<String>> readTransactions() throws ParseException {

		DBCollection collection = database.getCollection(topic);

		Object input1 = collection.findOne("wordTransactions").get("desc");

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
	/*
	 * public static void main(String[] args) { JSONObject tree = null; String topic
	 * = "Gano"; MongoClient client = new MongoClient(); DB database =
	 * client.getDB("mzk"); AprioriWithTweets<String> serviceImpl = new
	 * AprioriWithTweets<String>(client, database, topic); List<Set<String>> data =
	 * null; try { data = serviceImpl.readTransactions(); } catch (ParseException e)
	 * { e.printStackTrace(); } Map<Set<String>, Integer> frequentItemSets =
	 * serviceImpl.generateFrequentItemSets(data, 2); // add topic at end try { tree
	 * = serviceImpl.printCandidates(frequentItemSets); } catch (ParseException e) {
	 * e.printStackTrace(); } serviceImpl.wordCloudCalculator(frequentItemSets); }
	 */
}