package org.ninjatjj;

import org.json.JSONException;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.naming.NamingException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

//@Singleton
// @TransactionAttribute(TransactionAttributeType.NEVER)
//@AccessTimeout(-1)
@Stateless
public class FeedHandler {

    private static String hostname = System.getProperty("hostNameOfApp", "twit2mail-twit2mail.193b.starter-ca-central-1.openshiftapps.com");
    private static Date runningSince = new Date();
    private static int refreshCount;
    private SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
    private SimpleDateFormat runningSinceFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.UK);
    @EJB
    private EmailManager emailManager;
    @EJB
    private CircleManager circleManager;
    @EJB
    private FeedManager feedManager;
    private boolean init;
    private String youtubeAPIKey;
    @EJB
    private ConfigManager configManager;
    @EJB
    private YoutubeHandler youtubeHandler;


    public FeedHandler() throws NoSuchAlgorithmException, KeyManagementException {
        if (!init) {
            // TODO Not suitable for production - don't copy!!!
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            init = true;
        }
    }

    private String getAPIKey() throws IOException {
        return configManager.getAPIKey();
    }

    public void getYoutubeFeeds(User user) throws SecurityException, IllegalStateException, IOException,
            NamingException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException,
            HeuristicRollbackException, ParserConfigurationException, SAXException, JSONException {
        youtubeHandler.downloadFollowing(user, feedManager, getAPIKey(), circleManager);
    }


    public String addToWatchLater(String screenName, String videoId) throws IOException, JSONException {
        return youtubeHandler.addToWatchLater(screenName, videoId);
    }

    public void emailAllNewItems(final User user, String circle, List<ContentBean> contentBeans) throws Exception {
//        System.out.println("FeedHandler emailAllNewItems: " + circle);

        try {
            getYoutubeFeeds(user);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Thread> dispatched = new ArrayList<Thread>();
        Iterator<Feed> feedNamesIterator = user.getFeeds().iterator();
        final List<FeedItem> feedItems = new ArrayList<FeedItem>();
        final List<Feed> updatedFeeds = new ArrayList<>();

        while (feedNamesIterator.hasNext()) {

            final Feed feed = feedNamesIterator.next();

            if (circle != null && !circleManager.inCircle(user, circle, feed.getName())) {
                if (circle.startsWith("___feed:")) {
                    if (!circle.substring(8).equals(feed.getName())) {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Feed updated = refreshFeed(user, feed, feedItems);
                        if (updated != null) {
                            updatedFeeds.add(updated);
                        }
                    } catch (Exception e) {
                        System.out.println("Problem refreshing feed: " + feed.getName());
                        e.printStackTrace();
                    }
                }
            });
            t.start();
            dispatched.add(t);
        }

        for (Thread t : dispatched) {
            try {
                t.join();
            } catch (InterruptedException e) {

                emailManager.sendEmail("twit2mail could not join thread: " + circle,
                        null, "Error thread.join", "text/html", user.getEmailAddress());
            }
        }

        Collections.sort(feedItems);



        for (Feed updated : updatedFeeds) {
            feedManager.updated(updated);
        }

        if (feedItems.size() > 0) {
            Iterator<FeedItem> feedItemsIterator = feedItems.iterator();
            while (feedItemsIterator.hasNext()) {
                FeedItem fi = feedItemsIterator.next();
                Feed feed = fi.feed;
                boolean dontdigest = feed.isIncludeContent();
                StringBuffer content = dontdigest ? new StringBuffer() : null;

                if (fi.link == null) {
                    continue;
                }
                fi.link = fi.link.replace("go.theregister.com/feed/www", "m").replace("www.theverge.com",
                        "mobile.theverge.com");
                ContentBean bean = new ContentBean();

                if (dontdigest) {
                    content.append("<p>");
                    if (!feed.isIncludeContent()) {
                        content.append(feed.getName() + " ");
                    }
                    content.append("<a href=\"" + fi.link + "\">" + sdf.format(fi.updatedDate)
                            + "</a> ");
                    content.append("<a href=\"http://" + hostname + "/unsubscribe.jsf?param="
                            + fi.feedName + "\">Unsubscribe</a> ");
                    content.append("<a href=\"http://" + hostname + "/sendStory.jsf?param="
                            + fi.link + "\">Offline</a> ");
                    if (feed.isYoutube()) {
                        content.append("<a href=\"http://" + hostname + "/watchLater.jsf?param="
                                + fi.link.substring(fi.link.lastIndexOf('=') + 1) + "\">Add to watch later</a> ");
                    }
                } else {
                    bean.name = fi.feedName;
                    bean.link = fi.link;
                    bean.date = fi.updatedDate;
                    if (feed.isYoutube()) {
                        bean.youtubeWL = fi.link.substring(fi.link.lastIndexOf('=') + 1);
                    }
                }
                if (feed.isIncludeContent()) {
                    if (fi.summary != null) {
                        if (dontdigest) {
                            fi.summary = fi.summary.replaceAll("style=\"[A-Za-z0-0 :;-]*\"", "");
                            fi.summary = fi.summary.replaceAll("<iframe.*https://www.youtube.com/embed/(.*)\\?feature=oembed.*iframe>", "<a href=\"http://youtube.com/watch?v=$1\"><img src=\"http://img.youtube.com/vi/$1/maxresdefault.jpg\" /></a>");
                            int hostUrlEndIndex = fi.link.indexOf("/");
                            if (hostUrlEndIndex <= 0) {
                                hostUrlEndIndex = fi.link.length();
                            }
                            String hostUrl = fi.link.substring(0, hostUrlEndIndex);
                            fi.summary = fi.summary.replaceAll("href=\"/", "href=\"http://" + hostUrl + "/");
                            fi.summary = replaceHTML(fi.summary);
                            content.append("\n" + fi.summary + " ");
                        } else {
                            bean.content = fi.summary;
                        }
                    }
                } else {
                    if (dontdigest) {
                        content.append(fi.title + " ");
                    } else {
                        bean.content = fi.title;
                    }
                }
                if (dontdigest) {
                    content.append("</p>\n");
                } else {
                    contentBeans.add(bean);
                }
                bean.content = replaceHTML(bean.content);

                if (dontdigest) {
                    emailManager.sendEmail(fi.feedName + ": " + fi.title, null, content.toString(), "text/html",
                            user.getEmailAddress());
                }
            }
        }
//        System.out.println("FeedHandler emailAllNewItems: " + circle + " (done)");
    }

    private HttpURLConnection connect(String newUrl) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(newUrl).openConnection();
        con.addRequestProperty("User-Agent", "Mozilla/4.0");
        con.setReadTimeout(120000);
        con.connect();
        return con;
    }

    public Feed refreshFeed(User user, Feed feed, List<FeedItem> items)
            throws IOException, ParseException, ParserConfigurationException, SAXException, TransformerException,
            AddressException, MessagingException, NamingException, NotSupportedException, SystemException,
            SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
        Feed updated = null;
        List<FeedItem> feedItems = new ArrayList<FeedItem>();
//        Date lastFeedRead = null;
//        String lastFeedReadTitle = null;

//        System.out.println("Feed: " + feed.getName() + " " + feed.getUrl() + feed.getLastFeedRead());
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setCoalescing(true);

        HttpURLConnection con = connect(feed.getUrl());
        int status = con.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = con.getHeaderField("Location");
                con = connect(newUrl);
                status = con.getResponseCode();
            }
        }
        if (status != HttpURLConnection.HTTP_OK) {
            byte[] bufferArr = new byte[4 * 1024];
            int read;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream in = con.getErrorStream();

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
            throw new IOException(new String(bytes));
        }

        byte[] bufferArr = new byte[4 * 1024];
        int read;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in = con.getInputStream();

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
        String readContent = new String(bytes);
//        readContent = readContent.replaceAll("\\p{Cc}", "");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dom = builder.parse(new InputSource(new StringReader(readContent)));

        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName("entry");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {

                // get the employee element
                Element element = (Element) nl.item(i);

                FeedItem fi = new FeedItem();
                fi.feed = feed;
                fi.feedName = feed.getName();
                fi.title = getTextValue(element, "title");
                if (fi.title != null) {
                    String textValue = getTextValue(element, "published");
                    if (textValue == null || textValue.equals("")) {
                        textValue = getTextValue(element, "updated");
                    }
                    fi.updatedDate = javax.xml.bind.DatatypeConverter.parseDateTime(textValue).getTime();
                    {
                        NodeList elementsByTagName = element.getElementsByTagName("link");
                        int linkOffset = 1;
                        fi.link = ((Element) elementsByTagName.item(elementsByTagName.getLength() - linkOffset))
                                .getAttribute("href");
                    }

                    if (feed.isYoutube()) {
//                        linkOffset = 2;
                        NodeList elementsByTagName = element.getElementsByTagName("media:thumbnail");
                        fi.summary = "<p><a href=\"" + fi.link + "\"><img src=\"" + ((Element) elementsByTagName.item(elementsByTagName.getLength() - 1))
                                .getAttribute("url") + "\"/></a></p>";
                        fi.summary = fi.summary + "<p>" + getTextValue(element, "media:description") + "</p>";
                    } else {
                        fi.summary = getTextValue(element, "content");
                        if (fi.summary == null) {
                            fi.summary = getTextValue(element, "summary");
                        }
                    }
                    feedItems.add(fi);
                } else {
                    TransformerFactory transFactory = TransformerFactory.newInstance();
                    Transformer transformer = transFactory.newTransformer();
                    StringWriter buffer = new StringWriter();
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    transformer.transform(new DOMSource(element), new StreamResult(buffer));
                    String string = buffer.toString();
                    if (string.indexOf("This comment has been removed by the author") < 0) {
//                        System.err.println("Ignoring: " + string);
                    }
                }
            }
        }
        nl = docEle.getElementsByTagName("atom:entry");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {

                // get the employee element
                Element element = (Element) nl.item(i);

                FeedItem fi = new FeedItem();
                fi.feed = feed;
                fi.feedName = feed.getName();
                fi.title = getTextValue(element, "atom:title");
                if (fi.title != null) {
                    String textValue = getTextValue(element, "atom:published");
                    if (textValue == null || textValue.equals("")) {
                        textValue = getTextValue(element, "atom:updated");
                    }
                    fi.updatedDate = javax.xml.bind.DatatypeConverter.parseDateTime(textValue).getTime();
                    fi.summary = getTextValue(element, "atom:content");
                    if (fi.summary == null) {
                        fi.summary = getTextValue(element, "atom:summary");
                    }
                    NodeList elementsByTagName = element.getElementsByTagName("atom:link");
                    fi.link = ((Element) elementsByTagName.item(elementsByTagName.getLength() - 1))
                            .getAttribute("href");

                    feedItems.add(fi);
                } else {
                    TransformerFactory transFactory = TransformerFactory.newInstance();
                    Transformer transformer = transFactory.newTransformer();
                    StringWriter buffer = new StringWriter();
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    transformer.transform(new DOMSource(element), new StreamResult(buffer));
                    String string = buffer.toString();
                    if (string.indexOf("This comment has been removed by the author") < 0) {
//                        System.err.println("Ignoring: " + string);
                    }
                }
            }
        }
        nl = docEle.getElementsByTagName("item");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {

                // get the employee element
                Element element = (Element) nl.item(i);

                FeedItem fi = new FeedItem();
                fi.feed = feed;
                fi.feedName = feed.getName();
                fi.title = getTextValue(element, "title");
                String textValue = getTextValue(element, "pubDate");
                if (textValue == null) {
                    textValue = getTextValue(element, "dc:date");
                    fi.updatedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(textValue);
                } else {
                    try {
                        fi.updatedDate = (Date) new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").parse(textValue);
                    } catch (java.text.ParseException e) {
                        fi.updatedDate = (Date) new SimpleDateFormat("MMM dd, yyyy").parse(textValue);
                    }
                }
                fi.summary = getTextValue(element, "content:encoded");
                if (fi.summary == null) {
                    fi.summary = getTextValue(element, "description");
                }
                fi.link = getTextValue(element, "link");
                if (fi.link == null) {
                    NodeList enclosure = element.getElementsByTagName("enclosure");
                    if (enclosure.getLength() > 0) {
                        Element element1 = (Element)enclosure.item(0);
                        fi.link = element1.getAttribute("url");
                    }
                }

                if (!(feed.getUrl().contains("www.facebook.com") && (fi.title.endsWith("post.") || Pattern.compile("[A-Za-z0-9-_ ]*(is at|shared a).*").matcher(fi.title).matches()))) {
                    feedItems.add(fi);
                }
            }
        }


        Collections.sort(feedItems);

        for (FeedItem item: feedItems) {
            if (item.updatedDate.after(feed.getLastFeedRead())) {
                items.add(item);
                feed.setLastFeedRead(item.updatedDate);
                feed.setLastFeedReadTitle(item.title);
                updated = feed;
//                lastFeedRead = item.updatedDate;
//                lastFeedReadTitle = item.title;
            }
        }

        synchronized (this.getClass()) {
            refreshCount++;
        }

//        System.out.println("Feed; " + feed.getName() + " (done)");
        return updated;
    }

    public int getRefreshCount() {
        return refreshCount;
    }

    private String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl == null || nl.getLength() == 0) {
            nl = ele.getElementsByTagNameNS("*", tagName);
        }
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            if (tagName.equals("content:encoded")) {
                Node child = el.getFirstChild();
                if (child instanceof CharacterData) {
                    CharacterData cd = (CharacterData) child;
                    textVal = cd.getData();
                } else {
                    textVal = el.getTextContent();
                }
            } else {
                if (el.getFirstChild() != null) {
                    textVal = el.getFirstChild().getNodeValue();
                }
            }
        }

        return textVal;
    }

    public String getRunningSince() {
        return runningSinceFormat.format(runningSince);
    }


    public static String replaceHTML(String readContent) {
        readContent = readContent.replaceAll("•", "&#8226;");
        readContent = readContent.replaceAll("“", "&#8220;");
        readContent = readContent.replaceAll("”", "&#8221;");
        readContent = readContent.replaceAll("’", "&#8217;");
        readContent = readContent.replaceAll("'", "&#8217;");
        readContent = readContent.replaceAll("£", "&#163;");
        readContent = readContent.replaceAll("€", "&#8364;");
        return readContent;
    }
}
