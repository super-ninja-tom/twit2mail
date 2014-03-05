package org.ninjatjj;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.ResourceId;
import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by still on 07/04/2017.
 */
@Singleton
public class YoutubeHandler {

    private static String appHome = System.getProperty("pathToApp", "/opt/app-root/src/config/");

    private static String hostname = System.getProperty("hostNameOfApp", "localhost:8080");
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();
    final static Lock lock = new ReentrantLock();
    @EJB
    private ConfigManager configManager;

    public static void downloadFollowing(User user, FeedManager feedManager, String youtubeAPIKey, CircleManager circleManager) throws SecurityException, IllegalStateException, IOException,
            NamingException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException,
            HeuristicRollbackException, ParserConfigurationException, SAXException, JSONException {
        if (user.getYoutubeUsername() == null || user.getYoutubeUsername().length() == 0) {
            Set<Feed> feeds = new HashSet<Feed>(user.getFeeds());
            for (Feed feed : feeds) {
                if (feed.isYoutube()) {
                    feedManager.removeFeed(user, feed);
                }
            }
            return;
        }

        HashMap<String, Feed> youtubeFeeds = new HashMap<String, Feed>();

        Iterator<Feed> feeds = user.getFeeds().iterator();
        while (feeds.hasNext()) {
            Feed next = feeds.next();
            if (next.isYoutube()) {
                youtubeFeeds.put(next.getName(), next);
            }
        }


        String nextPageToken = "";

        do {
            if (!nextPageToken.equals("")) {
                nextPageToken = "&pageToken=" + nextPageToken;
            }

            String readContent = getContent("https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&channelId=" + user.getYoutubeUsername() + nextPageToken, youtubeAPIKey);
            JSONObject foo = new JSONObject(readContent);
            if (foo.has("nextPageToken")) {
                nextPageToken = foo.getString("nextPageToken");
            } else {
                nextPageToken = "";
            }

            JSONArray jsonArray = foo.getJSONArray("items");


            for (int i = 0; i < jsonArray.length(); i++) {

                // get the employee element
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String title = jsonObject.getJSONObject("snippet").getString("title").replaceAll(" ", "");
                String channelId = jsonObject.getJSONObject("snippet").getJSONObject("resourceId").getString("channelId");
                Feed feed = youtubeFeeds.get(title);
                if (feed == null) {
                    feed = feedManager.addFeed(user, title,
                            "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId, true);
                }
                youtubeFeeds.remove(title);
            }
        } while (!nextPageToken.equals(""));

        Iterator<Feed> iterator = youtubeFeeds.values().iterator();
        while (iterator.hasNext()) {
            Feed next = iterator.next();
            circleManager.removeFromCircles(user, next.getName());
            feedManager.removeFeed(user, next);
        }
    }

    private static String getContent(String url, String youtubeAPIKey) throws IOException {
        url = url + "&key=" + youtubeAPIKey;
        URL theUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) theUrl.openConnection();
        conn.connect();

        byte[] bufferArr = new byte[4 * 1024];
        int read;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in = conn.getInputStream();

        while ((read = in.read(bufferArr)) > 0) {
            baos.write(bufferArr, 0, read);
        }

        baos.close();
        in.close();

        byte[] bytes = baos.toByteArray();
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == (byte) 9) {
                bytes[i] = (byte) 32;
            }
        }
        return new String(bytes);
    }

    public synchronized String addToWatchLater(String screenName, String videoId) throws IOException, JSONException {
        String clientSecretsFile = configManager.getClientSecrets();
        File file = new File("tmp.json");
        FileWriter writer = new FileWriter(file);
        try {
            writer.write(clientSecretsFile);
        } finally {
            writer.close();
        }
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");
        Reader clientSecretReader = new FileReader(file);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, clientSecretReader);
        file.delete();
        FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(new File(appHome + "oauth-credentials"));
        DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore("twit2mail");
        AuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes).setCredentialDataStore(datastore).setAccessType("offline").setApprovalPrompt("force")
                .build();

        // Build the local server and bind it to port 8080
        // LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(80).setHost("localhost").build();
        // Authorize.
//        VerificationCodeReceiver receiver = //new LocalServerReceiver.Builder().setPort(8080).setHost("localhost").build();
//                new MyReceiver(appHome, flow, screenName, videoId); //new GooglePromptReceiver();
        YoutubeHandler.flow = flow;
        YoutubeHandler.screenName = screenName;
        YoutubeHandler.videoId = videoId;

        Credential credential = flow.loadCredential(screenName);
        String redirectUri = "http://"+hostname+"/callbackYT";//receiver.getRedirectUri();
        if (credential == null) {
            AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);
            return authorizationUrl.build();
        }

        YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("twit2mail")
                .build();
        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(videoId);
        PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
        playlistItemSnippet.setPlaylistId("WL");
        playlistItemSnippet.setResourceId(resourceId);
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setSnippet(playlistItemSnippet);
        YouTube.PlaylistItems.Insert playlistItemsInsertCommand =
                youtube.playlistItems().insert("snippet,contentDetails", playlistItem);
        try {
            playlistItemsInsertCommand.execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 409) {
                // This can be ignored
            } else if (e.getStatusCode() == 401) {
                AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);
                return authorizationUrl.build();
            } else if (e.getStatusCode() == 404) {
                return "/videoNotExist.jsf?param="+videoId;
            } else {
                throw new IOException("Could not add to watch later", e);
            }
        }
        return null;
    }


    public static String setCode(String code) throws IOException {
        _code = code;
        TokenResponse response = flow.newTokenRequest(code).setRedirectUri("http://"+hostname+"/callbackYT").execute();
        flow.createAndStoreCredential(response, screenName);
        try {
            return videoId;
        } finally {
            videoId = null;
            screenName = null;
            flow = null;
        }
    }
    private static AuthorizationCodeFlow flow = null;
    private static String _code = null;
    private static String videoId = null;
    private static String screenName = null;
}
