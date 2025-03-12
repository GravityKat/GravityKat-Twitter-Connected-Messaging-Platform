package timedelayqueue;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

/*
 * Class Description:
 * Stores items that implement TimestampedObject interface, returns objects in an order that is determined by their timestamps and delay parameter.
 *
 * Rep Invariant & Abstraction Function:
 * delay >= 0
 *
 * Thread Safety Argument:
 * Synchronization due to multiple threads adding messages at once that must be sorted into chronological order
 * */
public class TimeDelayQueue {
    // here

    private List<PubSubMessage> queue;
    private List<Timestamp> actionsLog;

    private int DELAY;

    private int counting;

    /**
     * Create a new TimeDelayQueue
     *
     * @param delay the delay, in milliseconds, that the queue can tolerate, >= 0
     */
    public TimeDelayQueue(int delay) {
        this.queue = Collections.synchronizedList(new ArrayList<PubSubMessage>());
        this.actionsLog = Collections.synchronizedList(new ArrayList<Timestamp>());
        this.DELAY = delay;
    }

    /**
     * Add a message to the TimeDelayQueue
     *
     * @param msg is a valid message to be added to queue
     * @return false if message already exists in queue, true if added successfully
     */
    public synchronized boolean add(PubSubMessage msg) {
        if (queue.size() == 0) {
            queue.add(msg);
            counting++;
            actionsLog.add(new Timestamp(System.currentTimeMillis()));
            return true;
        }
        for (TimestampedObject currMsg : queue) {
            if (currMsg.getId() == msg.getId()) {
                return false;
            }
        }
        queue.add(msg);
        counting++;
        queue.sort(new PubSubMessageComparator());
        actionsLog.add(new Timestamp(System.currentTimeMillis()));
        return true;
    }

    /**
     * Get the count of the total number of messages processed
     * by this TimeDelayQueue
     *
     * @return the total number of messages processed over the total lifetime of the TimeDelayQueue
     */
    public long getTotalMsgCount() {
        return counting;
    }


    /**
     * Get the next message from the queue
     *
     * @return active message that is next in chronological order
     */
    public synchronized PubSubMessage getNext() {
        //current time
        Timestamp currTime = new Timestamp(System.currentTimeMillis());
        if (queue.size() == 0) {
            actionsLog.add(new Timestamp(System.currentTimeMillis()));
            return PubSubMessage.NO_MSG;
        }
        for (int i = 0; i < queue.size(); i++) {
            PubSubMessage currMsg = queue.get(i);
            if (currTime.getTime() - currMsg.getTimestamp().getTime() >= DELAY) {
                if (currMsg.isTransient()) {
                    TransientPubSubMessage tMsg = (TransientPubSubMessage) currMsg;
                    if (currTime.getTime() - tMsg.getTimestamp().getTime() <= tMsg.getLifetime()) {
                        queue.remove(i);
                        actionsLog.add(new Timestamp(System.currentTimeMillis()));
                        return tMsg;
                    }
                } else {
                    queue.remove(i);
                    actionsLog.add(new Timestamp(System.currentTimeMillis()));
                    return currMsg;
                }
            }
        }

        actionsLog.add(new Timestamp(System.currentTimeMillis()));
        return PubSubMessage.NO_MSG;
    }

    /**
     * Get peak load of the TimeDelayQueue
     *
     * @param timeWindow > 0
     * @return the maximum number of operations (add and getNext) performed on this TimeDelayQueue over any time of length timeWindow
     */
    public synchronized int getPeakLoad(int timeWindow) {
        Collections.sort(actionsLog);
        if (actionsLog.size() == 0) {
            return 0;
        }
        if (actionsLog.get(0).getTime() - actionsLog.get(actionsLog.size() - 1).getTime() <= timeWindow) {
            return actionsLog.size();
        }

        List<Integer> numActions = new ArrayList<>();

        for (int i = 0; i < actionsLog.size(); i++) {
            Timestamp startAction = actionsLog.get(i);
            long startTime = startAction.getTime();
            for (int j = i + 1; j < actionsLog.size() - 1; j++) {
                Timestamp currAction = actionsLog.get(j);
                long currTime = currAction.getTime();
                if (startTime - currTime >= timeWindow) {
                    numActions.add(actionsLog.subList(i, j - 1).size());
                    break;
                }
            }
        }

        return Collections.max(numActions);
    }

    // a comparator to sort messages by chronological order
    private static class PubSubMessageComparator implements Comparator<PubSubMessage> {
        public int compare(PubSubMessage msg1, PubSubMessage msg2) {
            return msg1.getTimestamp().compareTo(msg2.getTimestamp());
        }
    }

}