package org.ninjatjj;

import java.util.Date;

public class FeedBean {

    private final String name;
    private final User user;
    private final CircleManager circleManager;
    private final String defaultCircle;
    private FeedManager feedManager;
    private Feed feed;
    private boolean includeInCircle;
    private String url;

    public FeedBean(User user, String name, Feed feed, FeedManager feedManager, CircleManager circleManager, String defaultCircle) {
        this.user = user;
        this.name = name;
        this.feed = feed;
        this.feedManager = feedManager;
        this.circleManager = circleManager;
        this.defaultCircle = defaultCircle;
        this.includeInCircle = circleManager.inCircle(user, defaultCircle, name);
    }

    public String getFeedName() {
        return name;
    }

    public String getFeedUrl() {
        return feed.getUrl();
    }

    public boolean isIncludeContent() {
        return feed.isIncludeContent();
    }

    public void setIncludeContent(boolean includeContent) {
        feedManager.setIncludeContent(feed, includeContent);
    }

    public Date getLastFeedRead() {
        return feed.getLastFeedRead();
    }

    public boolean isIncludeInCircle() {
        return includeInCircle;
    }

    public void setIncludeInCircle(boolean includeInCircle) {
        if (includeInCircle != this.includeInCircle) {
            if (includeInCircle) {
                circleManager.addToCircle(user, defaultCircle, name, null, false);
            } else {
                circleManager.removeFromCircle(user, defaultCircle, name);
            }
            this.includeInCircle = includeInCircle;
        }
    }
}
