package edu.nps.deep.beArtifactGui.d3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.vaadin.server.Page;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import edu.nps.deep.beArtifactGui.py.RunBefriendPyThreaded.FriendEdge;
import edu.nps.deep.beArtifactGui.py.RunBefriendPyThreaded.FriendNode;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class ForceDirectedPanel extends _ForceDirectedPanel
{
  public static float WINDOW_WIDTH  = 650f;
  public static float WINDOW_HEIGHT = 725f;
  public static int   GRAPH_WIDTH   = 620;
  public static int   GRAPH_HEIGHT  = 585;
  
  public static void showBeFriends(String windowName, String imagename, String graphFileName, HashSet<FriendNode>nodes, HashSet<FriendEdge>edges)
  {
    Window win = new Window(windowName); 
    ForceDirectedPanel pan = new ForceDirectedPanel(imagename, graphFileName, nodes, edges);

    win.setContent(pan);
    win.setWidth(WINDOW_WIDTH,Unit.PIXELS);
    win.setHeight(WINDOW_HEIGHT,Unit.PIXELS);
    win.center();
    
    UI.getCurrent().addWindow(win);
  }
  public static void show(String windowName, String filename, String email, HashMap<String,Long> emailCloseness)
  {
    Window win = new Window(windowName);
    ForceDirectedPanel pan = new ForceDirectedPanel(filename, email, emailCloseness, GRAPH_WIDTH, GRAPH_HEIGHT);
    win.setContent(pan);
    win.setWidth(WINDOW_WIDTH,Unit.PIXELS);
    win.setHeight(WINDOW_HEIGHT,Unit.PIXELS);
    win.center();
    
    UI.getCurrent().addWindow(win);
  }
  
  public ForceDirectedPanel(String filename, String email, HashMap<String,Long> emailCloseness, int width, int height)
  {    
    filenameLab.setValue("Disk image: "+filename);
    emailLab.setValue("Target email: "+email);
    graphFileLab.setVisible(false);
    ForceDirected fd = new ForceDirected();
    
    JsonArray wrap = Json.createArray();
    wrap.set(0, width);
    wrap.set(1, height);
    wrap.set(2, buildJson(email,emailCloseness, Math.min(width, height)/2 - 80));
    
    fd.setValue(wrap);
    
    graphVL.addComponent(fd);
    fd.addValueChangeListener(()->{
      Notification.show("Value from client: "+fd.getValue());
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
    System.out.println("node: "+email+" 2");
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
  // BeFriends graph

  public ForceDirectedPanel(String diskImagePath, String graphFileName, HashSet<FriendNode> nodes, HashSet<FriendEdge> edges)
  {
    filenameLab.setValue("Disk image: "+diskImagePath);
    emailLab.setVisible(false);
    graphFileLab.setValue("Graph file: "+graphFileName);
    int height = Page.getCurrent().getBrowserWindowHeight();
    int width = Page.getCurrent().getBrowserWindowWidth();
    ForceDirected fd = new ForceDirected();
    
    JsonArray wrap = Json.createArray();
    wrap.set(0, width-80); // adjust for Vaadin margins
    wrap.set(1, height-150);
    wrap.set(2, buildBeFriendJson(nodes,edges));
    
    fd.setValue(wrap);
    
    graphVL.addComponent(fd);
    fd.addValueChangeListener(()->{
      Notification.show("Value from client: "+fd.getValue());
    });
  }
  private JsonObject buildBeFriendJson(HashSet<FriendNode> nodes, HashSet<FriendEdge> edges)
  {
    JsonObject returnObj = Json.createObject();
    JsonArray linkArr = Json.createArray();
    JsonArray nodeArr = Json.createArray();
    returnObj.put("nodes",nodeArr);
    returnObj.put("links", linkArr);
    int idx = 0;
    for(FriendNode fn : nodes) {
      nodeArr.set(idx++,nodeObject(fn.label,1,5));
    }
    idx = 0;
    for(FriendEdge lnk : edges) {
      linkArr.set(idx++, linkObject(lnk.source,lnk.target,lnk.weight));
    }
    return returnObj;
  }
}
