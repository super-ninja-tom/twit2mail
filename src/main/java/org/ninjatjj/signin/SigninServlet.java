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

import org.ninjatjj.Login;
import org.ninjatjj.User;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/signin")
public class SigninServlet extends HttpServlet {
    private static final long serialVersionUID = -6205814293093350242L;
    public static String COOKIE_NAME = "FOO";
    @PersistenceContext
    EntityManager entityManager;

    @EJB
    private TwitterCredentialManager twitterCredentialManager;

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        twitterCredentialManager.init();
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    Cookie c = cookies[i];
                    if (c.getName().equals(COOKIE_NAME)) {
                        Login find = entityManager.find(Login.class, c.getValue());
                        if (find != null) {
                            Twitter twitter = new TwitterFactory().getInstance();
                            request.getSession().setAttribute("twitter", twitter);

                            User user = find.getUser();
                            // AccessToken accessToken = new AccessToken(
                            // user.getAccessToken(),
                            // user.getAccessTokenSecret());
                            // twitter.setOAuthAccessToken(accessToken);

                            request.getSession().setAttribute("user", user);

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
                                response.sendRedirect(request.getContextPath() + "/twit2mail.jsf");
                            }
                            return;
                        } else {
                            c.setMaxAge(0);
                            c.setValue(null);
                            response.addCookie(c);
                            break;
                        }
                    }
                }
            }

            Twitter twitter = new TwitterFactory().getInstance();
            request.getSession().setAttribute("twitter", twitter);
            StringBuffer callbackURL = request.getRequestURL();
            int index = callbackURL.lastIndexOf("/");
            callbackURL.replace(index, callbackURL.length(), "").append(
                    "/callback");

            RequestToken requestToken = twitter
                    .getOAuthRequestToken(callbackURL.toString());
            request.getSession().setAttribute("requestToken", requestToken);
            response.sendRedirect(requestToken.getAuthenticationURL());

        } catch (TwitterException e) {
            throw new ServletException(e);
        } catch (RuntimeException e) {
            throw new ServletException(e);
        }
    }
}
