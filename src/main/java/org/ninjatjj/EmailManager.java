package org.ninjatjj;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

@Stateless
public class EmailManager {

    @EJB
    private ConfigManager configManager;

    private static String hostname = System.getProperty("hostNameOfApp", "twit2mail-twit2mail.193b.starter-ca-central-1.openshiftapps.com");
    private String username;

    private String password;

    private Properties properties;

    private Session session;

    private int emailCount;
    private Transport transport;

    public void sendEmail(String subject, List<ContentBean> beans, String content, String mimeType,
                          String emailAddress) throws
            MessagingException, IOException {
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
            sb.append("<p>Sent by <a href=\"http://" + hostname + "\">twit2mail</a></p>\n");
            content = sb.toString();
        } else if (content.length() == 0) {
            return;
        }
        content = "<html><body>" + content + "</body></html>";
        if (username == null) {
            username = configManager.getUsername();
            password = configManager.getPassword();

            // Get system properties
            properties = System.getProperties();

            // Setup mail server
            properties.setProperty("mail.smtp.host", "smtp.gmail.com");
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.port", "587");

            session = Session.getInstance(properties,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });
//        session = Session.getInstance(properties);
            transport = session.getTransport("smtp");
//        transport.connect(username, password);

        }
//        System.out.println("sending mail");
        MimeMessage message = new MimeMessage(session);

        // Doesn't work on gmail
        // message.setSender(new InternetAddress(username
        // + "@gmail.com", "t2m:" + name));

//        message.addRecipient(Message.RecipientType.TO, );
//		message.setSentDate(new Date());
        message.setSubject(subject);
        message.setContent(content, mimeType);

        transport.send(message, new InternetAddress[]{new InternetAddress(emailAddress)});
        emailCount++;
//        System.out.println("sending mail (done)");
    }

    public int getEmailCount() {
        return emailCount;
    }
}
