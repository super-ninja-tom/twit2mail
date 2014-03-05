package org.ninjatjj;

import org.junit.Test;

public class TestSendStory extends BaseTest {
    @Test
    public void testSendStory() throws Exception {
        String story = "https://twitter.com/nintendolife/status/1039143995119689728";
        sendStoryHandler.sendStory(user, story);
        System.out.println("Sent: " + story);
    }
}
