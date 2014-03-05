package org.ninjatjj;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Iterator;

//@Singleton
//@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
// @AccessTimeout(-1)
@Stateless
public class TweeterManager {
    @PersistenceContext
    EntityManager entityManager;

    public void create(Tweeter tweeter) {
        entityManager.persist(tweeter);
    }

    public void update(Tweeter tweeter) {
        entityManager.merge(tweeter);
    }

    public void remove(User user, String twitterId) {
        Iterator<Tweeter> iterator = user.getTweeters().iterator();
        while (iterator.hasNext()) {
            Tweeter next = iterator.next();
            if (next.getTwitterId().equals(twitterId)) {
                entityManager.remove(entityManager.merge(next));
                user.getTweeters().remove(next);
                break;
            }
        }
    }
}
