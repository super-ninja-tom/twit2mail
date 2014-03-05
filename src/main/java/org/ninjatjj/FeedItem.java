package org.ninjatjj;

import java.util.Date;

public class FeedItem implements Comparable {

    public String feedName;
    public Date updatedDate;
    public String title;
    public String summary;
    public String link;

    public Feed feed;

    @Override
    public int compareTo(Object o) {
        if (o.getClass().equals(getClass())) {
            int toReturn = updatedDate.compareTo(((FeedItem) o).updatedDate);
            if (toReturn == 0) {
                toReturn = feedName.compareTo(((FeedItem) o).feedName);
            }
            if (toReturn == 0) {
                toReturn = title.compareTo(((FeedItem) o).title);
            }
            return toReturn;
        } else {
            return -1;
        }
    }
}
