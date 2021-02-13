package last;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

public class TweetPullingServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6741297773865979416L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject tweetScore = null;

		String topic = request.getParameter("param");
		MongoClient client = new MongoClient();
		DB database = client.getDB("mzk");
		
			DBCollection collection = database.getCollection(topic);

			Object input1 = collection.findOne("tweetJson").get("desc");

			String inputText = new JSON().serialize(input1);

			JSONParser parser = new JSONParser();
			try {
				tweetScore = (JSONObject) parser.parse(inputText);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Set<String> tweetScoreSet = new HashSet<>();

			for(int i = 0; i<10; i++)
			{
				tweetScoreSet.add((String)tweetScore.get(String.valueOf(i)));
			}
			TweetsGet tweetsGet = new TweetsGet();
			Map<String, List<String>> allTweets = new HashMap<String, List<String>>();
			List<String> allTweets2 ;
			System.out.println(tweetScoreSet);
			for(String e : tweetScoreSet)
			{
				allTweets2 = tweetsGet.tweetText(e);
				if(allTweets2.contains("nothingMZK")) {
					continue;
				}
				allTweets.put(e, allTweets2);
			}
		client.close();
			
		request.setAttribute("tweetScore", allTweets);
		request.setAttribute("topic", topic);

		//System.out.println(tweetScore);
		RequestDispatcher rd = request.getRequestDispatcher("/tweetsDisplay.jsp");
		rd.forward(request, response);
	}

}
