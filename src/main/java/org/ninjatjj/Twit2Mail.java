/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ninjatjj;

import javax.ejb.EJB;
import javax.faces.bean.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Named("twit2mail")
@ApplicationScoped
public class Twit2Mail implements Serializable {
    private static String hostname = System.getProperty("hostNameOfApp", "twit2mail-twit2mail.193b.starter-ca-central-1.openshiftapps.com");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("Europe/London"));
    }


    private static final long serialVersionUID = 1L;
    @PersistenceContext
    EntityManager entityManager;
    @EJB
    private EmailManager emailManager;
    @EJB
    private TwitterHandler twitterHandler;
    @EJB
    private FeedHandler feedHandler;
    @EJB
    private CircleManager circleManager;
    @EJB
    private FeedManager feedManager;
    @EJB
    private SendStoryHandler sendStoryHandler;

    public void sendTestEmail() throws IOException, MessagingException {

        final User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        emailManager.sendEmail(
                "twit2mail: test email",
                null, "a test email",
                "text/html", user.getEmailAddress());

    }

    public void deleteAccount() throws SecurityException,
            IllegalStateException, RollbackException, HeuristicMixedException,
            HeuristicRollbackException, SystemException, NamingException,
            IOException, NotSupportedException {
        UserTransaction ut = (UserTransaction) new InitialContext()
                .lookup("java:jboss/UserTransaction");
        {

            // Map<String, Object> cookies = FacesContext.getCurrentInstance()
            // .getExternalContext().getRequestCookieMap();
            // Cookie cookie = (Cookie) cookies.get(SigninServlet.COOKIE_NAME);
            // Login find = entityManager.find(Login.class, cookie.getValue());
            // System.out.println(find.getCookie());
            // entityManager.remove(find);

            ut.begin();
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaDelete<Login> delete = cb.createCriteriaDelete(Login.class);
            Root<Login> e = delete.from(Login.class);
            User user = entityManager.find(User.class, getScreenName());
            delete.where(cb.equal(e.get("user"), user));
            entityManager.createQuery(delete).executeUpdate();
            ut.commit();
        }

        {
            ut.begin();
            User user = entityManager.find(User.class, getScreenName());
            entityManager.remove(user);
            ut.commit();
        }

        FacesContext.getCurrentInstance().getExternalContext()
                .invalidateSession();
        FacesContext.getCurrentInstance().getExternalContext().redirect("/");
    }

    public void refreshNamedCircle() throws Exception {

        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        final String circle = params.get("circle");

        final User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        List<ContentBean> beans = new ArrayList<ContentBean>();

        if (circle != null) {
            final List<Exception> toThrow = new ArrayList<Exception>();
            feedHandler.emailAllNewItems(user, circle, beans);
            twitterHandler.emailAllNewItems(user, circle,
                    new Object(), beans);
        }

        if (beans != null && !beans.isEmpty()) {
            Collections.sort(beans);
            Iterator<ContentBean> iterator = beans.iterator();
            StringBuffer sb = new StringBuffer();
            while (iterator.hasNext()) {
                ContentBean next = iterator.next();
                sb.append("<p>");
                sb.append(next.content + " ");
                sb.append("(<a href=\"" + next.link + "\">" + next.name + "</a>");
                if (next.youtubeWL != null) {
                    sb.append(" <a href=\"http://" + hostname + "/watchLater.jsf?param=" + next.youtubeWL + "\">&#10133;</a>");
                } else {
                    sb.append(" <a href=\"http://" + hostname + "/sendStory.jsf?param=" + next.link + "\">&#9993;</a>");
                }
                sb.append(" <a href=\"http://" + hostname + "/unsubscribe.jsf?param=" + next.name + "\">&#10060;</a>)</p>\n");
            }
            sb.append(" <a href=\"http://" + hostname + "/\">Go home</a></p>\n");
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("refreshed", sb.toString());
            FacesContext.getCurrentInstance().getExternalContext().redirect("/refreshed.jsf");//https://mail.google.com/mail/");
        } else {
            FacesContext.getCurrentInstance().getExternalContext()
                    .redirect("/");
        }
    }

    public void includeContentToggled(ValueChangeEvent event) {
    }

    public void removeNamedCircle() throws Exception {

        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        final String circle = params.get("circle");

        final User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        if (circle != null) {
            circleManager.removeCircle(user, circle);
        }
    }

    public void refreshNamedTweeter() throws Exception {

        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        final String tweeter = params.get("param");

        if (tweeter != null) {
            User user = (User) FacesContext.getCurrentInstance()
                    .getExternalContext().getSessionMap().get("user");

            Iterator<Tweeter> iterator = user.getTweeters().iterator();
            while (iterator.hasNext()) {
                Tweeter next = iterator.next();
                if (next.getName().equals(tweeter)) {
                    twitterHandler.refreshTweeter(user, next);
                    break;
                }
            }
        }

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect("https://mail.google.com/mail/");
    }

    public void refreshNamedFeed() throws Exception {

        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        final String feed = params.get("param");

        if (feed != null) {
            final User user = (User) FacesContext.getCurrentInstance()
                    .getExternalContext().getSessionMap().get("user");

            final StringBuffer digestContent = new StringBuffer();
            List<ContentBean> beans = new ArrayList<ContentBean>();
            feedHandler.emailAllNewItems(user,
                    "___feed:" + feed, beans);

            emailManager.sendEmail(
                    "twit2mail: " + feed,
                    beans, digestContent.toString(),
                    "text/html", user.getEmailAddress());
        }

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect("https://mail.google.com/mail/");
    }

    public String getRunningSince() {
        return feedHandler.getRunningSince();
    }

    public int getTweetRefreshCount() {
        return twitterHandler.getRefreshCount();
    }

    public int getFeedRefreshCount() {
        return feedHandler.getRefreshCount();
    }

    public int getApiAccessCount() {
        return twitterHandler.getApiAccessCount();
    }

    public int getEmailCount() {
        return emailManager.getEmailCount();
    }

    public Collection<FeedBean> getFollowing() {
        User user = (User) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("user");
        Collection<String> following = null;
        try {
            following = twitterHandler.getFollowing(user);
        } catch (Exception e) {
            following = Arrays.asList("COULDNOTGETFOLLOWING");
            e.printStackTrace();
        }

        List<FeedBean> feedBeans = new ArrayList<FeedBean>();
        Iterator<String> iterator = following.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            FeedBean fb = new FeedBean(user, next, null, null, circleManager, "twitter");
            feedBeans.add(fb);
        }
        return feedBeans;
    }

    public void refreshFollowing() throws Exception {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        twitterHandler.downloadFollowing(user);
    }

    public void refreshYoutube() throws Exception {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        feedHandler.getYoutubeFeeds(user);
    }

    public List<FeedBean> getFeeds() {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        Iterator<Feed> iterator2 = user.getFeeds().iterator();
        Set<Feed> feeds = user.getFeeds();
        List<FeedBean> feedBeans = new ArrayList<FeedBean>();
        Iterator<Feed> iterator = feeds.iterator();
        while (iterator.hasNext()) {
            Feed next = iterator.next();
            if (!next.isYoutube()) {
                FeedBean fb = new FeedBean(user, next.getName(), next, feedManager, circleManager, "feeds");
                feedBeans.add(fb);
            }
        }
        return feedBeans;
    }

    public List<FeedBean> getTubers() {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        Iterator<Feed> iterator2 = user.getFeeds().iterator();
        boolean hasYoutube = false;
        while (iterator2.hasNext()) {
            Feed next = iterator2.next();
            if (next.isYoutube()) {
                hasYoutube = true;
                break;
            }
        }
        if (!hasYoutube) {
            try {
                feedHandler.getYoutubeFeeds(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Set<Feed> feeds = user.getFeeds();
        List<FeedBean> feedBeans = new ArrayList<FeedBean>();
        Iterator<Feed> iterator = feeds.iterator();
        while (iterator.hasNext()) {
            Feed next = iterator.next();
            if (next.isYoutube()) {
                FeedBean fb = new FeedBean(user, next.getName(), next, feedManager, circleManager, "youtube");
                feedBeans.add(fb);
            }
        }
        return feedBeans;
    }

    public void addFeed(String feedName, String feedUrl) throws IOException,
            SecurityException, IllegalStateException, NamingException,
            NotSupportedException, SystemException, RollbackException,
            HeuristicMixedException, HeuristicRollbackException {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        addFeedImpl(user, feedName, feedUrl);
    }

    public void addFeedImpl(User user, String feedName, String feedUrl) throws HeuristicRollbackException, IOException, SystemException, NamingException, HeuristicMixedException, NotSupportedException, RollbackException {

        feedUrl = feedUrl.replaceAll("%3A", ":");
        feedUrl = feedUrl.replaceAll("%2F", "/");
        feedUrl = feedUrl.replaceAll("%3F", "?");
        if (feedName == null || feedName.equals("")) {
            feedName = feedUrl.substring(feedUrl.indexOf('/') + 2, feedUrl.indexOf('.'));
        }

        feedManager.addFeed(user, feedName, feedUrl, false);
    }

    public void removeFeed() throws IOException, SecurityException,
            IllegalStateException, NamingException, NotSupportedException,
            SystemException, RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        final String feedName = params.get("feed");

        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        for (Feed feed : user.getFeeds()) {
            if (feed.getName().equals(feedName)) {
                circleManager.removeFromCircles(user, feed.getName());
                feedManager.removeFeed(user, feed);
                break;
            }
        }
    }

    public String getEmailAddress() {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        return user.getEmailAddress();
    }

    public void setEmailAddress(String emailAddress) {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        user.setEmailAddress(emailAddress);
    }

    public String getScreenName() {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        return user.getScreenName();
    }

    public void setScreenName(String screenName) {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        user.setScreenName(screenName);
    }

    public String getYoutubeUsername() {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        return user.getYoutubeUsername();
    }

    public void setYoutubeUsername(String youtubeUsername) throws Exception {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        user.setYoutubeUsername(youtubeUsername);
        refreshYoutube();
    }

    public List<CircleBean> getCircles() {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        Iterator<Feed> iterator2 = user.getFeeds().iterator();
        boolean hasYoutube = false;
        while (iterator2.hasNext()) {
            Feed next = iterator2.next();
            if (next.isYoutube()) {
                hasYoutube = true;
                break;
            }
        }
        if (!hasYoutube) {
            try {
                feedHandler.getYoutubeFeeds(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            twitterHandler.getFollowing(user);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<CircleBean> circleBeans = new ArrayList<CircleBean>();
        Iterator<Circle> iterator = user.getCircles().iterator();
        while (iterator.hasNext()) {
            Circle next = iterator.next();
            CircleBean cb = new CircleBean(next.getName(), next.getFeeds(),
                    next.getSchedule());
            circleBeans.add(cb);
        }
        return circleBeans;
    }

    public void updateCircle(String circleName, String addition, String schedule)
            throws IOException, SecurityException, IllegalStateException,
            NamingException, NotSupportedException, SystemException,
            RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        circleManager.addToCircle(user, circleName, addition, schedule, true);
    }

    public void addToCircle(String circleName, String addition)
            throws IOException, SecurityException, IllegalStateException,
            NamingException, NotSupportedException, SystemException,
            RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        circleManager.addToCircle(user, circleName, addition, null, false);
    }

    public void removeFromCircle(String circleName, String removal)
            throws IOException, SecurityException, IllegalStateException,
            NamingException, NotSupportedException, SystemException,
            RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        circleManager.removeFromCircle(user, circleName, removal);
    }

    public void completeUser() throws IOException, NotSupportedException,
            SystemException, NamingException, SecurityException,
            IllegalStateException, RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
        User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");
        UserTransaction ut = (UserTransaction) new InitialContext()
                .lookup("java:jboss/UserTransaction");
        ut.begin();
        entityManager.merge(user);
        ut.commit();

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect("/twit2mail.jsf");
    }

    public void sendStory() throws Exception {
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();

        final String story = params.get("param");
        final User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        new Thread(new Runnable() {
            public void run() {
                try {
                    sendStoryHandler.sendStory(user, story);
                } catch (Exception e) {
                    System.out.println("Problem retrieving story: " + story);
                    e.printStackTrace();
                }
            }
        }).start();

        // More responsive not to wait to see if the feed can be contacted,
        // plus we should get an email error (unless overload email
        // server...)
        // toThrow.wait();
    }

    public void subscribe() throws HeuristicRollbackException, IOException, SystemException, NamingException, HeuristicMixedException, NotSupportedException, RollbackException {
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();

        final String feedUrl = params.get("param");
        final User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        addFeed(null, feedUrl);

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect("/twit2mail.jsf");
    }

    public synchronized void unsubscribe() throws Exception {
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();

        final String feedName = params.get("param");
//        final String type = params.get("type");
//        boolean twitter = false;
//        if (type != null) {
//            twitter = type.equals("twitter");
//        }
        final User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        if (user != null) {
//            if (twitter) {
            for (Tweeter feed : user.getTweeters()) {
                if (feed.getName().equals(feedName)) {
                    circleManager.removeFromCircles(user, feed.getName());
                    break;
                }
            }
//            } else {
            for (Feed feed : user.getFeeds()) {
                if (feed.getName().equals(feedName)) {
                    circleManager.removeFromCircles(user, feed.getName());
                    break;
                }
            }
//            }
        }
    }

    public void watchLater() throws Exception {
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();

        final String videoId = params.get("param");
        final User user = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("user");

        if (user != null) {
            String redirectURL = feedHandler.addToWatchLater(user.getYoutubeUsername(), videoId);
            if (redirectURL != null) {
                FacesContext.getCurrentInstance().getExternalContext()
                        .redirect(redirectURL);
            }
        }
    }

    public String getRefreshed2() throws Exception {
        FacesContext currentInstance = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = currentInstance.getExternalContext().getSessionMap();
        final User user = (User) sessionMap.get("user");
        final String circle = (String) ((HttpServletRequest)currentInstance.getExternalContext().getRequest()).getSession().getAttribute("toRefresh");

        if (circle != null) {
            Object lastToRefresh = sessionMap.get("lastToRefresh");
            if (lastToRefresh == null || !circle.equals(lastToRefresh) && sessionMap.containsKey("tosave")) {
                long currentTime = System.currentTimeMillis();
                final List<ContentBean> beans = Collections.synchronizedList(new ArrayList<ContentBean>());
                Thread t = new Thread() {
                    public void run() {
                        try {
                            feedHandler.emailAllNewItems(user, circle, beans);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();
                Thread t2 = new Thread() {
                    public void run() {
                        try {
                            twitterHandler.emailAllNewItems(user, circle, new Object(), beans);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                t2.start();
                t.join();
                t2.join();

                if (beans != null && !beans.isEmpty()) {
                    Collections.sort(beans);
                    Iterator<ContentBean> iterator = beans.iterator();
                    StringBuffer sb = new StringBuffer();
                    while (iterator.hasNext()) {
                        ContentBean next = iterator.next();
                        sb.append("<p>");
                        sb.append(next.content + " ");
                        sb.append("(<a href=\"" + next.link + "\" target=\"_blank\">" + next.name + "</a>");
                        if (next.youtubeWL != null) {
                            sb.append(" <a href=\"http://" + hostname + "/watchLater.jsf?param=" + next.youtubeWL + "\">&#10133;</a>");
                        } else {
                            sb.append(" <a href=\"http://" + hostname + "/sendStory.jsf?param=" + next.link + "\">&#9993;</a>");
                        }
                        sb.append(" <a href=\"http://" + hostname + "/unsubscribe.jsf?param=" + next.name + "\">&#10060;</a>)</p>\n");
                    }

                    sb.append("<p>Generated at " + SDF.format(new Date()) + " in " + (System.currentTimeMillis() - currentTime) + " milliseconds</p>");
                    String content = sb.toString();
                    sessionMap.put("tosave", content);
                    sessionMap.put("lastToRefresh", circle);
                    return (String) sessionMap.get("tosave");
                } else {
                    sessionMap.remove("tosave");
                    StringBuffer sb = new StringBuffer();
                    sb.append("<p>No new items</p>");
                    sb.append("<p>Generated at " + SDF.format(new Date()) + " in " + (System.currentTimeMillis() - currentTime) + " milliseconds</p>");
                    return sb.toString();
                }
            } else {
                return (String) sessionMap.get("tosave");
            }
        } else {
            return "<p>Unknown circle " + circle + "</p>";
        }
    }

    public void save() throws IOException, MessagingException {
        FacesContext currentInstance = FacesContext.getCurrentInstance();
        final User user = (User) currentInstance.getExternalContext().getSessionMap().get("user");
        String tosave = (String) currentInstance.getExternalContext().getSessionMap().remove("tosave");
        currentInstance.getExternalContext().getSessionMap().remove("lastToRefresh");
        if (tosave != null) {
            emailManager.sendEmail("Saved results", null, tosave,"text/html", user.getEmailAddress());
        }
        String circle = (String) ((HttpServletRequest)currentInstance.getExternalContext().getRequest()).getSession().getAttribute("toRefresh");
        currentInstance.getExternalContext().redirect("/post/"+circle);
    }

    public void reload() throws IOException, MessagingException {
        FacesContext currentInstance = FacesContext.getCurrentInstance();
        final User user = (User) currentInstance.getExternalContext().getSessionMap().get("user");
        currentInstance.getExternalContext().getSessionMap().remove("tosave");
        currentInstance.getExternalContext().getSessionMap().remove("lastToRefresh");
        String circle = (String) ((HttpServletRequest)currentInstance.getExternalContext().getRequest()).getSession().getAttribute("toRefresh");
        currentInstance.getExternalContext().redirect("/post/"+circle);
    }
}
