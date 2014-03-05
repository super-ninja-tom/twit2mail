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
import java.util.Date;
import java.util.Iterator;

//@Singleton
//@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
// @AccessTimeout(-1)
@Stateless
public class FeedManager {

    @PersistenceContext
    EntityManager entityManager;

    @EJB
    private CircleManager circleManager;

    public Feed addFeed(User user, String feedName, String feedUrl,
                        boolean youtube) throws IOException, NamingException,
            NotSupportedException, SystemException, SecurityException,
            IllegalStateException, RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
        for (Feed feed : user.getFeeds()) {
            if (feed.getName().equals(feedName)) {
                feed.setUrl(feedUrl);
                entityManager.merge(feed);
                entityManager.flush();
                return feed;
            }
        }
        Feed feed = new Feed();
        feed.setName(feedName);
        feed.setUrl(feedUrl);
        feed.setUser(user);
        feed.setYoutube(youtube);
        feed.setIncludeContent(false);
        feed.setLastFeedRead(new Date());

        entityManager.persist(feed);
        entityManager.flush();

        user.getFeeds().add(feed);

        if (!youtube) {
            circleManager.addToCircle(user, "feeds", feedName, null, false);
        } else {
            boolean add = true;
            if (user.getCircles().size() > 0) {
                Iterator<Circle> iterator = user.getCircles().iterator();
                while (iterator.hasNext()) {
                    if (circleManager.inCircle(user, iterator.next().getName(), feedName)) {
                        add = false;
                        break;
                    }
                }
            }
            if (add) {
                circleManager.addToCircle(user, "youtube", feedName, null, false);
            }
        }

        return feed;
    }

    public void removeFeed(User user, Feed feed) {
        user.getFeeds().remove(feed);
        entityManager.remove(entityManager.merge(feed));
        entityManager.flush();
    }

    public void updated(Feed feed) {
        entityManager.merge(feed);
//        entityManager.flush();

    }

    public void setIncludeContent(Feed feed, boolean includeContent) {
        feed.setIncludeContent(includeContent);

        entityManager.merge(feed);
//        entityManager.flush();
    }
}
