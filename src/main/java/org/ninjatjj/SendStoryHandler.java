package org.ninjatjj;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

@Stateless
public class SendStoryHandler {
    @EJB
    private EmailManager emailManager;
    @EJB
    private TwitterHandler twitterHandler;
    @EJB
    private SentStoryManager sentStoryManager;

    public void sendStory(User user, String story) throws Exception {

        SentStory sentStory = new SentStory();
        sentStory.setUrl(story);
        sentStory.setUser(user);

        if (story != null && !user.getSentStories().contains(sentStory)) {
            try {
                if (story.startsWith("https://twitter.com")) {
                    String content = twitterHandler.getTweet(user, story);
                    emailManager.sendEmail(content, null, "<html><p><a href=\""+story+"\">Original</a></p><p>"+content+"</p></html>", "text/html",
                            user.getEmailAddress());
                    sentStoryManager.create(user, sentStory);
                    return;
                }
                URL theUrl;
                theUrl = new URL(story);
                URLConnection con = theUrl.openConnection();
                con.connect();
                String headerField = con.getHeaderField("Content-Type");
                if (headerField.toLowerCase().startsWith("text")) {

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
                    String title = story;
                    int indexOf = readContent.indexOf("<title>");
                    int indexOf2 = readContent.indexOf("</title>");
                    if (indexOf >= 0 && indexOf2 > 0) {
                        title = readContent.substring(indexOf + 7, indexOf2);
                    }
                    readContent = readContent.replaceAll("\\p{Cc}", "");
                    readContent = readContent.replaceAll("src=\"/", "src=\"http://" + con.getURL().getHost() + "/");
                    readContent = readContent.replaceAll("src=\"artwork",
                            "src=\"http://" + con.getURL().getHost() + "/artwork");
                    readContent = readContent.replaceAll("href=\"/", "href=\"http://" + con.getURL().getHost() + "/");
                    int indexOf3 = 1;
                    while (indexOf3 > 0) {
                        indexOf3 = readContent.indexOf("href=\"image.asp");
                        if (indexOf3 > 0) {
                            int indexOf4 = readContent.indexOf("img=", indexOf3);
                            String substring = readContent.substring(0, indexOf3);
                            String substring2 = readContent.substring(indexOf4 + 4);
                            readContent = substring + "href=\"http://" + con.getURL().getHost() + "/" + substring2;
                        }
                    }
                    readContent = FeedHandler.replaceHTML(readContent);

                    int startOfContent = readContent.indexOf("<h1 ");
                    if (startOfContent < 0) {
                        startOfContent = readContent.indexOf("<h1");
                        if (startOfContent < 0 && con.getURL().getHost().contains("register")) {
                            startOfContent = readContent.indexOf("<h2");
                        }
                    }

                    if (startOfContent > 0) {
                        readContent = readContent.substring(startOfContent);
                        readContent = "<html>".concat(readContent);
                    }

                    readContent = readContent.replaceFirst("<html>",
                            "<html><a href=\"" + story + "\">View Original</a><br/>");
                    // readContent =
                    // readContent.substring(readContent.indexOf("<h1"));

                    emailManager.sendEmail(title, null, readContent.toString(), "text/html",
                            user.getEmailAddress());

                    sentStoryManager.create(user, sentStory);
                }
            } catch (Exception e) {

                emailManager.sendEmail("twit2mail: error downloading " + story, null, e.getClass() + " - " + e.getMessage(),
                        "text/html", user.getEmailAddress());
                throw e;
            }
        }
    }

}
