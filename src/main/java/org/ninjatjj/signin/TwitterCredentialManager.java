package org.ninjatjj.signin;

import org.ninjatjj.ConfigManager;
import org.ninjatjj.User;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;

@Stateless
public class TwitterCredentialManager {

    @EJB
    private ConfigManager configManager;
    private boolean init;

    public void init() throws IOException {
        if (!init) {
            System.setProperty("twitter4j.oauth.consumerKey", getTwitter_consumer_key());
            System.setProperty("twitter4j.oauth.consumerSecret",
                    getTwitter_consumer_secret());
            System.setProperty("twitter4j.http.useSSL", "true");
            init = true;
        }
    }

    public String getTwitter_consumer_key() throws IOException {
        return configManager.getTwitter_consumer_key();
    }

    public String getTwitter_consumer_secret() throws IOException {
        return configManager.getTwitter_consumer_secret();
    }

    public void setAccessToken(User user, String accessToken, String accessTokenSecret) throws IOException {
        configManager.setAccessToken(user.getScreenName(), accessToken, accessTokenSecret);
    }

    public String getAccessTokenSecret(User user) throws IOException {
        return configManager.getAccessTokenSecret(user);
    }

    public String getAccessToken(User user) throws IOException {
        return configManager.getAccessToken(user);
    }
}

