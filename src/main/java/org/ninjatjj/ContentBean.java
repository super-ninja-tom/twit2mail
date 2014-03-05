package org.ninjatjj;

import java.util.Date;

public class ContentBean implements Comparable {
    public String name;
    public String link;
    public Date date;
    public String youtubeWL;
    public String content;

    @Override
    public int compareTo(Object other) {
        if (other instanceof ContentBean) {
            return date.compareTo(((ContentBean) other).date);
        } else {
            return 1;
        }
    }
}
