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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.vaadin.data.HasValue.ValueChangeEvent;
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
  public BeFriendPanel(String sourcefile)
  {
    myUI = UI.getCurrent();
    this.sourcefile = sourcefile;
    this.sourceFileLab.setValue("Artifact file: "+sourcefile);
    
    helpButt.setDescription(helpToolTip, ContentMode.HTML);  // not supported well by designer
    helpButt.addClickListener(e->Notification.show("Mouse-over the question mark", Notification.Type.HUMANIZED_MESSAGE));
    
    beginButt.addClickListener(e->doBegin());    
    cancelButt.addClickListener(e->doCancel());
    resultsCB.addSelectionListener(e-> {
      doGraphButt.setEnabled(true);
      @SuppressWarnings("unchecked")
      LinkedList<File[]> renderQ = (LinkedList<File[]>) myUI.getSession().getAttribute(BeGuiUI.RENDERQ_ATTRIBUTE);
      renderQ.add(new File[] {new File(sourcefile),getGexfFile()});
    });
    BrowserWindowOpener opener = new BrowserWindowOpener(RunBefriendPyThreaded.class);
    opener.extend(doGraphButt);
    
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
    //resultsCB.setItems(new ArrayList<String>()); // empty
    try {
      runner = new RunBefriendPyThreaded().go(sourcefile, this, getOptions());
    }
    catch(Exception ex) {
      Notification.show("Error from python runner: "+ex.getClass().getSimpleName()+" "+ex.getMessage());
    }
    cancelButt.setEnabled(true);
  }

  private void doCancel()
  {
    runner.cancel();
    cancelButt.setEnabled(false);
  }
  
  private void appendToTA(String s, Boolean cancelButtState)
  {
    myUI.access(()-> {
      ta.setValue(ta.getValue()+s);
      ta.setCursorPosition(ta.getValue().length());
      if(cancelButtState != null)
        cancelButt.setEnabled(cancelButtState);
    });
  }
  @Override
  public void appendStatus(String s)
  {
    appendToTA(s,null);
  }

  @Override
  public void jobDone(int errCode)
  {
    appendToTA("Job complete."+System.getProperty("line.separator"),false);
  }

  private      List<File>   files;
  
  @Override
  public void graphList(List<File> list)
  {
    files = list;
    ArrayList<String> fileNames = new ArrayList<>();
    for(File f : list)
      fileNames.add(f.getName());
    resultsCB.setItems(fileNames);
    resultsCB.setSelectedItem(fileNames.get(0));
  }
 
  private File getGexfFile()
  {
    Optional<String> opt = resultsCB.getSelectedItem();
    if(opt.isPresent()) {
      String nm = opt.get();
      for(File f : files)
        if(f.getName().equals(nm))
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
