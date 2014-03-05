package org.ninjatjj;

import java.util.Date;

public class Tweet {

    private String id;
    private String text;
    private Date created_at;

    public Tweet(String id, String text, Date created_at) {
        this.id = id;
        this.text = text;
        this.created_at = created_at;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Date getCreated_at() {
        return created_at;
    }

}
