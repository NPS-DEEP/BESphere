package edu.nps.deep.beArtifactGui.py;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import edu.nps.deep.beArtifactGui.BeGuiUI;
import edu.nps.deep.beArtifactGui.d3.BeFriendPanel.FriendOptions;
import edu.nps.deep.beArtifactGui.d3.ForceDirectedPanel;
import edu.nps.deep.beArtifactGui.d3.fruchRein.FruchtermanReingoldPanel;

public class RunBefriendPyThreaded extends UI implements Runnable
{
  public static class FriendEdge
  {
    public String source;
    public String target;
    public int weight;
    public FriendEdge(String source,String target,int weight) {this.source=source;this.target=target;this.weight=weight;}
    @Override
    public boolean equals(Object obj)
    {
      return obj instanceof FriendEdge && source.equals(((FriendEdge)obj).source) && target.equals(((FriendEdge)obj).target);
    }   
  }
  public static class FriendNode
  {
    public String id;
    public String label;
    public FriendNode(String id, String label) {this.id=id;this.label=label;}
    @Override
    public boolean equals(Object obj)
    {
      return obj instanceof FriendNode && id.equals(((FriendNode)obj).id) && label.equals(((FriendNode)obj).label);
    }
  }

  public interface ProcessListener {
    public void appendStatus(String s);
    public void jobDone(int errCode);
    public void graphList(List<File> list);
  }
  
  public static String PY3_ENV = "PYTHON3_EXECUTABLE";
  public static String SOURCE_NAME = "befriend.py";
  
  private File resultsDirectory;
  private File inputData;
  private String pySrc;
  private String executable;
  private ProcessListener lis;
  private Process p;
  private Thread thr;
  private FriendOptions opts;
  
  public RunBefriendPyThreaded go(String dataFilePath, ProcessListener lis, FriendOptions opts) throws Exception
  {
    this.lis = lis;
    this.opts = opts;
    makeResultsDir();
    
    inputData = new File(dataFilePath);
    
    if(pySrc == null)
      pySrc = copySource();
    
    if(executable == null)
      executable = findExecutable();
    
    thr = new Thread(this,"RunBefriendPyThreaded");
    thr.setPriority(Thread.NORM_PRIORITY);
    thr.start();
    return this;
  }

  public void cancel()
  {
    if(p != null)
      p.destroy();
    if(thr != null)
      thr.interrupt();
    killDotTimer();
  }
  
  @Override
  public void run()
  {    
    try {
      ArrayList<String> arrLis = new ArrayList<>();
      arrLis.add(executable);
      arrLis.add(pySrc);
      arrLis.add("-f");
      arrLis.add(inputData.getName());
      arrLis.add("-o");
      arrLis.add(resultsDirectory.getAbsolutePath());

      handleOptions(arrLis);
      arrLis.add(inputData.getParent());      

      String[] arguments = new String[arrLis.size()];
      arguments = arrLis.toArray(arguments);
      
      ProcessBuilder pb = new ProcessBuilder(arguments);
     // pb.inheritIO();
      pb.redirectErrorStream(true);
      pb.directory(resultsDirectory);

      lis.appendStatus("Job begun."); //+System.getProperty("line.separator"));  
      beginDots();
      
      p = pb.start();
      IOThreadHandler outputHandler = new IOThreadHandler(p.getInputStream());
      outputHandler.start();
      
      int errCode = p.waitFor();
      if(errCode != 0) {
        killDotTimer();
        lis.jobDone(errCode);
        return;
      }

      processResults(inputData.getAbsolutePath()); // dataFilePath);
      killDotTimer();
      lis.jobDone(errCode);
    }
    catch(InterruptedException ex) {
      killDotTimer();
      lis.appendStatus("Job interrupted");
      lis.jobDone(-1);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      killDotTimer();
      lis.appendStatus("Exception: "+ex.getClass().getSimpleName()+": "+ex.getMessage());
      lis.jobDone(-1);
    }
  }
  
  Timer dotTimer = null;
  private void killDotTimer()
  {
    if(dotTimer != null) {
      dotTimer.cancel();
      dotTimer = null;
    }
  }
  private void beginDots()
  {
    dotTimer = new Timer();
    dotTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        if(dotTimer==null)
          cancel();
        lis.appendStatus(". ");
      }
    }, 1000l, 1000l);
  }
  
  private void handleOptions(ArrayList<String>arrLis)
  {
    arrLis.add("-w");
    arrLis.add(""+opts.windowSize);
    if(opts.fixedWindow)
      arrLis.add("-F");
    if(opts.keepLoners)
      arrLis.add("-k");
    arrLis.add("-m");
    arrLis.add(""+opts.subgraph);
  }
  
  class IOThreadHandler extends Thread
  {
    private InputStream inputStream;

    IOThreadHandler(InputStream inputStream)
    {
      this.inputStream = inputStream;
    }

    public void run()
    {
      Scanner br = null;
      try {
        br = new Scanner(new InputStreamReader(inputStream));
        String line = null;
        while (br.hasNextLine()) {
          line = br.nextLine();
          lis.appendStatus(line + System.getProperty("line.separator"));
        }
      } finally {
        br.close();
      }
    }
  }

  private String findExecutable()
  {
    // Eclipse has to be launched from the command line to find shell variables
    String s = System.getenv(PY3_ENV);
    if(s != null)
      return s;
    return "python3";
  }
  
  private void processResults(String filename) throws Exception
  {
    String pattern = "glob:**.gexf";
    File[] fils = resultsDirectory.listFiles();
    ArrayList<File> arrLis = new ArrayList<>();
    PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
    for(File f : fils) {
      if(matcher.matches(f.toPath())) {
        arrLis.add(f);
        //System.out.println("gexf file found: "+f.getName());
      }
    }
    if(arrLis.isEmpty())
      throw new Exception("No results from befriend.py");
    
    lis.graphList(arrLis);
  }
  
  private void makeResultsDir() throws IOException
  {
    if(resultsDirectory != null)
      return;
    FileAttribute<?> attrs = PosixFilePermissions.asFileAttribute(
        EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE));
            
    resultsDirectory = Files.createTempDirectory("befriend", attrs).toFile();
    resultsDirectory.deleteOnExit();
    //System.out.println("results directory:"+resultsDirectory.getAbsolutePath());
  }
  
  String copySource() throws IOException
  {
    ClassLoader cldr = this.getClass().getClassLoader();
    String input = this.getClass().getPackage().getName()+File.separator;
    input = input.replace('.', File.separatorChar) + SOURCE_NAME;
    Path inputPath = Paths.get(cldr.getResource(input).getPath());
    Path outputPath = Paths.get(resultsDirectory.getAbsolutePath() + File.separator + SOURCE_NAME);   
    
    Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
    outputPath.toFile().deleteOnExit();
    return outputPath.toString();
  }
  
/*******************************************************
  Begin section used as UI for new browser window;
********************************************************/
  
  public RunBefriendPyThreaded() {} // for UI
  private static void buildD3Data(File artifactFile, File gefx) throws Exception
  {
     DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
     DocumentBuilder builder = dbfactory.newDocumentBuilder();
     Document document = builder.parse(gefx);
     
     /* optional
     Schema schema = null;
     String language = XMLConstants.W3C_XML_SCHEMA_NS_URI;
     SchemaFactory factory = SchemaFactory.newInstance(language);
     schema = factory.newSchema(shortest);
     Validator validator = schema.newValidator();
     validator.validate(new DOMSource(document));
     */
     
     HashSet<FriendNode> nodeset = new HashSet<>();
     HashSet<FriendEdge> edgeset = new HashSet<>();
     Element rootgext = document.getDocumentElement();
     Element graph = (Element)rootgext.getElementsByTagName("graph").item(0);
     Element nodes = (Element)graph.getElementsByTagName("nodes").item(0);
     NodeList nlist = nodes.getChildNodes();
     for(int i=0; i<nlist.getLength(); i++) {
       Node n = nlist.item(i);
       if(n.getNodeType() == Node.ELEMENT_NODE) {
         Element el = (Element)n;
//         System.out.println("Node id : "+el.getAttribute("id"));
//         System.out.println("Node label: "+ el.getAttribute("label"));
         nodeset.add(new FriendNode(el.getAttribute("id"),el.getAttribute("label")));
       }
     }
     
     Element edges = (Element)graph.getElementsByTagName("edges").item(0);
     NodeList elist = edges.getChildNodes();
     for(int i=0; i<elist.getLength(); i++) {
       Node n = elist.item(i);
       if(n.getNodeType() == Node.ELEMENT_NODE) {
         Element el = (Element)n;
//         System.out.println("Edge id : "+el.getAttribute("id"));
//         System.out.println("Edge source: "+ el.getAttribute("source"));
//         System.out.println("Edge target: "+el.getAttribute("target"));
//         System.out.println("Edge weight: "+el.getAttribute("weight"));
         edgeset.add(new FriendEdge(el.getAttribute("source"),
                                    el.getAttribute("target"),
                                    Integer.parseInt(el.getAttribute("weight"))));
       }
     }
     myinputPath = artifactFile.getAbsolutePath();
     mygraphFile = gefx.getName();
     mynodeset = nodeset;
     myedgeset = edgeset;
  }
  
  private static String myinputPath,mygraphFile;
  private static HashSet<FriendNode> mynodeset;
  private static HashSet<FriendEdge> myedgeset;
  
  private static ForceDirectedPanel getfPan()
  {
    ForceDirectedPanel pan = new  ForceDirectedPanel(myinputPath,mygraphFile,mynodeset,myedgeset);
    myinputPath = null;
    mynodeset = null;
    myedgeset=null;
    mygraphFile=null;
    return pan;
  }
  
  
  private static FruchtermanReingoldPanel getFrPan()
  {
    FruchtermanReingoldPanel pan = new  FruchtermanReingoldPanel(myinputPath,mygraphFile,mynodeset,myedgeset);
    myinputPath = null;
    mynodeset = null;
    myedgeset=null;
    mygraphFile=null;
    return pan;    
  }
  /* When this class is used as a UI, this is the operative method */
  @Override
  protected void init(VaadinRequest request)
  {
    @SuppressWarnings("unchecked")
    LinkedList<File[]> renderQ = (LinkedList<File[]>)getSession().getAttribute(BeGuiUI.RENDERQ_ATTRIBUTE);
    if(renderQ.isEmpty()) {
      VerticalLayout vl = new VerticalLayout();
      vl.addComponent(new Label("No file"));
      setContent(vl);
      return;
    }
    try {
      File[] files = renderQ.removeFirst();
      Page.getCurrent().setTitle("BE Friends: "+files[1].getName());
      buildD3Data(files[0],files[1]);
      setContent(getfPan());
    }
    catch(Exception ex) {
      VerticalLayout vl = new VerticalLayout();
      vl.addComponent(new Label("Exception: "+ex.getClass().getSimpleName()+": "+ex.getMessage()));
      setContent(vl);
    }
  }
  
  @Theme("beGuiTheme")
  public static class FruchReinUI extends UI
  {
    @Override
    protected void init(VaadinRequest request)
    {
      @SuppressWarnings("unchecked")
      LinkedList<File[]> renderQ = (LinkedList<File[]>)getSession().getAttribute(BeGuiUI.RENDERQ_ATTRIBUTE);
      if(renderQ.isEmpty()) {
        VerticalLayout vl = new VerticalLayout();
        vl.addComponent(new Label("No file"));
        setContent(vl);
        return;
      }
      try {
        File[] files = renderQ.removeFirst();
        Page.getCurrent().setTitle("BE Friends / FruchtermanReingold: "+files[1].getName());
        buildD3Data(files[0],files[1]);
        setContent(getFrPan());
      }
      catch(Exception ex) {
        VerticalLayout vl = new VerticalLayout();
        vl.addComponent(new Label("Exception: "+ex.getClass().getSimpleName()+": "+ex.getMessage()));
        setContent(vl);
      }
      
    }
    
  }
  
}
