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

@Entity
@Table(name = "Circles", uniqueConstraints = @UniqueConstraint(columnNames = {
        "USER_ID", "name"}))
public class Circle implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @NotNull
    @Column(length = 1000)
    private String feeds;
    @NotNull
    private String name;
    @ManyToOne
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;
    @Column(length = 2000)
    private String schedule;

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

    public String getFeeds() {
        return feeds;
    }

    public void setFeeds(String url) {
        this.feeds = url;
    }

    @Override
    public int hashCode() {
        if (name == null) {
            return 0;
        }
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == Circle.class
                && (name == null && ((Circle) obj).name == null || ((Circle) obj)
                .getName().equals(name));
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }
}
