package edu.nps.deep.beArtifactGui;

import java.io.File;
import java.util.LinkedList;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

/**
 * This UI is the application entry point. A UI may either represent a browser
 * window (or tab) or some part of a html page where a Vaadin application is
 * embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is
 * intended to be overridden to add component to the user interface and
 * initialize non-component functionality.
 */
@Theme("beGuiTheme")
@Push
public class BeGuiUI extends UI
{
  public static String RENDERQ_ATTRIBUTE = "renderQ";

  @Override
  protected void init(VaadinRequest vaadinRequest)
  {
    getPage().setTitle("Bulk Extractor Artifact Explorer");
    setContent(new MainP());
    
    /* Because we're potentially launching D3 graphs in new tabs, which we launch by providing a UI class name, we can't readily
     * pass parameters, as in which dataset to display.  We use this global queue shared only by windows in this session.  A simple static
     * queue would not suffice since we may have multiple users sharing the same VM.  Hang the queue off the session object which
     * can be accessed through all UIs.
     */
    LinkedList<File[]> renderQ = new LinkedList<>();
    getSession().setAttribute(RENDERQ_ATTRIBUTE, renderQ);
  }

  @WebServlet(urlPatterns = "/*", name = "begui", asyncSupported = true)
  @VaadinServletConfiguration(ui = BeGuiUI.class, productionMode = false)
  public static class BeGuiUIServlet extends VaadinServlet
  {
  }
}
