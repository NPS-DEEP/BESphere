package edu.nps.deep.beArtifactGui;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.vaadin.ui.Grid;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import edu.nps.deep.beArtifactGui.d3.ForceDirectedPanel;
public class InfoWindow extends _InfoWindow
{
  public static void show(String filename, HashMap<String, Long[]> email2OffsetMap,
      TreeMap<Long, String> offset2EmailMap, TreeMap<Integer,String>frequencyMap)
  {
    Window win = new Window(filename);
    InfoWindow pan = new InfoWindow(filename,email2OffsetMap,offset2EmailMap,frequencyMap);
    win.setContent(pan);
    win.setWidth("650px");
    win.setHeight("725px");
    win.center();
    
    UI.getCurrent().addWindow(win);
  }
  
  private HashMap<String, Long[]> email2OffsetMap;
  private TreeMap<Long, String> offset2EmailMap;
  //private TreeMap<Integer,String>frequencyMap;
  public long NEARNESS = 1000l; //500l;
  
  public InfoWindow(String filename, HashMap<String, Long[]> email2OffsetMap, 
      TreeMap<Long, String> offset2EmailMap, TreeMap<Integer,String>frequencyMap)
  {
    super();
    System.out.println("InfoWindow: filename: "+filename+" email2OffsetMap.size: "+email2OffsetMap.size()+" offset2EmailMap.size: "+offset2EmailMap.size()
                       + " frequencyMap.size: "+frequencyMap.size());
    this.email2OffsetMap = email2OffsetMap;
    this.offset2EmailMap = offset2EmailMap;
    //this.frequencyMap = frequencyMap;
    
    nameTF.setValue(filename);
    numArtifactsTF.setValue(""+offset2EmailMap.size());
    numEmailsTF.setValue(""+email2OffsetMap.size());
    
    Set<Entry<Integer,String>> set = frequencyMap.descendingMap().entrySet();
    @SuppressWarnings("unchecked")
    Grid<Entry<Integer,String>> mygrid = frequencyGrid;
    mygrid.setItems(set);
    mygrid.addColumn(Map.Entry::getValue).setCaption("Email");
    mygrid.addColumn(Map.Entry::getKey).setCaption("Frequency");
    
    mygrid.addSelectionListener(e->graphButt.setEnabled(true));
    
    graphButt.addClickListener(e-> {
      Set<Entry<Integer,String>> sset = mygrid.getSelectedItems();
      handleEmailGraph(sset);
    });   
  }
  
  private void handleEmailGraph(Set<Entry<Integer, String>> sset)
  {
    String email = sset.iterator().next().getValue();
    System.out.println(email);
    Long[] offsets = email2OffsetMap.get(email);
    System.out.println(""+offsets.length+" locations");
    
    HashMap<String,Long> emailCloseness = new HashMap<>();
    
    for(long off : offsets) {
      findNeighbor(email,off,emailCloseness);
    }
    System.out.println("Neighbor count: "+emailCloseness.size());
    
    ForceDirectedPanel.show("Email Proximity Graph", nameTF.getValue(), email, emailCloseness);
  }

  private void findNeighbor(String email, long off, HashMap<String,Long> emailCloseness)
  {
    long start = Math.max(off-NEARNESS, 0);
    long end = off + NEARNESS;

    for(long o=start; o<end; o++) {
      long diff = Math.abs(off-o);
      String emailAtOff = offset2EmailMap.get(o);
      if(emailAtOff == null)
        continue;
      
      else if(!emailAtOff.equals(email) && !emailAtOff.contains("emirates.net.ae")) {
        Long val = emailCloseness.get(emailAtOff);  // see if already there
        if(val == null || val > diff) {
          emailCloseness.put(emailAtOff, diff);
        }
      }
    }
  }
}
