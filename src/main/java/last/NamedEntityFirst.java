package last;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public class NamedEntityFirst extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6741297773865979416L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject tree = null;
		JSONObject tree2 = null;

		String topic = request.getParameter("param");
		boolean containFlag = false;
		MongoClient client = new MongoClient();
		DB database = client.getDB("mzk");
		Set<String> collectionNames = database.getCollectionNames();
		/*String specialCharacters=" !#$%&'()*+,-./:;<=>?@[]^`{|}";
		System.out.println("tpic : " + topic);
		if(specialCharacters.contains(Character.toString(topic.charAt(0))))
				topic = topic.substring(1);*/
		if (!collectionNames.contains(topic)) {
			NamedEntityAndRelativeWordsTweetId namedEntityObject = new NamedEntityAndRelativeWordsTweetId(client,
					database, topic);
			try {
				namedEntityObject.evaluate();
			} catch (ParseException e1) {
				e1.printStackTrace();
			}

			AprioriWithTweets<String> serviceImpl = new AprioriWithTweets<String>(client, database, topic);
			AprioriForWords<String> serviceWordsImpl =  new AprioriForWords<String>(client, database, topic);
			List<Set<String>> data = null;
			List<Set<String>> data2 = null;

			try {
				data = serviceImpl.readTransactions();
				data2 = serviceWordsImpl.readTransactions();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			Map<Set<String>, Integer> frequentItemSets = serviceImpl.generateFrequentItemSets(data, 2);
			Map<Set<String>, Integer> frequentItemSetsWords = serviceWordsImpl.generateFrequentItemSets(data2, 2);

			serviceImpl.wordCloudCalculator(frequentItemSets);
			serviceWordsImpl.wordCloudCalculator(frequentItemSetsWords);
			// add topic at end
			try {
				 tree = serviceImpl.printCandidates(frequentItemSets);
				 DBCollection collection = database.getCollection(topic);
				 Object input1 = collection.findOne("treeOutput2").get("desc");

					String inputText = new JSON().serialize(input1);

					JSONParser parser = new JSONParser();
					try {
						tree2 = (JSONObject) parser.parse(inputText);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				 
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		else {
			DBCollection collection = database.getCollection(topic);

			Object input1 = collection.findOne("treeOutput").get("desc");

			String inputText = new JSON().serialize(input1);

			JSONParser parser = new JSONParser();
			try {
				tree = (JSONObject) parser.parse(inputText);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		client.close();
		//System.out.println(tree);
		JSONArray treeArray = new JSONArray();
		treeArray.add(tree);
		String temp = StringEscapeUtils.escapeEcmaScript(treeArray.toString());
		request.setAttribute("tree", temp);
		JSONArray treeArray2 = new JSONArray();
		treeArray.add(tree2);
		String temp2 = StringEscapeUtils.escapeEcmaScript(treeArray.toString());
		request.setAttribute("tree2", temp2);
		request.setAttribute("topic", topic);

		System.out.println(temp2);
		RequestDispatcher rd = request.getRequestDispatcher("/graphPage.jsp");
		rd.forward(request, response);
	}

}
