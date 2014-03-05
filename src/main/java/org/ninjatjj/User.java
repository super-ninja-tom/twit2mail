package org.ninjatjj;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Users", uniqueConstraints = @UniqueConstraint(columnNames = "screenName"))
public class User implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @NotNull
    @Id
    private String screenName;

    private String emailAddress;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    private Set<Feed> feeds = new HashSet<Feed>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    private Set<Circle> circles = new HashSet<Circle>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    private Set<Tweeter> tweeters = new HashSet<Tweeter>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    private Set<SentStory> sentStories = Collections
            .synchronizedSet(new HashSet<SentStory>());

    private String youtubeUsername;

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Set<Feed> getFeeds() {
        return feeds;
    }

    public void setFeeds(Set<Feed> feeds) {
        this.feeds = feeds;
    }

    public String getYoutubeUsername() {
        return youtubeUsername;
    }

    public void setYoutubeUsername(String youtubeUsername) {
        this.youtubeUsername = youtubeUsername;
    }

    public Set<Circle> getCircles() {
        return circles;
    }

    public void setCircles(Set<Circle> circles) {
        this.circles = circles;
    }

    public Set<Tweeter> getTweeters() {
        return tweeters;
    }

    public void setTweeters(Set<Tweeter> tweeters) {
        this.tweeters = tweeters;
    }

    public Set<SentStory> getSentStories() {
        return sentStories;
    }

    public void setSentStories(Set<SentStory> sentStories) {
        this.sentStories = sentStories;
    }

}
