package org.ninjatjj;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Config")
public class Config implements Serializable {

    @Id
    @NotNull
    private int id;

    private String email1;

    private String email2;

    private String twitter1;
    private String twitter2;
    private String youtube1;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "config")
    private Set<ConfigTwitterCredential> twitterCredentials = new HashSet<>();

    @Column(length = 500)
    private String json;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail1() {
        return email1;
    }

    public void setEmail1(String email1) {
        this.email1 = email1;
    }

    public String getEmail2() {
        return email2;
    }

    public void setEmail2(String email2) {
        this.email2 = email2;
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

    public String getYoutube1() {
        return youtube1;
    }

    public void setYoutube1(String youtube1) {
        this.youtube1 = youtube1;
    }

    public Set<ConfigTwitterCredential> getTwitterCredentials() {
        return twitterCredentials;
    }

    public void setTwitterCredentials(Set<ConfigTwitterCredential> user) {
        this.twitterCredentials = user;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
