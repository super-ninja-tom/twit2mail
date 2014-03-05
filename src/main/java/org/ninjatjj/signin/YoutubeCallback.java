package org.ninjatjj.signin;

import org.ninjatjj.YoutubeHandler;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

@WebServlet("/callbackYT")
public class YoutubeCallback extends HttpServlet {
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String code = request.getParameter("code");
        String videoId = YoutubeHandler.setCode(code);
        response.sendRedirect(request.getContextPath() + "/watchLater.jsf?param=" + videoId);
    }
}
