package org.ninjatjj.signin;

import org.ninjatjj.Login;
import org.ninjatjj.User;
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

@WebFilter(urlPatterns = {"/sendStory.jsf", "/twit2mail.jsf", "/edit.jsf", "/openMail.jsf", "/settings.jsf", "/unsubscribe.jsf", "/watchLater.jsf", "/subscribe.jsf", "/post/*"})
public class LoginFilter extends LoginFilter2 {
    public static final String PAGE_REQUESTED = "requestedResource";
    public static final String QUERY = "requestQuery";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        try {
            if (!checkCookie(entityManager, req, res)) {
                req.getSession().setAttribute(PAGE_REQUESTED, req.getRequestURI());
                req.getSession().setAttribute(QUERY, req.getQueryString());
                res.sendRedirect(req.getContextPath() + "/index.jsf");
                return;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        chain.doFilter(req, response);
    }
}
