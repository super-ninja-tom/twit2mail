package org.ninjatjj.signin;

import org.ninjatjj.Login;
import org.ninjatjj.User;
import org.ninjatjj.signin.SigninServlet;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;

@WebFilter(urlPatterns = {"/index.jsf"})
public class LoginFilter2 implements Filter {
    @PersistenceContext
    EntityManager entityManager;

    @EJB
    private TwitterCredentialManager twitterCredentialManager;

    public final boolean checkCookie(EntityManager entityManager,
                                            HttpServletRequest req, HttpServletResponse res)
            throws IOException, NamingException, NotSupportedException,
            SystemException, SecurityException, IllegalStateException,
            RollbackException, HeuristicMixedException,
            HeuristicRollbackException {
        twitterCredentialManager.init();
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie c = cookies[i];
                if (c.getName().equals(SigninServlet.COOKIE_NAME)) {
                    UserTransaction ut = (UserTransaction) new InitialContext()
                            .lookup("java:jboss/UserTransaction");
                    ut.begin();

                    Login find = entityManager.find(Login.class, c.getValue());
                    if (find != null) {
                        Twitter twitter = new TwitterFactory().getInstance();
                        req.getSession().setAttribute("twitter", twitter);

                        User user = find.getUser();
                        user.getFeeds().size();
                        user.getCircles().size();
                        user.getTweeters().size();
                        user.getSentStories().size();
                        // AccessToken accessToken = new AccessToken(
                        // user.getAccessToken(),
                        // user.getAccessTokenSecret());
                        // twitter.setOAuthAccessToken(accessToken);

                        req.getSession().setAttribute("user", user);
                        ut.commit();
                        return true;
                    } else {
                        c.setMaxAge(0);
                        c.setValue(null);
                        res.addCookie(c);
                        ut.commit();
                        break;
                    }
                }

            }
        }
        return false;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        try {
            if (checkCookie(entityManager, req, res)) {
                res.sendRedirect(req.getContextPath() + "/twit2mail.jsf");
                return;
            } else {
                chain.doFilter(req, response);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub

    }

}
