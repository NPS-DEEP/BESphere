package edu.nps.deep.beArtifactGui.d3.fruchRein;

import static edu.nps.deep.beArtifactGui.HandlePreferences.*;
import static edu.nps.deep.beArtifactGui.d3.fruchRein.FruchtermanReingoldState.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import edu.nps.deep.beArtifactGui.HandlePreferences;
import edu.nps.deep.beArtifactGui.py.RunBefriendPyThreaded.FriendEdge;
import edu.nps.deep.beArtifactGui.py.RunBefriendPyThreaded.FriendNode;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class FruchtermanReingoldPanel extends _FruchtermanReingoldPanel
{
  public static float WINDOW_WIDTH  = 650f;
  public static float WINDOW_HEIGHT = 725f;
  public static int   GRAPH_WIDTH   = 620;
  public static int   GRAPH_HEIGHT  = 585;
  
  public static void showBeFriends(String windowName, String imagename, String graphFileName, HashSet<FriendNode>nodes, HashSet<FriendEdge>edges)
  {
    Window win = new Window(windowName); 
    FruchtermanReingoldPanel pan = new FruchtermanReingoldPanel(imagename, graphFileName, nodes, edges);

    win.setContent(pan);
    win.setWidth(WINDOW_WIDTH,Unit.PIXELS);
    win.setHeight(WINDOW_HEIGHT,Unit.PIXELS);
    win.center();
    
    UI.getCurrent().addWindow(win);
  }
  public static void show(String windowName, String filename, String email, HashMap<String,Long> emailCloseness)
  {
    Window win = new Window(windowName);
    FruchtermanReingoldPanel pan = new FruchtermanReingoldPanel(filename, email, emailCloseness, GRAPH_WIDTH, GRAPH_HEIGHT);
    win.setContent(pan);
    win.setWidth(WINDOW_WIDTH,Unit.PIXELS);
    win.setHeight(WINDOW_HEIGHT,Unit.PIXELS);
    win.center();
    
    UI.getCurrent().addWindow(win);
  }
  
  private FruchtermanReingold fr;
  public FruchtermanReingoldPanel(String filename, String email, HashMap<String,Long> emailCloseness, int width, int height)
  {    
    imageFileLab.setValue(filename);
    graphFileLab.setVisible(false);
    setupParamWidgets();
    
    fr = new FruchtermanReingold();
    
    JsonObject jo = buildJson(email,emailCloseness, Math.min(width, height)/2 - 80);
    loadParams();
    loadParamsJsonObject(jo);
    FruchtermanReingoldState.addLayout(jo);
    fr.setValue(jo);
    
    graphVL.addComponent(fr);
    fr.addValueChangeListener(()->{
      Notification.show("Value from client: "+fr.getValue());
    });
  }
  
  private JsonObject buildJson(String email, HashMap<String,Long> emailCloseness, int maxLinkLength)
  {
    JsonObject returnObj = Json.createObject();
    JsonArray linkArr = Json.createArray();
    JsonArray nodeArr = Json.createArray();
    returnObj.put("nodes",nodeArr);
    returnObj.put("links", linkArr);

    nodeArr.set(0, nodeObject(email,2,10));
    //System.out.println("node: "+email+" 2");
    Set<Entry<String,Long>> vals = emailCloseness.entrySet();
    int idx = 1;
    for(Entry<String,Long> entry : vals) {
      //System.out.println("node: "+entry.getKey()+" 1");
      nodeArr.set(idx++,nodeObject(entry.getKey(),1,5));
    }
    
    int maxval = -1;
    idx=0;
    for(Entry<String,Long> entry : vals) {
      int dist = entry.getValue().intValue();
      if(dist>maxval)
        maxval=dist;
      //System.out.println("link: "+email+" "+entry.getKey()+" "+dist);
      linkArr.set(idx++,linkObject(email,entry.getKey(),dist));
    }
    scaleLinks(linkArr,maxval,maxLinkLength);
    return returnObj;
  }
  private void scaleLinks(JsonArray linkArr,int maxl, int maxLinkLength)
  {
    int sz = linkArr.length();
    for(int i=0;i<sz;i++) {
      JsonObject obj = linkArr.get(i);
      double num = obj.getNumber("value");
      num = (num/maxl)*maxLinkLength + 30;
      obj.put("value", num);
    }
  }
  private JsonObject linkObject(String source, String target, int distance)
  {
    JsonObject jobj = Json.createObject();
    jobj.put("source", source);
    jobj.put("target", target);
    jobj.put("value", distance);
    return jobj;
  }
  private JsonObject nodeObject(String name, int group, int diam)
  {
    JsonObject jobj = Json.createObject();
    jobj.put("id", name);
    jobj.put("group",group);
    jobj.put("diam", diam);
    return jobj;
  }

  public FruchtermanReingoldPanel(String diskImagePath, String graphFileName, HashSet<FriendNode> nodes, HashSet<FriendEdge> edges)
  {
    imageFileLab.setValue(diskImagePath);
    graphFileLab.setValue(graphFileName);
    nodeCountLab.setValue(""+nodes.size());
    edgeCountLab.setValue(""+edges.size());
    setupParamWidgets();
    upButt.addClickListener(e->{
      Integer val = (Integer)checkValid(areaTF.getValue().trim(),false);
      val = val*5;
      areaTF.setValue(val.toString());
    });
    downButt.addClickListener(e->{
      Integer val = (Integer)checkValid(areaTF.getValue().trim(),false);
      val = val/5;
      areaTF.setValue(val.toString());
    });
    fr = new FruchtermanReingold();
    JsonObject jo = buildBeFriendJson(nodes,edges);
    loadParams();
    loadParamsJsonObject(jo);
    FruchtermanReingoldState.addLayout(jo);
    fr.setValue(jo);
    
    graphVL.addComponent(fr);
    fr.addValueChangeListener(()->{
      Notification.show("Value from client: "+fr.getValue());
    });
  }
  
  private JsonObject stateData;
  private HashSet<String> check2WayLinks = new HashSet<>();  //test
  private JsonObject buildBeFriendJson(HashSet<FriendNode> nodes, HashSet<FriendEdge> edges)
  {
    JsonObject wrap = Json.createObject();
    stateData = Json.createObject();
    wrap.put("data", stateData);
    JsonObject links = Json.createObject();
    JsonObject nods = Json.createObject();
    
    stateData.put("nodes",nods);
    stateData.put("links", links);
    
    int idx = 0;
    for(FriendNode fn : nodes) {
      nods.put(fn.label, nodeObject(fn.label,idx,7));
    }
    idx = 0;
    for(FriendEdge lnk : edges) {      
      String s1 = lnk.source+lnk.target;  //test
      String s2 = lnk.target+lnk.source;  //test
      if(check2WayLinks.contains(s1) || check2WayLinks.contains(s2)) {  //test
         throw new RuntimeException("Data: "+s1);  //test
      }
      check2WayLinks.add(lnk.source+lnk.target); //test
      check2WayLinks.add(lnk.target+lnk.source); //test
      
      
      links.put(lnk.source+idx++, linkObject(lnk.source,lnk.target,lnk.weight));
    }
    return wrap; //returnObj;
  }
  
  // state changes
  class ParamState {
    public Boolean autoarea;
    public Integer area;
    public Float speed;
    public Float gravity;
    public Integer kcore;
    public Integer iterations;
  }
  private ParamState params = new ParamState();
  
  private ArrayList<String>nums = new ArrayList<>(50);
  private void setupParamWidgets()
  {
    for(int i=0;i<51;i++)
      nums.add(""+i);
    kCoreCombo.setItems(nums);
    
    kCoreCombo.addValueChangeListener(e->{
      Integer val = (Integer)checkValid(kCoreCombo.getValue().trim(),false);
      if(val != null) {
        params.kcore = val;
        layoutButt.setEnabled(true);
        HandlePreferences.putInt(FRUCHT_REIN_KCORE_KEY+graphFileLab.getValue(),val);
      }
    });
    
    autoareaCB.addValueChangeListener(e->{
      params.autoarea = autoareaCB.getValue();
      layoutButt.setEnabled(true);
      areaTF.setEnabled(!params.autoarea);
      upButt.setEnabled(!params.autoarea);
      downButt.setEnabled(!params.autoarea);
      HandlePreferences.putBoolean(FRUCHT_REIN_AUTOAREA_KEY+graphFileLab.getValue(),autoareaCB.getValue());
    });
    
    areaTF.addValueChangeListener(e->{
      Integer val = (Integer)checkValid(areaTF.getValue().trim(),false);
      if(val != null) {
        params.area = val;
        layoutButt.setEnabled(true);
        HandlePreferences.putInt(FRUCHT_REIN_AREA_KEY+graphFileLab.getValue(),val);
      }
    });
    
    speedTF.addValueChangeListener(e->{
      Float val = (Float)checkValid(speedTF.getValue().trim(),true);
      if(val != null) {
        params.speed = val;
        layoutButt.setEnabled(true);
        HandlePreferences.putFloat(FRUCHT_REIN_SPEED_KEY+graphFileLab.getValue(),val);
      }
    });
    
    gravityTF.addValueChangeListener(e-> {
      Float val = (Float)checkValid(gravityTF.getValue().trim(),true);
      if(val != null) {
        params.gravity = val;
        layoutButt.setEnabled(true);
        HandlePreferences.putFloat(FRUCHT_REIN_GRAVITY_KEY+graphFileLab.getValue(),val);
      }
    });
    
    iterationsTF.addValueChangeListener(e-> {
      Integer val = (Integer)checkValid(iterationsTF.getValue().trim(),false);
      if(val != null) {
        params.iterations = val;
        layoutButt.setEnabled(true);
        HandlePreferences.putInt(FRUCHT_REIN_ITERATIONS_KEY+graphFileLab.getValue(), val);
      }
    });
    defaultsButt.addClickListener(e-> {
      autoareaCB.setValue(AUTOAREA_DEFAULT); HandlePreferences.removeKey(FRUCHT_REIN_AUTOAREA_KEY+graphFileLab.getValue());
      areaTF.setValue(""+AREA_DEFAULT);      HandlePreferences.removeKey(FRUCHT_REIN_AREA_KEY+graphFileLab.getValue());
      speedTF.setValue(""+SPEED_DEFAULT);    HandlePreferences.removeKey(FRUCHT_REIN_SPEED_KEY+graphFileLab.getValue());
      gravityTF.setValue(""+GRAVITY_DEFAULT);HandlePreferences.removeKey(FRUCHT_REIN_GRAVITY_KEY+graphFileLab.getValue());
      kCoreCombo.setValue(""+KCORE_DEFAULT); HandlePreferences.removeKey(FRUCHT_REIN_KCORE_KEY+graphFileLab.getValue());
      iterationsTF.setValue(""+ITERATIONS_DEFAULT); HandlePreferences.removeKey(FRUCHT_REIN_ITERATIONS_KEY+graphFileLab.getValue());
      
      layoutButt.setEnabled(true);
    });
    
    layoutButt.addClickListener(e-> {
      JsonObject jo = Json.createObject();
      if(stateData != null)
        FruchtermanReingoldState.addData(stateData,jo);
      loadParamsJsonObject(jo);
      FruchtermanReingoldState.addLayoutAgain(jo);
      
      fr.setValue(jo);  // off to client land
    });


    boolean aa = HandlePreferences.getBoolean(FRUCHT_REIN_AUTOAREA_KEY+graphFileLab.getValue(), AUTOAREA_DEFAULT);
    autoareaCB.setValue(aa);
    params.autoarea = aa; // doesn't hit listener like the others
     
    areaTF.setValue(""+HandlePreferences.getInt(FRUCHT_REIN_AREA_KEY+graphFileLab.getValue(),AREA_DEFAULT));
    speedTF.setValue(""+HandlePreferences.getFloat(FRUCHT_REIN_SPEED_KEY+graphFileLab.getValue(),SPEED_DEFAULT));
    gravityTF.setValue(""+HandlePreferences.getFloat(FRUCHT_REIN_GRAVITY_KEY+graphFileLab.getValue(),GRAVITY_DEFAULT));
    kCoreCombo.setValue(""+HandlePreferences.getInt(FRUCHT_REIN_KCORE_KEY+graphFileLab.getValue(),KCORE_DEFAULT));
    iterationsTF.setValue(""+HandlePreferences.getInt(FRUCHT_REIN_ITERATIONS_KEY+graphFileLab.getValue(),ITERATIONS_DEFAULT));

    layoutButt.setEnabled(false);
  }
  
  private void loadParams()
  {
    Integer val = (Integer) checkValid(kCoreCombo.getValue().trim(), false);
    if (val != null)
      params.kcore = val;
    params.autoarea = autoareaCB.getValue();
    val = (Integer) checkValid(areaTF.getValue().trim(), false);
    if (val != null)
      params.area = val;
    Float fval = (Float) checkValid(speedTF.getValue().trim(), true);
    if (fval != null)
      params.speed = fval;
    fval = (Float) checkValid(gravityTF.getValue().trim(), true);
    if (fval != null)
      params.gravity = fval;
    val = (Integer) checkValid(iterationsTF.getValue().trim(), false);
    if(val != null)
      params.iterations = val;
  }
      
  private void loadParamsJsonObject(JsonObject holder)
  {
    if(params.autoarea != null)FruchtermanReingoldState.addAutoArea(params.autoarea,holder);
    if(params.area != null)    FruchtermanReingoldState.addArea(params.area,holder);
    if(params.speed != null)   FruchtermanReingoldState.addSpeed(params.speed,holder);
    if(params.gravity != null) FruchtermanReingoldState.addGravity(params.gravity,holder);
    if(params.kcore != null)   FruchtermanReingoldState.addKcore(params.kcore,holder);
    if(params.iterations != null) FruchtermanReingoldState.addIterations(params.iterations,holder);
  }
  
  private Object checkValid(String s, boolean isFloat)
  {
    try {
      if(isFloat)
        return Float.parseFloat(s);
      //else
      return Integer.parseInt(s);
    }
    catch (NumberFormatException ex) {
      Notification.show("Numerical value required", Notification.Type.ERROR_MESSAGE);
      return null;
    }
  }
}
