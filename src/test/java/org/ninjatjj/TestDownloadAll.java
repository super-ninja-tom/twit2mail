package org.ninjatjj;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by still on 07/04/2017.
 */
public class TestDownloadAll extends BaseTest {
    @Test
    public void test() throws Exception {
        ArrayList<ContentBean> contentBeans = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DAY_OF_YEAR, -14);
        Date oldest = c.getTime();

        feedHandler.getYoutubeFeeds(user);
        feedManager.addFeed(user, "jbtm", "https://jbossts.blogspot.com/feeds/posts/default?alt=rss", false);
//        for (Feed next : user.getFeeds()) {
//            next.setLastFeedRead(oldest);
//            next.setIncludeContent(false);
//        }
        feedHandler.emailAllNewItems(user, "feeds", contentBeans);
        feedHandler.emailAllNewItems(user, "youtube", contentBeans);


        twitterHandler.downloadFollowing(user);
//        for (Tweeter tweeter : user.getTweeters()) {
//            tweeter.setLastId(null);
//        }
        twitterHandler.emailAllNewItems(user, "twitter", new Object(), contentBeans);
        // In case this is a reset remove any old articles
        Iterator<ContentBean> iterator1 = contentBeans.iterator();
        while (iterator1.hasNext()) {
            ContentBean bean = iterator1.next();
            if (bean.date.before(oldest)) {
                iterator1.remove();
            }
        }

        if (contentBeans.size() > 0) {
            System.out.println("");
            for (ContentBean bean : contentBeans) {
                System.out.println(bean.content + "  " + bean.name + "(" + bean.link + ")");
            }
            final StringBuffer digestContent = new StringBuffer();
            emailManager.sendEmail(
                    "twit2mail: " + "test",
                    contentBeans, digestContent.toString(),
                    "text/html", user.getEmailAddress());
        }
    }
}
