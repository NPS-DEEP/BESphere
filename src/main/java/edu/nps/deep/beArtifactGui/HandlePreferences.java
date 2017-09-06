package edu.nps.deep.beArtifactGui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class HandlePreferences
{
  public static final String BEGUI_RECENT_FILES_KEY = "recentFiles";
  
  public static final String FRIENDS_WINDOW_SIZE_KEY = "windowSize";
  public static final String FRIENDS_FIXED_WINDOW_KEY = "fixedWindow";
  public static final String FRIENDS_KEEP_LONERS_KEY = "keepLoners";
  public static final String FRIENDS_BIG_SUBGRAPHS_KEY = "bigSubGraphs";
  
  public static final String FRUCHT_REIN_AUTOAREA_KEY = "fr_autoArea_";
  public static final String FRUCHT_REIN_AREA_KEY = "fr_area_";
  public static final String FRUCHT_REIN_SPEED_KEY = "fr_speed_";
  public static final String FRUCHT_REIN_GRAVITY_KEY = "fr_gravity_";
  public static final String FRUCHT_REIN_KCORE_KEY = "fr_kcore_";

  public static final int     FRIENDS_WINDOW_SIZE_DEFAULT = 128;
  public static final boolean FRIENDS_FIXED_WINDOW_DEFAULT = false;
  public static final boolean FRIENDS_KEEP_LONERS_DEFAULT = false;
  public static final int     FRIENDS_BIG_SUBGRAPHS_DEFAULT = 10;
  
  private static Preferences prefs;
  private static ArrayList<String> arrList;
  
  public static void init(Class<?> cls)
  {

    arrList = new ArrayList<>();
    
    prefs = Preferences.userNodeForPackage(cls);
    buildPrefsSet();
    //dumpPrefs();
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
  
  @SuppressWarnings("unchecked")
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
  
  public static String getString(String key, String defalt)
  {
    return prefs.get(key, defalt);
  }
  public static Boolean getBoolean(String key, Boolean defalt)
  {
    return prefs.getBoolean(key, defalt);
  }
  public static Integer getInt(String key, Integer defalt)
  {
    return prefs.getInt(key, defalt);
  }
  public static Float getFloat(String key, Float defalt)
  {
    return prefs.getFloat(key, defalt);
  }
  public static void putString(String key, String value)
  {
    prefs.put(key, value);
  }
  public static void putBoolean(String key, Boolean bool)
  {
    prefs.putBoolean(key, bool);
  }
  public static void putInt(String key, Integer intgr)
  {
    prefs.putInt(key, intgr);
  }
  public static void putFloat(String key, Float flt)
  {
    prefs.putFloat(key, flt);
  }
  
  public static void removeKey(String string)
  {
    prefs.remove(string);    
  }
  
  @SuppressWarnings("unused")
  private static void dumpPrefs()
  {
    try {
      String[] sa = prefs.keys();
      for (String s : sa)
        System.out.println(s+" : "+prefs.get(s, null));
    }
    catch(BackingStoreException ex) {
      System.err.println("Error dumping preferences: "+ex.getLocalizedMessage());
    }
  }
}
