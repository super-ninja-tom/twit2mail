package org.ninjatjj;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "Feeds", uniqueConstraints = @UniqueConstraint(columnNames = {
        "USER_ID", "name"}))
public class Feed implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @NotNull
    private String url;
    @NotNull
    private String name;
    @ManyToOne
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;
    @Column(name = "lastFeedRead")
    private Date lastFeedRead;
    @Column
    private boolean youtube = false;
    @Column
    private boolean includeContent = false;
    @Column
    private String lastFeedReadTitle;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == Feed.class
                && ((Feed) obj).getName().equals(name);
    }

    public Date getLastFeedRead() {
        return lastFeedRead;
    }

    public void setLastFeedRead(Date lastFeedRead) {
        this.lastFeedRead = lastFeedRead;
    }

    public boolean isYoutube() {
        return youtube;
    }

    public void setYoutube(boolean youtube) {
        this.youtube = youtube;
    }

    public boolean isIncludeContent() {
        return includeContent;
    }

    public void setIncludeContent(boolean includeContent) {
        this.includeContent = includeContent;
    }

    public String getLastFeedReadTitle() {
        return lastFeedReadTitle;
    }

    public void setLastFeedReadTitle(String lastFeedReadTitle) {
        this.lastFeedReadTitle = lastFeedReadTitle;
    }
}
