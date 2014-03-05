package org.ninjatjj;

import org.junit.Test;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestFeedHandler extends BaseTest {
    @Test
    public void testCircle() throws Exception {
        feedManager.addFeed(user, "gematsu", "http://gematsu.com/feed", false);
        feedManager.addFeed(user, "retrogamer", "https://www.retrogamer.net/feed/", false);

        assertTrue(circleManager.inCircle(user, "feeds", "gematsu"));
        assertTrue(circleManager.inCircle(user, "feeds", "retrogamer"));

        feedHandler.emailAllNewItems(user, "feeds", new ArrayList<ContentBean>());
    }

    @Test
    public void testIndividual() throws Exception {
//        System.setProperty("javax.net.debug", "all");
        Feed feed = feedManager.addFeed(user, "test", "https://feeds.feedburner.com/TheJimmyDoreShow?format=xml", false);
        feed.setIncludeContent(false);
        Calendar calender = Calendar.getInstance();
        calender.setTimeInMillis(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7);
        feed.setLastFeedRead(calender.getTime());
        ArrayList<ContentBean> contentBeans = new ArrayList<>();
        feedHandler.emailAllNewItems(user, "___feed:" + "test", contentBeans);
        assertFalse(contentBeans.isEmpty());
    }

    @Test
    public void testUnusualDate() throws Exception {
        feedManager.addFeed(user, "test", "http://www.metacritic.com/rss/games/switch", false).setIncludeContent(true);
        feedHandler.emailAllNewItems(user, "___feed:" + "test", new ArrayList<ContentBean>());
    }

    @Test
    public void testSubscribe() throws HeuristicMixedException, IOException, SystemException, NamingException, HeuristicRollbackException, NotSupportedException, RollbackException, NoSuchFieldException, IllegalAccessException {
        String url = "http%3A//shoryuken.com/feed/";
        twit2Mail.addFeedImpl(user, null, url);
    }
}
