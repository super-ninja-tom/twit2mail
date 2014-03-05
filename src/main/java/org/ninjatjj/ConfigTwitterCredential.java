package org.ninjatjj;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "ConfigTwitterCredential", uniqueConstraints = @UniqueConstraint(columnNames = "screenName"))
public class ConfigTwitterCredential {
    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;


    @ManyToOne
    @JoinColumn(name = "CONFIG_ID", nullable = false)
    private Config config;

    private String screenName;
    private String twitter1;
    private String twitter2;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getTwitter1() {
        return twitter1;
    }

    public void setTwitter1(String twitter1) {
        this.twitter1 = twitter1;
    }

    public String getTwitter2() {
        return twitter2;
    }

    public void setTwitter2(String twitter2) {
        this.twitter2 = twitter2;
    }
}
