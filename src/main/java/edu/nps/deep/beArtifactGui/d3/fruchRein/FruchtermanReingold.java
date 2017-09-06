package edu.nps.deep.beArtifactGui.d3.fruchRein;

import java.io.Serializable;
import java.util.ArrayList;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.ui.AbstractJavaScriptComponent;

import elemental.json.JsonObject;

@StyleSheet({"chart.css"})
@JavaScript({ "https://ajax.googleapis.com/ajax/libs/jquery/2.2.0/jquery.min.js","d3.v3.min.js","d3.layout.fruchtermanReingold.js","data.js","chart.js", "fruchtermanreingold-connector.js"})
public class FruchtermanReingold extends AbstractJavaScriptComponent
{
  public FruchtermanReingold()
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
  
  public void setValue(JsonObject value)
  {
    getState().value = value;
  }

  public JsonObject getValue()
  {
    return getState().value;
  }

  @Override
  protected FruchtermanReingoldState getState()
  {
    return (FruchtermanReingoldState) super.getState();
  }
}
