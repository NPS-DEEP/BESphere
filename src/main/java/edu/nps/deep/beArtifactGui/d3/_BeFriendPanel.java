package edu.nps.deep.beArtifactGui.d3;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.Design;

/** 
 * !! DO NOT EDIT THIS FILE !!
 * 
 * This class is generated by Vaadin Designer and will be overwritten.
 * 
 * Please make a subclass with logic and additional interfaces as needed,
 * e.g class LoginView extends LoginDesign implements View { }
 */
@DesignRoot
@AutoGenerated
@SuppressWarnings("serial")
public class _BeFriendPanel extends VerticalLayout
{
  protected Label sourceFileLab;
  protected Button beginButt;
  protected Button cancelButt;
  protected TextArea ta;
  protected ComboBox<java.lang.String> resultsCB;
  protected Button doGraphButt;
  protected Label spacer;

  public _BeFriendPanel()
  {
    Design.read(this);
  }
}
