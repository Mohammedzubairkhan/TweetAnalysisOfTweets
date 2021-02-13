package last;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.List;

/**
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @since Twitter4J 2.1.7
 */
public class test {
    /**
     * Usage: java twitter4j.examples.search.SearchTweets [query]
     *
     * @param args search query
     */
	
    public static void main(String[] args) {
       /* if (args.length < 1) {
            System.out.println("java twitter4j.examples.search.SearchTweets [query]");
            System.exit(-1);
        }*/
    	ConfigurationBuilder cb = new ConfigurationBuilder();
    	cb.setDebugEnabled(true)
    	  .setOAuthConsumerKey("E5zwJQzDCOVZyBaVTukidPtpl")
    	  .setOAuthConsumerSecret("AS7T8xDsGYsKtWnyJZNlXlcGYd2LVrWic5rcDSkQwGdQmv4KJ2")
    	  .setOAuthAccessToken("789405330555809792-3brOp5QwV3i493dzPFlReiLJU30FYvw")
    	  .setOAuthAccessTokenSecret("O6H9NodCNGb3QDOthgEomM81pRQbYb2jlh4dgRuc430KX");
    	TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();
        try {
            Query query = new Query("srk");
            QueryResult result;
            do {
                result = twitter.search(query);
                List<Status> tweets = result.getTweets();
                for (Status tweet : tweets) {
                    System.out.println("@" + tweet.getUser().getScreenName() + " - " + tweet.getText());
                }
            } while ((query = result.nextQuery()) != null);
            System.exit(0);
        } catch (TwitterException te) {
            te.printStackTrace();
            //System.out.println("Failed to search tweets: " + te.getMessage());
            System.exit(-1);
        }
    }
}