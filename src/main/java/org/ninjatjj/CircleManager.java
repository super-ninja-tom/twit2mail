package org.ninjatjj;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

//@Singleton
//@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
// @AccessTimeout(-1)
@Stateless
public class CircleManager {
    @PersistenceContext
    EntityManager entityManager;

    @EJB
    Twit2MailTimerBean twit2MailTimerBean;

    public void addToCircle(User user, String circleName, String circleList,
                            String schedule, boolean changeSchedule) {
        Circle circle = null;
        Iterator<Circle> iterator = user.getCircles().iterator();
        while (iterator.hasNext()) {
            Circle next = iterator.next();
            if (next.getName().equals(circleName)) {
                circle = next;
                String feeds = circle.getFeeds() == null ? circleList : circle
                        .getFeeds();

                List<String> feedArr = Arrays.asList(feeds.split(","));
                if (feedArr.contains(circleName)) {
                    return;//
                } else {
                    feeds = feeds + "," + circleList;
                }
                if (feeds.startsWith(",")) {
                    feeds = feeds.substring(1);
                }
                if (feeds.endsWith(",")) {
                    feeds = feeds.substring(0, feeds.length() - 1);
                }
                circle.setFeeds(feeds);
                if (changeSchedule) {
                    if (schedule != null && schedule.length() > 0
                            && !schedule.equals(circle.getSchedule())) {
                        circle.setSchedule(schedule);
                        twit2MailTimerBean.createTimer(user, circle);
                    } else {
                        circle.setSchedule(schedule);
                    }
                }
                entityManager.merge(circle);
                entityManager.flush();
                break;
            }
        }
        if (circle == null) {
            circle = new Circle();
            circle.setUser(user);
            circle.setName(circleName);
            String feeds = circleList;
            if (feeds.startsWith(",")) {
                feeds = feeds.substring(1);
            }
            if (feeds.endsWith(",")) {
                feeds = feeds.substring(0, feeds.length() - 1);
            }
            circle.setFeeds(feeds);
            circle.setSchedule(schedule);
            if (schedule != null && schedule.length() > 0) {
                twit2MailTimerBean.createTimer(user, circle);
            }
            entityManager.persist(circle);
            entityManager.flush();
            user.getCircles().add(circle);
        }
    }

    public void removeFromCircle(User user, String circleName, String addition)
            throws SecurityException, IllegalStateException {

        Iterator<Circle> iterator = user.getCircles().iterator();
        while (iterator.hasNext()) {
            Circle next = iterator.next();
            if (next.getName().equals(circleName)) {
                String currentCircleList = next.getFeeds();
                StringBuffer newCircleList = new StringBuffer();
                StringTokenizer stringTokenizer = new StringTokenizer(
                        currentCircleList, ",");
                while (stringTokenizer.hasMoreTokens()) {
                    String nextToken = stringTokenizer.nextToken();
                    if (!nextToken.equals(addition)) {
                        newCircleList.append(nextToken);
                        newCircleList.append(",");
                    }
                }
                if (newCircleList.length() > 0) {
                    String feeds = newCircleList.substring(0,
                            newCircleList.length() - 1);
                    if (feeds.startsWith(",")) {
                        feeds = feeds.substring(1);
                    }
                    if (feeds.endsWith(",")) {
                        feeds = feeds.substring(0, feeds.length() - 1);
                    }
                    next.setFeeds(feeds);
                } else {
                    next.setFeeds("");
                }
                entityManager.merge(next);
                entityManager.flush();
            }
        }

    }

    // @TransactionAttribute(TransactionAttributeType.NEVER)
    public boolean inCircle(User user, String circle, String name) {
        Iterator<Circle> iterator = user.getCircles().iterator();
        while (iterator.hasNext()) {
            Circle next = iterator.next();
            if (next.getName().equals(circle)) {
                String string = next.getFeeds();
                if (string != null) {
                    StringTokenizer st = new StringTokenizer(string, ",");
                    while (st.hasMoreTokens()) {
                        String trim = st.nextToken().trim();
                        if (trim.equals(name)) {
                            return true;
                        }
                        if (inCircle(user, trim, name)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public void removeCircle(User user, String circleName) {
        Iterator<Circle> iterator = user.getCircles().iterator();
        while (iterator.hasNext()) {
            Circle next = iterator.next();
            if (next.getName().equals(circleName)) {
                iterator.remove();
                next.setUser(null);
                entityManager.merge(user);
                entityManager.remove(entityManager.merge(next));
                entityManager.flush();
                break;
            }
        }
    }

    // TODO implement
    public String getSchedule(User user, String circleName, String defaultValue) {
        for (Circle circle : user.getCircles()) {
            if (circle.getName().equals(circleName)) {
                return circle.getSchedule();
            }
        }
        return defaultValue;
    }

    public void removeFromCircles(User user, String name)
            throws SecurityException, IllegalStateException, IOException,
            NamingException, NotSupportedException, SystemException,
            RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
        for (Circle circle : user.getCircles()) {
            if (inCircle(user, circle.getName(), name)) {
                removeFromCircle(user, circle.getName(), name);
            }
        }
    }
}
