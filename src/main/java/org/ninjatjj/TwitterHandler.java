package org.ninjatjj;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ninjatjj.signin.TwitterCredentialManager;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

//@Singleton
// @TransactionAttribute(TransactionAttributeType.NEVER)
//@AccessTimeout(-1)
@Stateless
public class TwitterHandler {
    static int apiAccessCount;
    private static String hostname = System.getProperty("hostNameOfApp", "twit2mail-twit2mail.193b.starter-ca-central-1.openshiftapps.com");
    private static int refreshCount;
    private SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
    @EJB
    private EmailManager emailManager;
    @EJB
    private CircleManager circleManager;
    @EJB
    private TweeterManager tweeterManager;
    @EJB
    private TwitterCredentialManager twitterCredentialManager;

    // private Map<String, String> followingIds = new HashMap<String, String>();

    public TwitterHandler() {
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
    }

    private static String computeSignature(String baseString, String keyString)
            throws GeneralSecurityException, UnsupportedEncodingException {
        SecretKey secretKey = null;

        byte[] keyBytes = keyString.getBytes();
        secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(secretKey);

        byte[] text = baseString.getBytes();

        return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
    }

    public void emailAllNewItems(final User user, String circle, final Object emailNow,
                                 final List<ContentBean> contentBeans) throws Exception {
//        System.out.println("TwitterHandler emailAllNewItems: " + circle);

        List<Thread> dispatched = new ArrayList<Thread>();
        try {
            downloadFollowing(user);
        } catch (Exception e) {
//            digestContent.append("Could not read users following: " + user.getScreenName() + "\n");
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            e.printStackTrace(pw);
//            digestContent.append(sw.toString());
//            throw e;
            e.printStackTrace();
        }

        final List<Tweeter> updatedFeeds = new ArrayList<>();
        Iterator<Tweeter> iterator = user.getTweeters().iterator();
        while (iterator.hasNext()) {
            final Tweeter tweeter = iterator.next();
            if (circle != null && !circleManager.inCircle(user, circle, tweeter.getName())) {
                continue;
            }
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
//                        StringBuffer content = new StringBuffer();
                        final DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
                        try {
                            HttpEntity entity;
                            if (tweeter.getLastId() == null) {
                                entity = call(user, "/statuses/user_timeline.json", new String[]{"include_rts=false",
                                        "since_id=901972607532015616", "tweet_mode=extended", "user_id=" + tweeter.getTwitterId()}, conn);
                            } else {
                                entity = call(user, "/statuses/user_timeline.json", new String[]{"include_rts=false",
                                        "since_id=" + tweeter.getLastId(), "tweet_mode=extended", "user_id=" + tweeter.getTwitterId()}, conn);
                            }

                            // get a nodelist of elements
                            ArrayList<Tweet> tweets = new ArrayList<Tweet>();
                            JSONArray jar = readFully(entity);

                            for (int i = 0; i < jar.length(); i++) {
                                JSONObject jsonObject = jar.getJSONObject(i);

                                String id = jsonObject.getString("id");
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
                                String dateAsString = jsonObject.getString("created_at");
                                Date created_at = (Date) simpleDateFormat.parse(dateAsString);
                                String text = jsonObject.getString("full_text");

                                // Create a new Employee with the value read from the xml nodes
                                Tweet e = new Tweet(id, text, created_at);

                                if (!e.getText().startsWith("@") && !e.getText().contains("#Repost") && !e.getText().contains("thanks for following me on Twitter!!") && !e.getText().contains("Our biggest fans this week")) {
                                    // add it to list
                                    tweets.add(e);
                                }
                            }

                            Collections.sort(tweets, new Comparator<Object>() {

                                public int compare(Object o1, Object o2) {
                                    Tweet a = (Tweet) o1;
                                    Tweet b = (Tweet) o2;
                                    return a.getCreated_at().compareTo(b.getCreated_at());
                                }
                            });

                            // Doesn't work on gmail
                            // message.setSender(new InternetAddress(username
                            // + "@gmail.com", "t2m:" + name));

                            // message.setSentDate(tweet.getCreated_at());

                            Iterator<Tweet> iterator2 = tweets.iterator();
                            while (iterator2.hasNext()) {
                                Tweet tweet = iterator2.next();
                                ContentBean bean = new ContentBean();
                                bean.name = tweeter.getName();
                                bean.link = "https://twitter.com/" + tweeter.getName() + "/status/" + tweet.getId();
                                bean.date = tweet.getCreated_at();
                                bean.content = tweet.getText().replaceAll("(\\A|\\s)((http|https|ftp|mailto):\\S+)(\\s|\\z)",
                                        "$1<a href=\"$2\">$2</a>$4");

                                bean.content = FeedHandler.replaceHTML(bean.content);
                                contentBeans.add(bean);

                                tweeter.setLastId(tweet.getId());
                            }

                            synchronized (this.getClass()) {
                                refreshCount++;
                            }
                        } finally {
                            conn.close();
                        }

                        updatedFeeds.add(tweeter);
                    } catch (Exception e) {
                        System.out.println("Problem refreshing tweeter: " + tweeter.getName());
                        e.printStackTrace();
                    }
                }
            });
            t.start();
            dispatched.add(t);
        }

        for (Thread t : dispatched) {
            t.join();
        }

        for (Tweeter updated : updatedFeeds) {
            tweeterManager.update(updated);
        }

//        System.out.println("TwitterHandler emailAllNewItems: " + circle + " (done)");
    }

    public void downloadFollowing(User user) throws Exception {
        // the following line adds 3 params to the request just as the
        // parameter string did above. They must match up or the request
        // will fail.
        final DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        try {
            HttpEntity entity = call(user, "/friends/ids.json",
                    new String[]{"screen_name=" + encode(user.getScreenName())}, conn);

            JSONObject jo = new JSONObject(EntityUtils.toString(entity));
            if (jo.has("errors")) {
                String message_from_twitter = jo.getJSONArray("errors").getJSONObject(0).getString("message");
                if (message_from_twitter.equals("Invalid or expired token")
                        || message_from_twitter.equals("Could not authenticate you"))
                    throw new Exception(message_from_twitter);
                else
                    throw new Exception(jo.getJSONArray("errors").getJSONObject(0).getString("message"));
            } else {
                List<String> ids = new ArrayList<String>();
                JSONArray jsonArray = jo.getJSONArray("ids");
                for (int i = 0; i < jsonArray.length(); i++) {

                    ids.add("" + jsonArray.getLong(i));

                }
                Set<Tweeter> tweeters = user.getTweeters();
                Iterator<String> iterator = ids.iterator();
                while (iterator.hasNext()) {
                    Tweeter tweeter = new Tweeter();
                    tweeter.setUser(user);
                    tweeter.setTwitterId(iterator.next());
                    if (!tweeters.contains(tweeter)) {
                        final DefaultHttpClientConnection conn2 = new DefaultHttpClientConnection();
                        try {
                            HttpEntity entity2 = call(user, "/users/lookup.json",
                                    new String[]{"user_id=" + encode(tweeter.getTwitterId())}, conn2);

                            JSONArray jar = readFully(entity2);
                            for (int i = 0; i < jar.length(); i++) {
                                JSONObject jsonObject = jar.getJSONObject(i);
                                JSONObject status = jsonObject.getJSONObject("status");
                                tweeter.setLastId(status.getString("id"));
//                                tweeter.setLastId(status.get);

                                tweeter.setName(jsonObject.getString("screen_name"));

                                boolean add = true;
                                if (user.getCircles().size() > 0) {
                                    Iterator<Circle> circles = user.getCircles().iterator();
                                    while (circles.hasNext()) {
                                        if (circleManager.inCircle(user, circles.next().getName(), tweeter.getName())) {
                                            add = false;
                                            break;
                                        }
                                    }
                                }
                                if (add) {
                                    circleManager.addToCircle(user, "twitter", tweeter.getName(), null, false);
                                }

                                tweeterManager.create(tweeter);
                                tweeters.add(tweeter);
                            }
                        } finally {
                            conn2.close();
                        }
                    }
                }

                // Remove the old items
                List<Tweeter> toRemove = new ArrayList<Tweeter>();
                Iterator<Tweeter> iterator3 = user.getTweeters().iterator();
                while (iterator3.hasNext()) {
                    Tweeter next = iterator3.next();
                    if (!ids.contains(next.getTwitterId())) {
                        toRemove.add(next);
                    }
                }

                for (Tweeter next : toRemove) {
                    circleManager.removeFromCircles(user, next.getName());
                    tweeterManager.remove(user, next.getTwitterId());
                }
            }
        } finally {
            conn.close();
        }
    }

    public String getTweet(User user, String tweet) throws Exception {
        int pos = tweet.indexOf("/");
        for (int i = 0; i < 2; i++) {
            pos = tweet.indexOf("/", pos + 1);
        }
        int pos2 = tweet.indexOf("/", pos + 1);

        String screename = tweet.substring(pos + 1, pos2);
        String id = tweet.substring(tweet.lastIndexOf("/") + 1);
        final DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        try {
            HttpEntity entity = call(user, "/statuses/user_timeline.json", new String[]{
                    "count=1",
                    "max_id=" + id,
                    "screen_name=" + encode(screename),
                    "tweet_mode=extended"
            }, conn);
            JSONArray jar = readFully(entity);

            for (int i = 0; i < jar.length(); i++) {
                JSONObject jsonObject = jar.getJSONObject(i);

                String text = jsonObject.getString("full_text");
                synchronized (this.getClass()) {
                    refreshCount++;
                }
                return text;
            }
        } finally {
            conn.close();
        }
        return null;
    }

    public String downloadTweet(User user, Tweeter tweeter, Object emailNow) throws Exception {
//        System.out.println(tweeter.getName());
        StringBuffer content = new StringBuffer();
        final DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        try {
            HttpEntity entity;
            if (tweeter.getLastId() == null) {
                entity = call(user, "/statuses/user_timeline.json", new String[]{
                        "include_rts=false",
                        "since_id=901972607532015616",
                        "tweet_mode=extended",
                        "user_id=" + tweeter.getTwitterId()
                }, conn);
            } else {
                entity = call(user, "/statuses/user_timeline.json", new String[]{
                        "include_rts=false",
                        "since_id=" + tweeter.getLastId(),
                        "tweet_mode=extended",
                        "user_id=" + tweeter.getTwitterId()
                }, conn);
            }

            // get a nodelist of elements
            ArrayList<Tweet> tweets = new ArrayList<Tweet>();
            JSONArray jar = readFully(entity);

            for (int i = 0; i < jar.length(); i++) {
                JSONObject jsonObject = jar.getJSONObject(i);

                String id = jsonObject.getString("id");
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
                String dateAsString = jsonObject.getString("created_at");
                Date created_at = (Date) simpleDateFormat.parse(dateAsString);
                String text = jsonObject.getString("full_text");

                // Create a new Employee with the value read from the xml nodes
                Tweet e = new Tweet(id, text, created_at);

                if (!e.getText().startsWith("@")) {
                    // add it to list
                    tweets.add(e);
                }
            }

            Collections.sort(tweets, new Comparator<Object>() {

                public int compare(Object o1, Object o2) {
                    Tweet a = (Tweet) o1;
                    Tweet b = (Tweet) o2;
                    return a.getCreated_at().compareTo(b.getCreated_at());
                }
            });

            // Doesn't work on gmail
            // message.setSender(new InternetAddress(username
            // + "@gmail.com", "t2m:" + name));

            // message.setSentDate(tweet.getCreated_at());

            Iterator<Tweet> iterator2 = tweets.iterator();
            while (iterator2.hasNext()) {
                Tweet tweet = iterator2.next();
                content.append("<p>");
                content.append(tweeter.getName() + " ");
                content.append("<a href=\"https://twitter.com/" + tweeter.getName()
                        + "/status/" + tweet.getId() + "\">" + sdf.format(tweet.getCreated_at()) + "</a> ");
                content.append("<a href=\"http://" + hostname + "/unsubscribe.jsf?param="
                        + tweeter.getName() + "&type=twitter\">Unsubscribe</a> ");
                content.append(tweet.getText().replaceAll("(\\A|\\s)((http|https|ftp|mailto):\\S+)(\\s|\\z)",
                        "$1<a href=\"$2\">$2</a>$4") + "</p>");
                tweeter.setLastId(tweet.getId());
                content.append("</p>\n");
            }

            synchronized (this.getClass()) {
                refreshCount++;
            }
        } finally {
            conn.close();
        }

        tweeterManager.update(tweeter);
        return content.toString();
    }

    public String encode(String value) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
            ignore.printStackTrace();
            encoded = value;
        }
        StringBuilder buf = new StringBuilder(encoded.length());
        char focus;
        for (int i = 0; i < encoded.length(); i++) {
            focus = encoded.charAt(i);
            if (focus == '*') {
                buf.append("%2A");
            } else if (focus == '+') {
                buf.append("%20");
            } else if (focus == '%' && (i + 1) < encoded.length() && encoded.charAt(i + 1) == '7'
                    && encoded.charAt(i + 2) == 'E') {
                buf.append('~');
                i += 2;
            } else {
                buf.append(focus);
            }
        }
        return buf.toString();
    }

    private HttpEntity call(User user, String url, String[] strings, DefaultHttpClientConnection conn)
            throws Exception {
        SyncBasicHttpParams params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "HttpCore/1.1");
        HttpProtocolParams.setUseExpectContinue(params, false);
        ImmutableHttpProcessor httpproc = new ImmutableHttpProcessor(
                new HttpRequestInterceptor[]{new RequestContent(), new RequestTargetHost(), new RequestConnControl(),
                        new RequestUserAgent(), new RequestExpectContinue()});

        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
        HttpHost host = new HttpHost("api.twitter.com", 443);

        BasicHttpContext context = new BasicHttpContext(null);
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, null, null);
        SSLSocketFactory ssf = sslcontext.getSocketFactory();
        Socket socket = ssf.createSocket();
        socket.connect(new InetSocketAddress(host.getHostName(), host.getPort()), 0);
        conn.bind(socket, params);

        final String oauth_nonce = UUID.randomUUID().toString().replaceAll("-", "");
        final String oauth_timestamp = (new Long(Calendar.getInstance().getTimeInMillis() / 1000)).toString();
        StringBuffer requestString = new StringBuffer("/1.1" + url + "?");
        for (String string2 : strings) {
            requestString.append(string2);
            requestString.append("&");
        }
        StringBuffer oAuthParams = new StringBuffer();
        String oath = "oauth_consumer_key=" + twitterCredentialManager.getTwitter_consumer_key() + "&oauth_nonce=" + oauth_nonce
                + "&oauth_signature_method=HMAC-SHA1&oauth_timestamp=" + oauth_timestamp + "&oauth_token="
                + encode(twitterCredentialManager.getAccessToken(user)) + "&oauth_version=1.0";

        for (String string2 : strings) {
            if (string2.compareTo(oath) < 0) {
                oAuthParams.append(string2);
                oAuthParams.append("&");
            }
        }
        oAuthParams.append(oath);
        for (String string2 : strings) {
            if (string2.compareTo(oath) > 0) {
                oAuthParams.append("&");
                oAuthParams.append(string2);
            }
        }
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("GET",
                requestString.substring(0, requestString.length() - 1));
        request.setParams(params);
        request.addHeader("Authorization",
                "OAuth oauth_consumer_key=\"" + twitterCredentialManager.getTwitter_consumer_key()
                        + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" + oauth_timestamp
                        + "\",oauth_nonce=\"" + oauth_nonce + "\",oauth_version=\"1.0\",oauth_signature=\""
                        + encode(computeSignature(
                        "GET&" + encode("https://api.twitter.com/1.1" + url) + "&"
                                + encode(oAuthParams.toString()),
                        twitterCredentialManager.getTwitter_consumer_secret() + "&" + encode(twitterCredentialManager.getAccessTokenSecret(user))))
                        + "\",oauth_token=\"" + encode(twitterCredentialManager.getAccessToken(user)) + "\"");
        httpexecutor.preProcess(request, httpproc, context);
        HttpResponse response3 = httpexecutor.execute(request, conn, context);
        response3.setParams(params);
        httpexecutor.postProcess(response3, httpproc, context);
        synchronized (this.getClass()) {
            apiAccessCount++;
        }

        if (response3.getStatusLine().toString().indexOf("500") != -1) {
            throw new Exception("Authentication issue");
        }
        HttpEntity entity = response3.getEntity();
        if (response3.getEntity().getContentLength() == -1) {
            throw new Exception("Could not read from twitter: " + response3.getStatusLine().toString());
        }
        return response3.getEntity();
    }

    private JSONArray readFully(HttpEntity entity) throws Exception {
        InputStream content = entity.getContent();
        int contentLength = (int) entity.getContentLength();
        byte[] contentB = new byte[contentLength];
        int offset = 0;
        while (offset < contentLength) {
            offset += content.read(contentB, offset, contentLength);
        }
        String foo = new String(contentB);
        if (foo.startsWith("{\"error")) {
            throw new Exception(foo);
        }
        return new JSONArray(foo);
    }

    public int getRefreshCount() {
        return refreshCount;
    }

    public int getApiAccessCount() {
        return apiAccessCount;
    }

    public Collection<String> getFollowing(User user) throws Exception {

        if (user.getTweeters().isEmpty()) {
            downloadFollowing(user);
        }

        Collection<String> following = new ArrayList<String>();
        Iterator<Tweeter> iterator = user.getTweeters().iterator();
        while (iterator.hasNext()) {
            following.add(iterator.next().getName());
        }
        return following;
    }

    public void refreshTweeter(User user, Tweeter tweeter) throws Exception {
        Object o = new Object();
        final StringBuffer digestContent = new StringBuffer();
        digestContent.append(downloadTweet(user, tweeter, o));
        emailManager.sendEmail("twit2mail: " + tweeter.getName(),
                null, digestContent.toString(), "text/html", user.getEmailAddress());
//        System.out.println("Tweeter: " + tweeter + " (done)");
    }
}
