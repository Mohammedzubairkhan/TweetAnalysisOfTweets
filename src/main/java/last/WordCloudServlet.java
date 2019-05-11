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

public class WordCloudServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6741297773865979416L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONArray wordCloud = null;

		String topic = request.getParameter("param");
		MongoClient client = new MongoClient();
		DB database = client.getDB("mzk");
		
			DBCollection collection = database.getCollection(topic);

			Object input1 = collection.findOne("wordCloudNe").get("desc");

			String inputText = new JSON().serialize(input1);

			JSONParser parser = new JSONParser();
			try {
				wordCloud = (JSONArray) parser.parse(inputText);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		client.close();
		
		String temp = StringEscapeUtils.escapeEcmaScript(wordCloud.toString());
		request.setAttribute("wordCloudNe", temp);
		request.setAttribute("topic", topic);

		System.out.println(wordCloud);
		RequestDispatcher rd = request.getRequestDispatcher("/wordCloudNe.jsp");
		rd.forward(request, response);
	}

}
