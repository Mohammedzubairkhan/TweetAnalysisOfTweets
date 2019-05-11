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

public class TweetGraphServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6741297773865979416L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject nePairing = null;

		String topic = request.getParameter("topic");
		String tid = request.getParameter("tid");
		String tweet = request.getParameter("tweet");

		MongoClient client = new MongoClient();
		DB database = client.getDB("mzk");
		System.out.println(topic);
			DBCollection collection = database.getCollection(topic);
			System.out.println(topic+ "---" +tweet+"~~~~~~~~~~~" +tid);
			Object input1 = collection.findOne("nePairing").get("desc");

			String inputText = new JSON().serialize(input1);

			JSONParser parser = new JSONParser();
			try {
				nePairing = (JSONObject) parser.parse(inputText);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONArray array = (JSONArray) nePairing.get(tid);
			JSONObject tweetGraph = new JSONObject();
			tweetGraph.put("name", tweet);
			String value;
			JSONArray children = new JSONArray();

			for (int i = 0; i < array.size(); i++) {
				JSONObject ob = (JSONObject) array.get(i);
				Set<String> keys = ob.keySet();
				for(String ne: keys) {
					value = (String)ob.get(ne);
					JSONArray childne = new JSONArray();
					JSONObject obj = new JSONObject();
					obj.put("name", ne);
					JSONObject object = new JSONObject();
					object.put("name", value);
					childne.add(object);
					obj.put("children", childne);
					children.add(obj);
				}
			}
			
			tweetGraph.put("children", children);
			System.out.println(tweetGraph);
		client.close();
		
		JSONArray treeArray = new JSONArray();
		treeArray.add(tweetGraph);
		String temp = StringEscapeUtils.escapeEcmaScript(treeArray.toString());
		request.setAttribute("tweetGraph", temp);
		request.setAttribute("topic", topic);
		request.setAttribute("tweet", StringEscapeUtils.escapeEcmaScript(tweet));

		RequestDispatcher rd = request.getRequestDispatcher("/tweetGraph.jsp");
		rd.forward(request, response);
	}

}
