package edu.nps.deep.beArtifactGui.d3;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.server.BrowserWindowOpener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import edu.nps.deep.beArtifactGui.py.RunBefriendPyThreaded;
import edu.nps.deep.beArtifactGui.py.RunBefriendPyThreaded.ProcessListener;

public class BeFriendPanel extends _BeFriendPanel implements ProcessListener
{
  public static int WINDOW_WIDTH = 550;
  public static int WINDOW_HEIGHT = 575;
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
    beginButt.addClickListener(e->doBegin());
    cancelButt.addClickListener(e->doCancel());
    resultsCB.addSelectionListener(e-> {
      doGraphButt.setEnabled(true);
      RunBefriendPyThreaded.renderQ.add(new File[] {new File(sourcefile),getGexfFile()});
    });
    BrowserWindowOpener opener = new BrowserWindowOpener(RunBefriendPyThreaded.class);
    opener.extend(doGraphButt);
  }
  
  private RunBefriendPyThreaded runner;
  private void doBegin()
  {
    try {
      runner = new RunBefriendPyThreaded().go(sourcefile, this);
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

  private ArrayList<String> fileNames = new ArrayList<>();
  private      List<File>   files;
  
  @Override
  public void graphList(List<File> list)
  {
    files = list;

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
}
