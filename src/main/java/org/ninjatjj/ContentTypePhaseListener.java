package org.ninjatjj;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletResponse;

public class ContentTypePhaseListener implements PhaseListener {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }

    public void afterPhase(PhaseEvent event) {
    }

    public void beforePhase(PhaseEvent event) {
        FacesContext facesContext = event.getFacesContext();
        HttpServletResponse httpResponse = (HttpServletResponse) facesContext
                .getExternalContext().getResponse();
//        httpResponse.setHeader("Cache-Control",
//                "no-cache, no-store, must-revalidate"); // HTTP 1.1
//        httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
//        httpResponse.setDateHeader("Expires", 0);
    }
}
