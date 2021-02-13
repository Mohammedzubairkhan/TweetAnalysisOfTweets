package last;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

public class TweetsGet {
    /**
     * Usage: java twitter4j.examples.search.SearchTweets [query]
     *
     * @param args search query
     */
	public List<String> tweetText(String search){
		ConfigurationBuilder cb = new ConfigurationBuilder();
    	cb.setDebugEnabled(true)
    	  .setOAuthConsumerKey("E5zwJQzDCOVZyBaVTukidPtpl")
    	  .setOAuthConsumerSecret("AS7T8xDsGYsKtWnyJZNlXlcGYd2LVrWic5rcDSkQwGdQmv4KJ2")
    	  .setOAuthAccessToken("789405330555809792-3brOp5QwV3i493dzPFlReiLJU30FYvw")
    	  .setOAuthAccessTokenSecret("O6H9NodCNGb3QDOthgEomM81pRQbYb2jlh4dgRuc430KX");
    	//cb.setUseSSL(true);
    	TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();
        List<String> tweetsList = new ArrayList<String>();
        int i=0;
        try {
            Query query = new Query(search);
            QueryResult result;
            System.out.println(query);
                result = twitter.search(query);
                List<Status> tweets = result.getTweets();
                for (Status tweet : tweets) {
                    System.out.println(tweet.getText());
                    tweetsList.add(StringEscapeUtils.escapeEcmaScript(tweet.getText()));
                    i++;
                    if(i == 3)
                    {
                    	break;
                    }
                }
           
        } catch (TwitterException te) {
        	tweetsList.add("nothingMZK");
            te.printStackTrace();
            //System.out.println("Failed to search tweets: " + te.getMessage());
            
        }
        
        return tweetsList;
		
	}
    
}