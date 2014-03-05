package org.ninjatjj;

import org.junit.Ignore;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.assertTrue;

/**
 * Created by still on 07/04/2017.
 */
public class TestYoutube extends BaseTest {
    @Test
    public void testDownloadFollowing() throws Exception {
        feedHandler.getYoutubeFeeds(user);
    }

    @Test
    public void testYoutube() throws Exception {
        feedHandler.getYoutubeFeeds(user);
        Iterator<Feed> iterator = user.getFeeds().iterator();
        while (iterator.hasNext()) {
            final Feed next = iterator.next();
            if (next.getName().equals("NintendoUK")) {
                next.setLastFeedRead(new SimpleDateFormat("yyyy/MM/dd").parse("2018/06/12"));
                next.setIncludeContent(false);

                ArrayList<ContentBean> contentBeans = new ArrayList<>();
                feedHandler.emailAllNewItems(user, "___feed:" + next.getName(), contentBeans);
                assertTrue(!contentBeans.isEmpty());

                contentBeans.clear();
                feedHandler.emailAllNewItems(user, "___feed:" + next.getName(), contentBeans);
                assertTrue(contentBeans.isEmpty());
            }
        }
    }

    @Ignore
    @Test
    public void testWatchLater() throws Exception {
        feedHandler.addToWatchLater(user.getScreenName(), "4Ej2Y2chPGo");
    }
}
