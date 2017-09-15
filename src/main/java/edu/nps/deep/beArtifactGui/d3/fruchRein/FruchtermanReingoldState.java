package edu.nps.deep.beArtifactGui.d3.fruchRein;

import com.vaadin.shared.ui.JavaScriptComponentState;

import elemental.json.Json;
import elemental.json.JsonObject;

/*
 * The json array, which is the "value" attribute of this state object, does not really consist of persistent state, but is rather
 * the latest list of data/commands which is sent from Java to Javascript.  In this implementation, we don't send data in the other direction.
 * Each element of the array is a separate json object.  The key of each object specifies the data or action.  The javascript component, on receiving
 * the state change notification, will deal with the objects in the array in order.
 * 
 * Possible array contents:
 * KEY:           VALUE:
 * 
 * "data"         {"nodes": jsonarray, "links": jsonarray}
 * 
 * "autoarea"     true|false
 * "area"         integer
 * "speed"        float
 * "gravity"      float
 * 
 * "command"      "layout" | "layoutagain" | "stoplayout"
 * 
 */
public class FruchtermanReingoldState extends JavaScriptComponentState
{
  
  public static String DATA_STATE = "data";
  public static String AUTOAREA_PARAM = "autoarea";
  public static String AREA_PARAM  = "area";
  public static String SPEED_PARAM  = "speed";
  public static String GRAVITY_PARAM  = "gravity";
  public static String KCORE_PARAM = "kcore";
  public static String ITERATIONS_PARAM = "iterations";

  public static String COMMAND_PARAM = "command";
  public static String LAYOUT_CMD = "layout";
  public static String LAYOUTAGAIN_CMD = "layout";
  public static String STOPLAYOUT_CMD = "stoplayout";
  
  
  public static Float   SPEED_DEFAULT = 0.1f;
  public static Float   GRAVITY_DEFAULT = 0.75f;
  public static Boolean AUTOAREA_DEFAULT = false;
  public static Integer AREA_DEFAULT = 2420000;
  public static Integer KCORE_DEFAULT = 0;
  public static Integer ITERATIONS_DEFAULT = 1500;
  
  public JsonObject value;
  
  public static JsonObject addData(JsonObject data, JsonObject holder)
  {
    holder.put(DATA_STATE, data);
    return holder;
  }
  
  public static JsonObject addAutoArea(boolean tf, JsonObject holder)
  {
    holder.put(AUTOAREA_PARAM,Json.create(tf));
    return holder;
  }
  
  public static JsonObject addArea(int val, JsonObject holder)
  {
    holder.put(AREA_PARAM,Json.create(val));
    return holder;
  }
  
  public static JsonObject addSpeed(float val, JsonObject holder)
  {
    holder.put(SPEED_PARAM,Json.create(val));
    return holder;
  }
  
  public static JsonObject addGravity(float val, JsonObject holder)
  {
    holder.put(GRAVITY_PARAM,Json.create(val));
    return holder;
  }  

  public static JsonObject addKcore(Integer kcore, JsonObject holder)
  {
    holder.put(KCORE_PARAM,Json.create(kcore));
    return holder;    
  }
  
  public static JsonObject addIterations(Integer iters, JsonObject holder)
  {
    holder.put(ITERATIONS_PARAM, Json.create(iters));
    return holder;
  }

  public static JsonObject addLayout(JsonObject holder)
  {
    holder.put(COMMAND_PARAM,LAYOUT_CMD);
    return holder;
  }
  
  public static JsonObject addLayoutAgain(JsonObject holder)
  {
    holder.put(COMMAND_PARAM,LAYOUTAGAIN_CMD);
    return holder;
  }
  
  public static JsonObject addStopLayout(JsonObject holder)
  {
    holder.put(COMMAND_PARAM,STOPLAYOUT_CMD);
    return holder;
  }
}
