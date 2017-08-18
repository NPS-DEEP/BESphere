package edu.nps.deep.beArtifactGui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

public class HandlePreferences
{
  public static final String BEGUI_RECENT_FILES_KEY = "recentFiles";

  private static Preferences prefs;
  private static ArrayList<String> arrList;
  
  public static void init(Class<?> cls)
  {

    arrList = new ArrayList<>();
    
    prefs = Preferences.userNodeForPackage(cls);
    buildPrefsSet();
  }
  public static void addToRecentList(String s)
  {
    arrList.remove(s);
    arrList.add(0, s);  // put at front
    updateRecentPrefs();
  }
  
  private static void updateRecentPrefs()
  {
    StringBuilder sb = new StringBuilder();
    Iterator<String> itr = arrList.iterator();
    while(itr.hasNext()) {
      sb.append(itr.next());
      sb.append(',');
    }
    String s = sb.toString();
    s = s.substring(0,s.length()-1);
    prefs.put(BEGUI_RECENT_FILES_KEY, s);
  }
  
  public static List<String> getRecentList()
  {
    return (List<String>)arrList.clone();
  }

  private static void buildPrefsSet()
  {
    String s = prefs.get(BEGUI_RECENT_FILES_KEY, "");
    if(s.length()>0) {
      String[] sa = s.split(",");
      for(String ss : sa) {
        arrList.add(ss);
      }
    }     
  }
}
