package edu.nps.deep.beArtifactGui.d3;

import static edu.nps.deep.beArtifactGui.HandlePreferences.FRIENDS_BIG_SUBGRAPHS_DEFAULT;
import static edu.nps.deep.beArtifactGui.HandlePreferences.FRIENDS_BIG_SUBGRAPHS_KEY;
import static edu.nps.deep.beArtifactGui.HandlePreferences.FRIENDS_FIXED_WINDOW_DEFAULT;
import static edu.nps.deep.beArtifactGui.HandlePreferences.FRIENDS_FIXED_WINDOW_KEY;
import static edu.nps.deep.beArtifactGui.HandlePreferences.FRIENDS_KEEP_LONERS_DEFAULT;
import static edu.nps.deep.beArtifactGui.HandlePreferences.FRIENDS_KEEP_LONERS_KEY;
import static edu.nps.deep.beArtifactGui.HandlePreferences.FRIENDS_WINDOW_SIZE_DEFAULT;
import static edu.nps.deep.beArtifactGui.HandlePreferences.FRIENDS_WINDOW_SIZE_KEY;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.server.BrowserWindowOpener;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import edu.nps.deep.beArtifactGui.BeGuiUI;
import edu.nps.deep.beArtifactGui.HandlePreferences;
import edu.nps.deep.beArtifactGui.py.RunBefriendPyThreaded;
import edu.nps.deep.beArtifactGui.py.RunBefriendPyThreaded.ProcessListener;

public class BeFriendPanel extends _BeFriendPanel implements ProcessListener
{
  public static int WINDOW_WIDTH = 640;
  public static int WINDOW_HEIGHT = 690;
  public static void show(String windowName, String filename)
  {
    Window win = new Window(windowName);
    BeFriendPanel pan = new BeFriendPanel(filename);
    win.setContent(pan);
    win.setWidth(WINDOW_WIDTH,Unit.PIXELS);
    win.setHeight(WINDOW_HEIGHT,Unit.PIXELS);
    win.center();
    
    UI.getCurrent().addWindow(win);
  }
  private String sourcefile;
  private UI myUI;
  private boolean inittingCB = false;
  
  public BeFriendPanel(String sourcefile)
  {
    myUI = UI.getCurrent();
    this.sourcefile = sourcefile;
    this.sourceFileLab.setValue("Artifact file: "+sourcefile);
    
    helpButt.setDescription(helpToolTip, ContentMode.HTML);  // not supported well by designer
    helpButt.addClickListener(e->Notification.show("Mouse-over the question mark", Notification.Type.HUMANIZED_MESSAGE));
    
    beginButt.addClickListener(e->doBegin());    
    cancelButt.addClickListener(e->doCancel());
    
    resultsCB.addSelectionListener(resultsCBLis);
    
    BrowserWindowOpener opener = new BrowserWindowOpener(RunBefriendPyThreaded.FruchReinUI.class); //RunBefriendPyThreaded.class);
    opener.extend(doGraphButt);

    doGraphButt.addClickListener(e->resultsCBLis.selectionChange(null));  // this just puts the selection in place if there is another render of the same file

    handlePrefs(sourcefile);
    
    winSizeTF.addValueChangeListener(e->saveIntPref(e));
    subgraphsTF.addValueChangeListener(e->saveIntPref(e));
    fixedWinCB.addValueChangeListener(e->{if(!initting)HandlePreferences.putBoolean(sourcefile+"_"+FRIENDS_FIXED_WINDOW_KEY,fixedWinCB.getValue());});
    lonersCB.addValueChangeListener(e->{if(!initting)HandlePreferences.putBoolean(sourcefile+"_"+FRIENDS_KEEP_LONERS_KEY,lonersCB.getValue());});

    restoreDefaultsButt.addClickListener(e-> {
      HandlePreferences.putInt(sourcefile+"_"+FRIENDS_WINDOW_SIZE_KEY,   FRIENDS_WINDOW_SIZE_DEFAULT);
      HandlePreferences.putInt(sourcefile+"_"+FRIENDS_BIG_SUBGRAPHS_KEY, FRIENDS_BIG_SUBGRAPHS_DEFAULT);
      HandlePreferences.putBoolean(sourcefile+"_"+FRIENDS_FIXED_WINDOW_KEY, FRIENDS_FIXED_WINDOW_DEFAULT);
      HandlePreferences.putBoolean(sourcefile+"_"+FRIENDS_KEEP_LONERS_KEY,  FRIENDS_KEEP_LONERS_DEFAULT);
      initting=true;
      handlePrefs(sourcefile);
      initting=false;
    });
  }
  private SingleSelectionListener<String> resultsCBLis = new SingleSelectionListener<String>() {
    @Override
    public void selectionChange(SingleSelectionEvent<String> event)
    {
      if(inittingCB) return;
      @SuppressWarnings("unchecked")
      LinkedList<File[]> renderQ = (LinkedList<File[]>) myUI.getSession().getAttribute(BeGuiUI.RENDERQ_ATTRIBUTE);  //not using as a queue
      renderQ.clear();
      renderQ.add(new File[] {new File(sourcefile),getGexfFile()});      
    }    
  };
  
  private boolean initting = false;
  private void handlePrefs(String sourcefile)
  {
    winSizeTF.setValue(""+  HandlePreferences.getInt(sourcefile+"_"+FRIENDS_WINDOW_SIZE_KEY,   FRIENDS_WINDOW_SIZE_DEFAULT));
    subgraphsTF.setValue(""+HandlePreferences.getInt(sourcefile+"_"+FRIENDS_BIG_SUBGRAPHS_KEY, FRIENDS_BIG_SUBGRAPHS_DEFAULT));
    
    fixedWinCB.setValue(HandlePreferences.getBoolean(sourcefile+"_"+FRIENDS_FIXED_WINDOW_KEY, FRIENDS_FIXED_WINDOW_DEFAULT));
    lonersCB.setValue(  HandlePreferences.getBoolean(sourcefile+"_"+FRIENDS_KEEP_LONERS_KEY,  FRIENDS_KEEP_LONERS_DEFAULT));
  }
  
  private void saveIntPref(ValueChangeEvent<String> e)
  {
    if(initting) return;
    int iVal;
    try {
      iVal = Integer.parseInt(e.getValue());
    }
    catch(NumberFormatException nfe) {
      Notification.show("Not an integer", Notification.Type.ERROR_MESSAGE);
      return;
    }
    if(e.getComponent() == winSizeTF) {
      HandlePreferences.putInt(sourcefile+"_"+FRIENDS_WINDOW_SIZE_KEY,iVal);
    }
    else if(e.getComponent() == subgraphsTF) {
      HandlePreferences.putInt(sourcefile+"_"+FRIENDS_BIG_SUBGRAPHS_KEY,iVal);     
    }
  }

  public static class FriendOptions {
    public int windowSize = FRIENDS_WINDOW_SIZE_DEFAULT;
    public int subgraph = FRIENDS_BIG_SUBGRAPHS_DEFAULT;
    public Boolean fixedWindow = FRIENDS_FIXED_WINDOW_DEFAULT;
    public Boolean keepLoners = FRIENDS_KEEP_LONERS_DEFAULT;
  }
  
  private FriendOptions getOptions()
  {
    FriendOptions opts = new FriendOptions();
    try {
      opts.windowSize = Integer.parseInt(winSizeTF.getValue().trim());
      opts.subgraph = Integer.parseInt(subgraphsTF.getValue().trim());
      opts.fixedWindow = fixedWinCB.getValue();
      opts.keepLoners = lonersCB.getValue();
    }
    catch(NumberFormatException ex) {
      Notification.show("Integer format error, using default", Notification.Type.ERROR_MESSAGE);
    }
    return opts;
  }
  
  private RunBefriendPyThreaded runner;
  private void doBegin()
  {
    beginButt.setEnabled(false);
    cancelButt.setEnabled(true);
    try {
      if(runner == null)
        runner = new RunBefriendPyThreaded();
      runner.go(sourcefile, this, getOptions());
    }
    catch(Exception ex) {
      Notification.show("Error from python runner: "+ex.getClass().getSimpleName()+" "+ex.getMessage());
    }
    cancelButt.setEnabled(true);
  }

  private void doCancel()
  {
    beginButt.setEnabled(true);
    cancelButt.setEnabled(false);
    runner.cancel();
    cancelButt.setEnabled(false);
  }
  
  private void appendToTA(String s, Boolean cancelButtState)
  {
    ta.setValue(ta.getValue()+s);
    ta.setCursorPosition(ta.getValue().length());
  }
  
  @Override
  public void appendStatus(String s)
  {
    myUI.access(()->{  // not in gui thread
      appendToTA(s,null);
    });
  }

  @Override
  public void jobDone(int errCode)
  {
    myUI.access(()->{  // not in gui thread
      appendToTA("Job complete."+System.getProperty("line.separator"),false);
      beginButt.setEnabled(true);
      cancelButt.setEnabled(false);
    });
  }

  private TreeMap<String,File> files = new TreeMap<>();
  
  // When a job is complete, the full file list comes back, including previously generated ones
  @Override
  public void graphList(List<File> list)
  {
    if(list.isEmpty()) {
      doGraphButt.setEnabled(false);
      return;
    }
    myUI.access(()-> {  // not in ui thread
      files.clear();
    
      String first = null;
      for(File f : list) {
        String name = f.getName();
        if(first == null)
          first = name;
        files.put(name, f);
      }
      //inittingCB = true;
      resultsCB.setItems(files.keySet());
      if(first != null)
        resultsCB.setSelectedItem(first);
      //inittingCB = false;
      
      doGraphButt.setEnabled(true);
    });
  }
  
  private File getGexfFile()
  {
    Optional<String> opt = resultsCB.getSelectedItem();
    if(opt.isPresent()) {
      String nm = opt.get();
      File f = files.get(nm);
      if(f != null)
        return f;
    }
    throw new RuntimeException("Program error in BeFriendPanel.getGexfFile()");
  }
  
  public static String helpToolTip =
      "BE Friend options"+
      "<ul>"+
      "<li><b>window size</b></li> window size description here"+
      "<li><b>fixed window</b></li><i>(Deprecated)</i>Only link addresses found within the distance defined by window size."+
      "<li><b>keep loners</b></li>Keep all nodes, even those with no edges to other nodes."+
      "<li><b>big subgraph #</b></li>Save a separate gexf file for the top N largest connected components of the graph."+
      "</ul>";
}
