package edu.nps.deep.beArtifactGui;

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
  @Override
  protected void init(VaadinRequest vaadinRequest)
  {
    getPage().setTitle("Bulk Extractor Artifact Explorer");
    setContent(new MainP());
  }

  @WebServlet(urlPatterns = "/*", name = "begui", asyncSupported = true)
  @VaadinServletConfiguration(ui = BeGuiUI.class, productionMode = false)
  public static class BeGuiUIServlet extends VaadinServlet
  {
  }
}
