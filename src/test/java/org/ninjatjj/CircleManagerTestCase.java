package org.ninjatjj;

import org.junit.Test;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CircleManagerTestCase extends BaseTest {

    @Test
    public void testSend() throws Exception {
        circleManager.removeCircle(user, "test");
        circleManager.addToCircle(
                user,
                "games",
                "nintendolife,RetroGamer_Mag,NintendoLife",
                "", true);

        final StringBuffer digestContent = new StringBuffer();

        List<ContentBean> beans = new ArrayList<ContentBean>();
        feedHandler.getYoutubeFeeds(user);
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_YEAR, -1);
        date = c.getTime();
        for (Feed feed: user.getFeeds()) {
            feed.setLastFeedRead(date);
            feed.setIncludeContent(false);
        }
        feedHandler.emailAllNewItems(user, "games", beans);
        twitterHandler.downloadFollowing(user);
        for (Tweeter tweeter : user.getTweeters()) {
            tweeter.setLastId(null);
        }
        twitterHandler.emailAllNewItems(user, "games",
                new Object(), beans);

        emailManager.sendEmail(
                "twit2mail: " + "test",
                beans, digestContent.toString(),
                "text/html", user.getEmailAddress());
    }

    @Test
    public void testRemove() throws IOException, NoSuchFieldException,
            SecurityException, IllegalArgumentException,
            IllegalAccessException, IllegalStateException, NamingException,
            NotSupportedException, SystemException, RollbackException,
            HeuristicMixedException, HeuristicRollbackException {
        circleManager.addToCircle(
                user,
                "foo",
                "vice,capcomsf,chocoblanka728,gadgetgirlkylie,danielortizvargas,miles923,bluemaximac099,",
                "", true);
        circleManager.removeFromCircle(user, "foo", "bar");
    }

    @Test
    public void testNestedCircle() throws Exception {
        circleManager.addToCircle(
                user,
                "foo",
                "bar",
                "",
                true);

        circleManager.addToCircle(
                user,
                "bar",
                "test",
                "",
                true);

        feedManager.addFeed(user, "test", "http://blog.rpgmakerweb.com/feed/", false);
        feedHandler.emailAllNewItems(user, "foo", new ArrayList<ContentBean>());
    }
}
