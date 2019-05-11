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

public class NamedEntitySecond extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6741297773865979416L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject tree2 = null;
		String topic = request.getParameter("param");

		System.out.println("topic = "+ topic);
		boolean containFlag = false;
		MongoClient client = new MongoClient();
		DB database = client.getDB("mzk");
		Set<String> collectionNames = database.getCollectionNames();
		
			DBCollection collection = database.getCollection(topic);
			Object input2 = collection.findOne("treeOutput2").get("desc");

			String inputText2 = new JSON().serialize(input2);
			JSONParser parser = new JSONParser();
			try {
				tree2 = (JSONObject) parser.parse(inputText2);

			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
		client.close();
		
		JSONArray treeArray2 = new JSONArray();
		treeArray2.add(tree2);
		String temp2 = StringEscapeUtils.escapeEcmaScript(treeArray2.toString());
		request.setAttribute("tree2", temp2);
		request.setAttribute("topic", topic);

		System.out.println(temp2);
		RequestDispatcher rd = request.getRequestDispatcher("/graphPage2.jsp");
		rd.forward(request, response);
	}

}
