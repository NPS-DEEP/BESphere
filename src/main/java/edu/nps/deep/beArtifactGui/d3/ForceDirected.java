package edu.nps.deep.beArtifactGui.d3;

import java.io.Serializable;
import java.util.ArrayList;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;

import elemental.json.JsonArray;

@JavaScript({ "d3.min.js", "d3forcedirected.js", "forcedirected-connector.js"})
public class ForceDirected extends AbstractJavaScriptComponent
{
  public ForceDirected()
  { /*
    addFunction("toJava", new JavaScriptFunction() {
      @Override
      public void call(JsonArray arguments) {
          setValue(arguments);
          for (ValueChangeListener listener: listeners)
              listener.valueChange();
      }
    }); */
  }
  
  public interface ValueChangeListener extends Serializable
  {
    void valueChange();
  }

  ArrayList<ValueChangeListener> listeners = new ArrayList<ValueChangeListener>();

  public void addValueChangeListener(ValueChangeListener listener)
  {
    listeners.add(listener);
  }

  //-------------------
  
  public void setValue(JsonArray value)
  {
    getState().value = value;
  }

  public JsonArray getValue()
  {
    return getState().value;
  }

  @Override
  protected ForceDirectedState getState()
  {
    return (ForceDirectedState) super.getState();
  }
}
