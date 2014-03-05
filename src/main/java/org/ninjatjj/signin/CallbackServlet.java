/*
Copyright (c) 2007-2009, Yusuke Yamamoto
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
 * Neither the name of the Yusuke Yamamoto nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY Yusuke Yamamoto ``AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Yusuke Yamamoto BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.ninjatjj.signin;

import org.ninjatjj.CircleManager;
import org.ninjatjj.Login;
import org.ninjatjj.User;
import twitter4j.Twitter;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;

@WebServlet("/callback")
public class CallbackServlet extends HttpServlet {
    private static final long serialVersionUID = 1657390011452788111L;
    @PersistenceContext
    EntityManager entityManager;
    @EJB
    private TwitterCredentialManager twitterCredentialManager;

    @EJB
    private CircleManager circleManager;

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        twitterCredentialManager.init();
        Enumeration<String> attributes = request.getSession().getAttributeNames();
        while (attributes.hasMoreElements()) {
            String attributeName = (String) attributes.nextElement();
            request.getParameter(attributeName);
        }

        Twitter twitter = (Twitter) request.getSession()
                .getAttribute("twitter");
        RequestToken requestToken = (RequestToken) request.getSession()
                .getAttribute("requestToken");
        String verifier = request.getParameter("oauth_verifier");
        try {
            AccessToken oAuthAccessToken = twitter.getOAuthAccessToken(
                    requestToken, verifier);
            request.getSession().removeAttribute("requestToken");

            String cookie = UUID.randomUUID().toString();
            Cookie c = new Cookie(SigninServlet.COOKIE_NAME, cookie);
            c.setMaxAge(365 * 24 * 60 * 60); // one year
            response.addCookie(c);

            UserTransaction ut = (UserTransaction) new InitialContext()
                    .lookup("java:jboss/UserTransaction");
            ut.begin();

            User user = entityManager.find(User.class, twitter.getScreenName());
            if (user == null) {
                user = new User();
                user.setScreenName(twitter.getScreenName());
                // user.setAccessToken(oAuthAccessToken.getToken());
                // user.setAccessTokenSecret(oAuthAccessToken.getTokenSecret());
                entityManager.persist(user);

                user = entityManager.find(User.class, twitter.getScreenName());

                circleManager.addToCircle(user, "content", "feeds,youtube,twitter", "0 8 * * *", true);

                twitterCredentialManager.setAccessToken(user,
                        oAuthAccessToken.getToken(),
                        oAuthAccessToken.getTokenSecret());
            } else {
                user.getFeeds().size();
                user.getCircles().size();
                user.getTweeters().size();
                user.getSentStories().size();

                twitterCredentialManager.setAccessToken(user,
                        oAuthAccessToken.getToken(),
                        oAuthAccessToken.getTokenSecret());

            }

            Login login = new Login();
            login.setCookie(cookie);
            login.setUser(user);
            entityManager.persist(login);

            request.getSession().setAttribute("user", user);
            ut.commit();
        } catch (Exception e) {
            throw new ServletException(e);
        }
        Object attribute = request.getSession().getAttribute(LoginFilter.PAGE_REQUESTED);
        if (attribute != null) {
            request.getSession().removeAttribute(LoginFilter.PAGE_REQUESTED);
            Object attribute1 = request.getSession().getAttribute(LoginFilter.QUERY);
            if (attribute1 != null) {
                request.getSession().removeAttribute(LoginFilter.QUERY);
                response.sendRedirect(attribute.toString() + "?" + attribute1.toString());
            } else {
                response.sendRedirect(attribute.toString());
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/");
        }
//        response.sendRedirect(request.getContextPath() + "/");

    }
}
