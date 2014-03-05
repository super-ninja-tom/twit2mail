package org.ninjatjj;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@javax.ejb.Singleton
@javax.ejb.Startup
public class ConfigManager {

    private static String appHome = System.getProperty("pathToApp", "/opt/app-root/src/config/");

    @PersistenceContext
    EntityManager entityManager;
    private Config config;

    @PostConstruct
    public void startup() {
        config = entityManager.find(Config.class, 1);
        if (config == null) {
            System.out.println("WAITING FOR INITIAL CONFIG");
            config = new Config();
            config.setId(1);
            entityManager.persist(config);
            config = entityManager.find(Config.class, 1);
        } else if (config.getEmail1() == null) {
            System.out.println(config.getId() + " " + config.getEmail1() + " " + config.getTwitter1() + " " + config.getTwitter2() + " " + config.getYoutube1());
        }
    }

    public String getTwitter_consumer_key() throws IOException {
        File file = new File(appHome + "files/twitter.txt");
        if (file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            config.setTwitter1(in.readLine());
            config.setTwitter2(in.readLine());
            in.close();
            entityManager.merge(config);
            file.renameTo(new File(appHome + "files/twitter.txt.bak"));
        }
        return config.getTwitter1();
    }

    public String getTwitter_consumer_secret() throws IOException {
        File file = new File(appHome + "files/twitter.txt");
        if (file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            config.setTwitter1(in.readLine());
            config.setTwitter2(in.readLine());
            in.close();
            entityManager.merge(config);
            file.renameTo(new File(appHome + "files/twitter.txt.bak"));
        }
        return config.getTwitter2();
    }

    public String getAccessToken(User userA) throws IOException {
        config = entityManager.find(Config.class, 1);
        config.getTwitterCredentials().size();

        File users = new File(appHome + "users/");
        if (users.exists()) {
            for (File user : users.listFiles()) {
                if (!user.getAbsolutePath().endsWith(".bak")) {
                    BufferedReader in = new BufferedReader(new FileReader(user));
                    String screenName = user.getName().substring(0, user.getName().lastIndexOf("."));
                    String twitter1 = in.readLine();
                    String twitter2 = in.readLine();
                    in.close();
                    setAccessToken(screenName, twitter1, twitter2);
                    user.renameTo(new File(user.getAbsolutePath() + ".bak"));
                }
            }
        }

        for (ConfigTwitterCredential user1 : config.getTwitterCredentials()) {
            if (userA.getScreenName().equals(user1.getScreenName())) {
                return user1.getTwitter1();
            }
        }
        return null;
    }

    public String getAccessTokenSecret(User userA) throws IOException {
        config = entityManager.find(Config.class, 1);
        config.getTwitterCredentials().size();

        File users = new File(appHome + "users/");
        if (users.exists()) {
            for (File user : users.listFiles()) {
                if (!user.getAbsolutePath().endsWith(".bak")) {
                    BufferedReader in = new BufferedReader(new FileReader(user));
                    String screenName = user.getName().substring(0, user.getName().lastIndexOf("."));
                    String twitter1 = in.readLine();
                    String twitter2 = in.readLine();
                    in.close();
                    setAccessToken(screenName, twitter1, twitter2);
                    user.renameTo(new File(user.getAbsolutePath() + ".bak"));
                }
            }
        }

        for (ConfigTwitterCredential user1 : config.getTwitterCredentials()) {
            if (userA.getScreenName().equals(user1.getScreenName())) {
                return user1.getTwitter2();
            }
        }
        return null;
    }

    public void setAccessToken(String screenName, String accessToken, String accessTokenSecret) {
        config = entityManager.find(Config.class, 1);
        config.getTwitterCredentials().size();
        for (ConfigTwitterCredential user1 : config.getTwitterCredentials()) {
            if (screenName.equals(user1.getScreenName())) {
                user1.setTwitter1(accessToken);
                user1.setTwitter2(accessTokenSecret);
                return;
            }
        }
        ConfigTwitterCredential credential = new ConfigTwitterCredential();
        credential.setScreenName(screenName);
        credential.setTwitter1(accessToken);
        credential.setTwitter2(accessTokenSecret);
        config = entityManager.find(Config.class, 1);
        config.getTwitterCredentials().size();
        credential.setConfig(config);
        config.getTwitterCredentials().add(credential);
        entityManager.persist(credential);
    }

    public String getUsername() throws IOException {
        File file = new File(appHome + "files/mail.txt");
        if (file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            config.setEmail1(in.readLine());
            config.setEmail2(in.readLine());
            in.close();
            entityManager.merge(config);
            file.renameTo(new File(file.getAbsolutePath()+".bak"));
        }
        return config.getEmail1();
    }

    public String getPassword() throws IOException {
        File file = new File(appHome + "files/mail.txt");
        if (file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            config.setEmail1(in.readLine());
            config.setEmail2(in.readLine());
            in.close();
            entityManager.merge(config);
            file.renameTo(new File(file.getAbsolutePath()+".bak"));
        }
        return config.getEmail2();
    }

    public String getAPIKey() throws IOException {
        File file = new File(appHome + "files/youtube.txt");
        if (file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            config.setYoutube1(in.readLine());
            in.close();
            entityManager.merge(config);
            file.renameTo(new File(file.getAbsolutePath()+".bak"));
        }
        return config.getYoutube1();
    }
    public String getClientSecrets() throws IOException {
        File file = new File(appHome + "files/client_secrets.json");
        if (file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            config.setJson(in.readLine());
            in.close();
            entityManager.merge(config);
            file.renameTo(new File(file.getAbsolutePath()+".bak"));
        }
        return config.getJson();
    }
}
