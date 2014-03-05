package org.ninjatjj;

import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@WebServlet("/post/*")
public class PostServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String circle = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/")+1);
        request.getSession().setAttribute("toRefresh", circle);
        request.getRequestDispatcher("/refreshed2.xhtml").forward(request,response);
    }
}
