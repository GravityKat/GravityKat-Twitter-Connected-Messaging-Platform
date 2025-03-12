package phemeservice;

import io.github.redouane59.twitter.dto.tweet.TweetV2;
import org.junit.jupiter.api.Test;
import twitter.TwitterListener;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Task3A {

    @Test
    public void testFetchRecentTweets() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertTrue(tweets.size() > 0);
    }

    @Test
    public void testDoubleFetchRecentTweets() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertTrue(tweets.size() > 0);
        tweets = tl.getRecentTweets();
        assertTrue(tweets.size() == 0); // second time around, in quick succession, no tweet
    }

    @Test
    public void testAddSubscription() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertFalse(tl.addSubscription("UBC"));
    }

    @Test
    public void testAddSubscriptionPattern() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC","UBC");
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertTrue(tweets.size() > 0);
    }

    @Test
    public void testCancelSubscription() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        tl.cancelSubscription("UBC");
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertEquals(0, tweets.size());
    }

    @Test
    public void testCancelSubscriptionFalse() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        assertFalse(tl.cancelSubscription("SFU"));
    }

    @Test
    public void testCancelSubscriptionPattern() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        tl.cancelSubscription("UBC","engineering");
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertEquals(0, tweets.size());
    }

    @Test
    public void testException() throws IllegalArgumentException{
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        List<TweetV2.TweetData> tweets = tl.getTweetsByUser("sd", LocalDateTime.parse("2022-09-01T00:00:00"),LocalDateTime.parse("2022-10-01T00:00:00"));
    }

    @Test
    public void testNull(){
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        assertFalse(tl.cancelSubscription("oashdfoh"));
        assertFalse(tl.cancelSubscription("oashdfoh", "random"));
        //assertFalse(tl.addSubscription("oashdfoh"));
        //List<TweetV2.TweetData> tweets = tl.getTweetsByUser("sd", LocalDateTime.parse("2022-09-01T00:00:00"),LocalDateTime.parse("2022-10-01T00:00:00"));
    }


}
