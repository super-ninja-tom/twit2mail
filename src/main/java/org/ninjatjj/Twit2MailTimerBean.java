package org.ninjatjj;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

@Singleton
@javax.ejb.Startup
// @TransactionAttribute(TransactionAttributeType.NEVER)
@AccessTimeout(-1)
public class Twit2MailTimerBean {

    @PersistenceContext
    EntityManager entityManager;
    @Resource
    TimerService timer;
    @EJB
    private TwitterHandler twitterHandler;
    @EJB
    private FeedHandler feedHandler;
    @EJB
    private EmailManager emailManager;

    @PostConstruct
    private void startup() {
        if (System.getProperty("startpostcontent") != null) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(3500);
                        Runtime.getRuntime().exec("cmd /C start http://localhost:8080/post/content");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }};
            t.start();
        }

        List<User> resultList = entityManager.createQuery(
                "select u from User u", User.class).getResultList();
        for (User user : resultList) {
            for (Circle circle : user.getCircles()) {
                if (circle.getSchedule() != null
                        && circle.getSchedule().length() > 0) {
                    createTimer(user, circle);
                }
            }
        }
    }

    public void createTimer(final User user, final Circle circle) {
        String schedule = circle.getSchedule();
        StringTokenizer stringTokenizer = new StringTokenizer(schedule,
                " \t\n\r\f", false);
        final String minute = stringTokenizer.nextToken();
        final String hour = stringTokenizer.nextToken();
        final String dayOfMonth = stringTokenizer.nextToken();
        final String month = stringTokenizer.nextToken();
        final String dayOfWeek = stringTokenizer.nextToken();
//        System.out.println("Created a timer for: " + user.getEmailAddress() + ", subscription: " + circle.getName() + " at: " + minute + " " + hour + " " + dayOfMonth + " " + month + " " + dayOfWeek);
        timer.createCalendarTimer(new ScheduleExpression() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getHour() {
                return hour;
            }

            @Override
            public String getMinute() {
                return minute;
            }

            @Override
            public String getDayOfMonth() {
                return dayOfMonth;
            }

            @Override
            public String getMonth() {
                return month;
            }

            @Override
            public String getDayOfWeek() {
                return dayOfWeek;
            }
        }, new TimerConfig(user.getScreenName() + "#" + circle.getName() + "#"
                + circle.getSchedule(), false));
    }

    @Timeout
    public void programmaticTimeout(Timer timer) {

        String string = (String) timer.getInfo();
        StringTokenizer st = new StringTokenizer(string, "#");
        String screenName = st.nextToken();
        String circleName = st.nextToken();
        String circleSchedule = st.nextToken();
        boolean validTimer = false;
        try {

            User user = entityManager.find(User.class, screenName);
            if (user != null && user.getCircles() != null) {
                for (Circle circle : user.getCircles())
                    if (circle.getName().equals(circleName)) {
                        if (circle.getSchedule().equals(circleSchedule)) {
                            validTimer = true;
                            user.getFeeds().size();
                            user.getTweeters().size();
                            final StringBuffer digestContent = new StringBuffer();

                            List<ContentBean> contentBeans = new ArrayList<ContentBean>();
                            try {
                                feedHandler.emailAllNewItems(user,
                                        circle.getName(), contentBeans);
                            } catch (Exception e) {
                                digestContent.append("Could not read users circle: " + user.getScreenName() + "\n");
                                StringWriter sw = new StringWriter();
                                PrintWriter pw = new PrintWriter(sw);
                                e.printStackTrace(pw);
                                digestContent.append(sw.toString());
                            } finally {
                                try {
                                    twitterHandler.emailAllNewItems(user,
                                            circle.getName(), new Object(),
                                            contentBeans);
                                } finally {

                                    emailManager.sendEmail(
                                            circle.getName(),
                                            contentBeans, digestContent.toString(),
                                            "text/html", user.getEmailAddress());
                                }
                            }
                        }
                    }
            }

            if (!validTimer) {
                timer.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
