package org.ninjatjj;

import org.junit.After;
import org.junit.Before;
import org.ninjatjj.signin.TwitterCredentialManager;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

import static org.junit.Assert.fail;

/**
 * Created by tom on 20/10/2016.
 */
public class BaseTest {
    static {
        java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.SEVERE);
    }
    private static String appHome = System.getProperty("pathToApp", "/opt/app-root/src/config/");

    protected ConfigManager configManager = new ConfigManager();
    protected final EntityManager entityManager = Persistence.createEntityManagerFactory("test").createEntityManager();
    protected Twit2Mail twit2Mail = new Twit2Mail();
    protected CircleManager circleManager;
    protected FeedManager feedManager;
    private YoutubeHandler youtubeHandler = new YoutubeHandler();
    protected FeedHandler feedHandler;
    protected User user;
    protected TwitterHandler twitterHandler = new TwitterHandler();
    protected EmailManager emailManager;
    protected TweeterManager tweeterManager;
    private EntityTransaction transaction;
    private TwitterCredentialManager twitterCredentialManager = new TwitterCredentialManager();
    protected SendStoryHandler sendStoryHandler;
    private SentStoryManager sentStoryManager;


    protected void setConfigManager(Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(
                "configManager");
        field.setAccessible(true);
        field.set(object, configManager);
    }
    protected void setYoutubeHandler(Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(
                "youtubeHandler");
        field.setAccessible(true);
        field.set(object, youtubeHandler);
    }


    protected void setCircleManager(Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(
                "circleManager");
        field.setAccessible(true);
        field.set(object, circleManager);
    }

    protected void setTweeterManager(Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(
                "tweeterManager");
        field.setAccessible(true);
        field.set(object, tweeterManager);
    }

    public void setFeedManager(Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(
                "feedManager");
        field.setAccessible(true);
        field.set(object, feedManager);
    }

    public void setEmailManager(Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(
                "emailManager");
        field.setAccessible(true);
        field.set(object, emailManager);
    }

    public void setEntityManager(Object feedManager) throws NoSuchFieldException, IllegalAccessException {

        Field field = feedManager.getClass().getDeclaredField(
                "entityManager");
        field.setAccessible(true);
        field.set(feedManager, entityManager);
    }

    public void setTwitterCredentialManager(Object feedManager) throws NoSuchFieldException, IllegalAccessException {

        Field field = feedManager.getClass().getDeclaredField(
                "twitterCredentialManager");
        field.setAccessible(true);
        field.set(feedManager, twitterCredentialManager);
    }

    protected void setTwitterHandler(Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(
                "twitterHandler");
        field.setAccessible(true);
        field.set(object, twitterHandler);
    }

    protected void setSentStoryManager(Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(
                "sentStoryManager");
        field.setAccessible(true);
        field.set(object, sentStoryManager);
    }


    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException, KeyManagementException, NoSuchAlgorithmException, IOException {

        if (new File(appHome + "files/twitter.txt.bak").exists())
            new File(appHome + "files/twitter.txt.bak").renameTo(new File(appHome + "files/twitter.txt"));
        if (new File(appHome + "files/mail.txt.bak").exists())
            new File(appHome + "files/mail.txt.bak").renameTo(new File(appHome + "files/mail.txt"));
        if (new File(appHome + "files/youtube.txt.bak").exists())
            new File(appHome + "files/youtube.txt.bak").renameTo(new File(appHome + "files/youtube.txt"));
        if (new File(appHome + "users/tom_jenkinson.txt.bak").exists())
            new File(appHome + "users/tom_jenkinson.txt.bak").renameTo(new File(appHome + "users/tom_jenkinson.txt"));
        if (new File(appHome + "users/3l33ttom.txt.bak").exists())
            new File(appHome + "users/3l33ttom.txt.bak").renameTo(new File(appHome + "users/3l33ttom.txt"));
        if (new File(appHome + "files/client_secrets.json.bak").exists())
            new File(appHome + "files/client_secrets.json.bak").renameTo(new File(appHome + "files/client_secrets.json"));

        transaction = entityManager.getTransaction();
        transaction.begin();
        user = entityManager.find(User.class, "tom_jenkinson");
        if (user == null) {
            user = new User();
            user.setScreenName("tom_jenkinson");
            user.setYoutubeUsername("UCWFX5Jil0hgtpS7dD9GROow");
            user.setEmailAddress("tom.jenkinson@gmail.com");
            entityManager.persist(user);
            user = entityManager.find(User.class, "tom_jenkinson");
            if (user == null) {
                fail("Could not find user");
            }
        } else {
            user.getFeeds().size();
            user.getCircles().size();
            user.getTweeters().size();
            user.getSentStories().size();
        }

        setEntityManager(configManager);
        configManager.startup();

        circleManager = new CircleManager();
        setEntityManager(circleManager);

        feedManager = new FeedManager();
        setCircleManager(feedManager);
        setEntityManager(feedManager);

        emailManager = new EmailManager();

        tweeterManager = new TweeterManager();
        setEntityManager(tweeterManager);

        feedHandler = new FeedHandler();

        sendStoryHandler = new SendStoryHandler();

        sentStoryManager = new SentStoryManager();

        setConfigManager(emailManager);


        setConfigManager(youtubeHandler);

        setYoutubeHandler(feedHandler);
        setFeedManager(feedHandler);
        setEmailManager(feedHandler);
        setCircleManager(feedHandler);
        setConfigManager(feedHandler);

        setConfigManager(twitterCredentialManager);

        setTwitterCredentialManager(twitterHandler);
        setCircleManager(twitterHandler);
        setTweeterManager(twitterHandler);

        setFeedManager(twit2Mail);
        setCircleManager(twit2Mail);

        setEntityManager(sentStoryManager);

        setEmailManager(sendStoryHandler);
        setTwitterHandler(sendStoryHandler);
        setSentStoryManager(sendStoryHandler);
    }

    @After
    public void tearDown() {
        transaction.commit();
//        entityManager.remove(user);

        if (new File(appHome + "files/twitter.txt.bak").exists())
            new File(appHome + "files/twitter.txt.bak").renameTo(new File(appHome + "files/twitter.txt"));
        if (new File(appHome + "files/mail.txt.bak").exists())
            new File(appHome + "files/mail.txt.bak").renameTo(new File(appHome + "files/mail.txt"));
        if (new File(appHome + "files/youtube.txt.bak").exists())
            new File(appHome + "files/youtube.txt.bak").renameTo(new File(appHome + "files/youtube.txt"));
        if (new File(appHome + "users/tom_jenkinson.txt.bak").exists())
            new File(appHome + "users/tom_jenkinson.txt.bak").renameTo(new File(appHome + "users/tom_jenkinson.txt"));
        if (new File(appHome + "users/3l33ttom.txt.bak").exists())
            new File(appHome + "users/3l33ttom.txt.bak").renameTo(new File(appHome + "users/3l33ttom.txt"));
        if (new File(appHome + "files/client_secrets.json.bak").exists())
            new File(appHome + "files/client_secrets.json.bak").renameTo(new File(appHome + "files/client_secrets.json"));
    }
}
