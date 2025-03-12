package twitter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.dto.user.User;
import timedelayqueue.TimeDelayQueue;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/* Class Description:
 * Obtains posts made by a specific user, which may also have to match a given pattern.
 *
 * Rep Invariant & Abstraction Function:
 * credentialsFile =! null && credentialsFile.contains(apiKey, apiSecretKey, accessToken, accessTokenSecret)
 *
 * Thread Safety Argument:
 * Not neccessary since TimeDelayQueue deals with thread safety
 * */
public class TwitterListener {

    TwitterClient twitter;
    private static final LocalDateTime OCT_1_2022 = LocalDateTime.parse("2022-10-01T00:00:00");

    private Map<User, List<String>> subscribers;
    //Who they are subscribed to matched to the patterns they are subscribed to.



    /**
     * Create a new TwitterListener
     *
     * @param credentialsFile is a file containing a valid apiKey, apiSecretKey, accessToken, and accessTokenSecret
     */
    public TwitterListener(File credentialsFile) {
        this.twitter = new TwitterClient(TwitterClient.getAuthentication(credentialsFile));
        this.subscribers = new HashMap<>();
        // ... add other elements ...
    }

    /**
     * Add a subscription for all tweets made by a specific Twitter user
     *
     * @param twitterUserName is a valid user
     * @return false if subscription already exists, true if subscription is added successfully
     */
    public boolean addSubscription(String twitterUserName) {
        User twitterUser = twitter.getUserFromUserName(twitterUserName);
        if(subscribers.containsKey(twitterUser) || twitterUser == null) {
            return false;
        }
        subscribers.put(twitterUser, new ArrayList<>());
        return true;

    }
    /**
     * Check if a Twitter username is a valid user
     * @param twitterUserName is not null
     * @return true if the username is valid, false otherwise
     **/
    private boolean isValidUser(String twitterUserName) {
        User twitterUser = twitter.getUserFromUserName(twitterUserName);

        if(twitterUser == null) {
            return false;
        }
        else {
            return true;
        }
    }


    /**
     * Add a subscription for all tweets made by a specific Twitter user that also match a given pattern
     *
     * @param twitterUserName is a valid user
     * @param pattern         is not null
     * @return false if subscription already exists, true if subscription is added successfully
     */
    public boolean addSubscription(String twitterUserName, String pattern) {
        User twitterUser = twitter.getUserFromUserName(twitterUserName);

        if(twitterUser == null) {
            return false;
        }
        if(subscribers.containsKey(twitterUser)) {
            subscribers.get(twitterUser).add(pattern.toLowerCase());
        }
        else {
            subscribers.put(twitterUser, new ArrayList<>());
            subscribers.get(twitterUser).add(pattern.toLowerCase());
        }

        return true;
    }

    /**
     * Cancel subscription to a specific Twitter user
     *
     * @param twitterUserName is a valid user
     * @return true if subscription is cancelled successfully, false otherwise
     */
    public boolean cancelSubscription(String twitterUserName) {
        User twitterUser = twitter.getUserFromUserName(twitterUserName);

        if(twitterUser == null) {
            return false;
        }
        if(!subscribers.containsKey(twitterUser)) {
            return false;
        }
        subscribers.remove(twitterUser);
        return true;
    }

    /**
     * Cancel subscription to a specific Twitter user and tweets matching a specific pattern
     *
     * @param twitterUserName is a valid user
     * @param pattern         is not null
     * @return true if subscription is cancelled successfully, false otherwise
     */
    public boolean cancelSubscription(String twitterUserName, String pattern) {
        User twitterUser = twitter.getUserFromUserName(twitterUserName);

        if (twitterUser == null) {
            return false;
        }
        if(!subscribers.containsKey(twitterUser)) {
            return false;
        }
        subscribers.remove(twitterUser);

        return true;
    }

    List<LocalDateTime> getRecentTweetsEndTimes = new ArrayList<>();

    /**
     * Get all subscribed tweets since last tweet or set of tweets was obtained
     *
     * @return list of tweets of all tweets since last tweet or set of tweets was obtained
     */
    public List<TweetV2.TweetData> getRecentTweets() {

        List<TweetV2.TweetData> tweets = new ArrayList<>();
        if(getRecentTweetsEndTimes.size() == 0) {
            LocalDateTime start = LocalDateTime.of(2022,10,1,0,0,0,0);
            LocalDateTime end = LocalDateTime.now();
            getRecentTweetsEndTimes.add(end);

            for(User user : subscribers.keySet()) {
                tweets.addAll(getTweetsByUser(user.getName(), start, end));
            }
        }
        else {
            // LocalDateTime start = timeStamper.getNext().getTimestamp().toLocalDateTime();
            LocalDateTime start = getRecentTweetsEndTimes.get(getRecentTweetsEndTimes.size()-1);
            LocalDateTime end = LocalDateTime.now();

            for(User user : subscribers.keySet()) {
                tweets.addAll(getTweetsByUser(user.getName(), start, end));
            }
        }
        return tweets;
    }

    /**
     * Get all the tweets made by a user within a time range
     *
     * @param twitterUserName is a valid user
     * @param startTime       is a timestamp that is not null and is in local time
     * @param endTime         is a timestamp that is not null and is in local time
     * @return list of tweets made by user within a specific time range
     */
    public List<TweetV2.TweetData> getTweetsByUser(String twitterUserName,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        User twUser = twitter.getUserFromUserName(twitterUserName);
        if (twUser == null) {
            throw new IllegalArgumentException();
        }
        TweetList twList = twitter.getUserTimeline(twUser.getId(), AdditionalParameters.builder().startTime(startTime).endTime(endTime).build());
        return twList.getData();
    }

    public Map<User, List<String>> getSubscribers() {
        return subscribers;
    }
}
