package pheme;

import io.github.redouane59.twitter.dto.tweet.TweetV2;
import timedelayqueue.BasicMessageType;
import timedelayqueue.PubSubMessage;
import timedelayqueue.TimeDelayQueue;
import twitter.TwitterListener;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;


/* Class Description:
 * Allows multiple users to subscribe to specific Twitter posts and to communicate among each other.
 *
 * Rep Invariant & Abstraction Function:
 * credentialsFile =! null && credentialsFile.contains(apiKey, apiSecretKey, accessToken, accessTokenSecret)
 *
 * Thread Safety Argument:
 * Not neccessary since TimeDelayQueue deals with thread safety
 * */
public class PhemeService {

    public static final int DELAY = 1000; // 1 second or 1000 milliseconds
    private File twitterCredentialsFile;

    private HashMap<String, UserInfo> usersMap;

    //          userName, twitterListener.
    private Map<String, TwitterListener> subscriptions = new HashMap<>();

    private HashMap<UUID, String> usersMapByID;

    //          ID of receiver, message.
    private Map<UUID, TimeDelayQueue> messages;

    //          PumSubMessage ID, queue of messages.
    private Map<UUID, List<UUID>> deliveredMessages;

    /**
     * Create a new PhemeService
     *
     * @param twitterCredentialsFile is a file containing a valid apiKey, apiSecretKey, accessToken, and accessTokenSecret
     */
    public PhemeService(File twitterCredentialsFile) {
        this.twitterCredentialsFile = twitterCredentialsFile;
        this.usersMap = new HashMap<>();
        this.usersMapByID = new HashMap<>();
        this.messages = new HashMap<>();
        this.deliveredMessages = new HashMap<>();
    }


    /**
     * Ignore this.
     *
     * @param configDirName
     */
    public void saveState(String configDirName) {
    }

    /**
     * Add a new user to Twitter
     *
     * @param userID       is not null
     * @param userName     is not null
     * @param hashPassword is not null
     * @return true if new unique user added successfully, false otherwise
     */
    public boolean addUser(UUID userID, String userName, String hashPassword) {
        if (usersMap.containsKey(userName)) {
            return false;
        }
        usersMap.put(userName, new UserInfo(userName, userID, hashPassword));
        usersMapByID.put(userID, userName);
        // twitterListeners.add(new TwitterListener(twitterCredentialsFile));

        if (usersMap.get(userName).getUserID() == userID
                && Objects.equals(usersMap.get(userName).getPassword(), hashPassword)) {
            subscriptions.put(userName, new TwitterListener(twitterCredentialsFile));
            messages.put(userID, new TimeDelayQueue(DELAY));
            return true;
        }
        return false;
    }

    /**
     * Remove a user from Twitter
     *
     * @param userName     is a valid username
     * @param hashPassword is not null
     * @return true if user is removed successfully, false otherwise
     */
    public boolean removeUser(String userName, String hashPassword) {
        if (!usersMap.containsKey(userName)) {
            return false;
        }
        if (!Objects.equals(usersMap.get(userName).getPassword(), hashPassword)) {
            return false;
        }
        UUID id = usersMap.get(userName).getUserID();
        usersMap.remove(userName);
        usersMapByID.remove(id);
        subscriptions.remove(userName);
        messages.remove(id);

        if (usersMap.containsKey(userName)) {
            return false;
        }
        return true;
    }

    /**
     * Cancel a user's subscription to a specific Twitter user
     *
     * @param userName        is a valid user
     * @param hashPassword    is not null
     * @param twitterUserName is a valid user
     * @return true if subscription is cancelled successfully, false otherwise
     */
    public boolean cancelSubscription(String userName,
                                      String hashPassword,
                                      String twitterUserName) {
        if (!usersMap.containsKey(userName)) {
            return false;
        }
        // check password matches.
        if (!Objects.equals(usersMap.get(userName).getPassword(), hashPassword)) {
            return false;
        }

        return subscriptions.get(userName).cancelSubscription(twitterUserName);
    }

    /**
     * Cancel a user's subscription to a specific Twitter user
     *
     * @param userName        is a valid user
     * @param hashPassword    is not null
     * @param twitterUserName is a valid user
     * @return true if subscription is cancelled successfully, false otherwise
     */
    public boolean cancelSubscription(String userName,
                                      String hashPassword,
                                      String twitterUserName,
                                      String pattern) {
        if (!usersMap.containsKey(userName)) {
            return false;
        }
        // check password matches.
        if (!Objects.equals(usersMap.get(userName).getPassword(), hashPassword)) {
            return false;
        }

        return subscriptions.get(userName).cancelSubscription(twitterUserName, pattern);
    }

    /**
     * Cancel a user's subscription to a specific Twitter user
     *
     * @param userName        is a valid user
     * @param hashPassword    is not null
     * @param twitterUserName is a valid user
     * @return true if subscription is cancelled successfully, false otherwise
     */
    public boolean addSubscription(String userName, String hashPassword,
                                   String twitterUserName) {
        // check if user exists.
        if (!usersMap.containsKey(userName)) {
            return false;
        }
        // check password matches.
        if (!Objects.equals(usersMap.get(userName).getPassword(), hashPassword)) {
            return false;
        }

        return subscriptions.get(userName).addSubscription(twitterUserName);
    }

    /**
     * Add a subscription for a user for all tweets made by a specific Twitter user
     *
     * @param userName        is not null
     * @param hashPassword    is not null
     * @param twitterUserName is a valid user
     * @return true if subscription is added successfully, false otherwise
     */
    public boolean addSubscription(String userName, String hashPassword,
                                   String twitterUserName,
                                   String pattern) {
        // check if user exists.
        if (!usersMap.containsKey(userName)) {
            return false;
        }
        // check password matches.
        if (!Objects.equals(usersMap.get(userName).getPassword(), hashPassword)) {
            return false;
        }

        return subscriptions.get(userName).addSubscription(twitterUserName, pattern);
    }


    /**
     * Send a message.
     *
     * @param userName     is a valid username
     * @param hashPassword is not null
     * @param msg          is not null
     * @return true if message sent successfully, false otherwise
     */
    public boolean sendMessage(String userName,
                               String hashPassword,
                               PubSubMessage msg) {
        if (!usersMap.containsKey(userName)) {
            return false;
        }
        if (!usersMap.get(userName).getPassword().equals(hashPassword)) {
            return false;
        }

        for (UUID user : msg.getReceiver()) {
            messages.get(user).add(msg);
        }
        deliveredMessages.put(msg.getId(), msg.getReceiver());
        return true;
    }

    /**
     * Checking if users receive a tweet
     *
     * @param msgID    is not null
     * @param userList is a list of users receiving the message and is not null
     * @return true if message was delivered successfully, false otherwise
     */
    public List<Boolean> isDelivered(UUID msgID, List<UUID> userList) {
        List<Boolean> isDelivered = new ArrayList<>();
        if (deliveredMessages.containsKey(msgID)) {
            for (UUID user : userList) {
                if (!deliveredMessages.get(msgID).contains(user)) {
                    isDelivered.add(false);
                } else {
                    isDelivered.add(true);
                }
            }
        }
        return isDelivered;
    }

    /**
     * Checking if a user received a tweet
     *
     * @param msgID is not null
     * @param user  is not null
     * @return true if message was delivered successfully, false otherwise
     */
    public boolean isDelivered(UUID msgID, UUID user) {
        if (deliveredMessages.containsKey(msgID)) {
            if (deliveredMessages.get(msgID).contains(user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a userName is a valid user
     *
     * @param userName is not null
     * @return true if the user is valid, false otherwise
     */
    public boolean isUser(String userName) {
        if (usersMap.containsKey(userName)) {
            return true;
        }
        return false;
    }


    /**
     * Get the next message in chronological order
     *
     * @param userName     is not null
     * @param hashPassword is not null
     * @return active message that is next in chronological order
     */
    public PubSubMessage getNext(String userName, String hashPassword) {
        if (!usersMap.containsKey(userName)) {
            return PubSubMessage.NO_MSG;
        }
        if (!usersMap.get(userName).getPassword().equals(hashPassword)) {
            return PubSubMessage.NO_MSG;
        }
        if (subscriptions.containsKey(userName)) {
            for (TweetV2.TweetData tweet : subscriptions.get(userName).getRecentTweets()) {
                PubSubMessage msg = new PubSubMessage(UUID.randomUUID(), Timestamp.valueOf(tweet.getCreatedAt()),
                        UUID.nameUUIDFromBytes(tweet.getAuthorId().getBytes()), usersMap.get(userName).getUserID(),
                        tweet.getText(), BasicMessageType.TWEET);
                messages.get(usersMap.get(userName).getUserID()).add(msg);
            }
            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                System.out.println("fail");
            }
        }

        PubSubMessage next = messages.get(usersMap.get(userName).getUserID()).getNext();

        if (!deliveredMessages.containsKey((next.getId()))) {
            deliveredMessages.put(next.getId(), new ArrayList<>(List.of(usersMap.get(userName).getUserID())));
        }
        else {
            deliveredMessages.get(next.getId()).add(usersMap.get(userName).getUserID());
        }
        return next;
    }

    /**
     * Get the all recent messages since last check
     *
     * @param userName     is not null
     * @param hashPassword is not null
     * @return list of subscribed messages since the last check
     */
    public List<PubSubMessage> getAllRecent(String userName, String hashPassword) {
        if (!usersMap.containsKey(userName)) {
            return new ArrayList<>();
        }
        if (!usersMap.get(userName).getPassword().equals(hashPassword)) {
            return new ArrayList<>();
        }

        if (subscriptions.containsKey(userName)) {
            for (TweetV2.TweetData tweet : subscriptions.get(userName).getRecentTweets()) {
                PubSubMessage msg = new PubSubMessage(UUID.randomUUID(), Timestamp.valueOf(tweet.getCreatedAt()),
                        UUID.nameUUIDFromBytes(tweet.getAuthorId().getBytes()), usersMap.get(userName).getUserID(),
                        tweet.getText(), BasicMessageType.TWEET);
                messages.get(usersMap.get(userName).getUserID()).add(msg);
            }

            try {
                Thread.sleep(DELAY);
            }
            catch (InterruptedException e) {
                System.out.println("it did not meet the delay.");
            }

        }

        List<PubSubMessage> listOfStuff = new ArrayList<>();

        PubSubMessage temporaryMessage = messages.get(usersMap.get(userName).getUserID()).getNext();

        while (!temporaryMessage.equals(PubSubMessage.NO_MSG)) {
            listOfStuff.add(temporaryMessage);
            temporaryMessage = messages.get(usersMap.get(userName).getUserID()).getNext();
        }

        return listOfStuff;
    }
}