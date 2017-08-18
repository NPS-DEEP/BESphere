package edu.nps.deep.beArtifactGui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Notification;

import edu.nps.deep.beArtifactGui.d3.BeFriendPanel;

public class MainP extends MainPanel
{
  //private static final long serialVersionUID = 1L;

  public MainP()
  {
	  super();
	  HandlePreferences.init(getClass());
	  List<String> list = HandlePreferences.getRecentList();
	  fileNameCombo.setItems(list);
	  if(!list.isEmpty())
	    fileNameCombo.setSelectedItem(list.get(0));
	  
	  fileNameCombo.addValueChangeListener(e->confirmPath(null));
	  
	  confirmButt.addClickListener(e -> confirmPath(e));
	  
	  generateButt.addClickListener(e -> generateGraph(e));
	  
	  generateBEFriendButt.addClickListener(e->generateBefriendGraph());
	  
	  fileNameCombo.setNewItemHandler(inputString -> {
	    HandlePreferences.addToRecentList(inputString);
	    fileNameCombo.setItems(HandlePreferences.getRecentList());
	    fileNameCombo.setSelectedItem(inputString);
	  });
  }

  private HashMap<String,Long[]> email2OffsetMap = new HashMap<>();
  private TreeMap<Long,String> offset2EmailMap = new TreeMap<>();

  private void generateBefriendGraph()
  {
    String filename = fileNameCombo.getValue();
    if(filename==null || filename.length()<=0)
      return;
    
    try {
      //RunBefriendPy.go(filename);
      BeFriendPanel.show("BE FRIEND", filename);
    }
    catch(Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  private void generateGraph(ClickEvent e)
  {
    if(!confirmPath(null))
      return;
    fillMaps();
    displaySpecs();
  }
  
  private void displaySpecs()
  {
    InfoWindow.show(fileNameCombo.getValue(), email2OffsetMap,offset2EmailMap,frequencyMap);
  }
  private void fillMaps()
  {
    email2OffsetMap.clear();
    offset2EmailMap.clear();
    frequencyMap.clear();
    BufferedReader br;
    try {
      File f = new File(fileNameCombo.getValue());
      br = new BufferedReader(new FileReader(f));
      String line;
      while((line = br.readLine()) != null) {
        if(line.startsWith("#"))
          continue;
        try {
          String [] sa = line.split("\t");
          if(sa.length >= 2) {
            long offset = Long.parseLong(sa[0]);  //throws if bogus
            String email = sa[1];
            if(!email.contains("@"))
              continue;
            String emaillc = email.toLowerCase();
            //System.out.println(emaillc+" "+offset);
            // put in one map
            offset2EmailMap.put(offset, emaillc);
            
            // put in other map
            Long[] la = email2OffsetMap.get(emaillc);
            if(la == null) {
              email2OffsetMap.put(emaillc, new Long[] {offset});
            }
            else {
              Long[] newLa = Arrays.copyOf(la, la.length+1);
              newLa[newLa.length-1] =  offset;
              email2OffsetMap.put(emaillc, newLa);
            }
          }
        }
        catch(Exception ex) {
          
        }
      }
      br.close();
    }
    catch(IOException ex) {
    }
    
    
    Set<String> ks = email2OffsetMap.keySet();    
    for(String s : ks)
      handleFrequencyList(s,email2OffsetMap.get(s).length);
    
  }
  
  
  private TreeMap<Integer,String> frequencyMap = new TreeMap<>();
  
  private void handleFrequencyList(String email, int freq)
  {
    Integer lastKey = null;
    Integer firstKey = 0;
    if(frequencyMap.size() > 0) {
      lastKey =  frequencyMap.lastKey();
      firstKey = frequencyMap.firstKey();
    }
    
    if(lastKey == null || /*frequencyMap.size()<20 ||*/ (freq>firstKey)) { //lastKey)) {
      frequencyMap.put(freq, email);
      if(frequencyMap.size() > 20)
        frequencyMap.pollFirstEntry();   // removes first (lowest)
    }
  }
  
  private boolean confirmPath(ClickEvent e)
  {
    String path = fileNameCombo.getValue();
    if(path != null && path.length()>0) {
      File f = new File(path);
      if(f.exists() && f.isFile()) {
        Notification notif = new Notification("SUCCESS", "File present at "+path,Notification.Type.HUMANIZED_MESSAGE);
        notif.setPosition(Position.TOP_CENTER);
        notif.show(Page.getCurrent());
        generateButt.setEnabled(true);
        generateBEFriendButt.setEnabled(true);
        HandlePreferences.addToRecentList(path);
        return true;
      }
    }

    Notification notif = new Notification("ERROR","File at <"+path+"> not found.",Notification.Type.ERROR_MESSAGE);
    notif.setPosition(Position.TOP_CENTER);
    notif.show(Page.getCurrent());
    generateButt.setEnabled(false);
    generateBEFriendButt.setEnabled(false);
    return false;
    }
}
