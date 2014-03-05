package org.ninjatjj;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestTwitter extends BaseTest {

    @Test
    public void testDownloadTweet() throws Exception {
        String content = twitterHandler.getTweet(user, "https://twitter.com/nintendolife/status/1034382600171933703");
        System.out.println(content);
        assertTrue(content.length() > 0);
    }

    @Test
    public void emailAllNewTweets() throws Exception {
        String name = "nintendolife";
        twitterHandler.downloadFollowing(user);
        circleManager.addToCircle(user, "test", name, "", true);
        for (Tweeter tweeter : user.getTweeters()) {
            if (tweeter.getName().equals(name)) {
                tweeter.setLastId(null);
                tweeterManager.update(tweeter);
            }
        }
        ArrayList<ContentBean> contentBeans = new ArrayList<>();
        twitterHandler.emailAllNewItems(user, "test", new Object(), contentBeans);
        for (ContentBean bean: contentBeans) {
            System.out.println(bean.content);
        }
        assertFalse(contentBeans.isEmpty());
    }
}
