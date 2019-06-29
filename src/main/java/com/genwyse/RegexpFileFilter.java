package com.genwyse;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexpFileFilter implements FilenameFilter {
  private Pattern pattern = null;
  private boolean acceptDirectories = true;
  private boolean acceptFiles = true;
 

  public RegexpFileFilter(String regexp, boolean acceptDirectories, boolean acceptFiles) {
    this.acceptDirectories = acceptDirectories;
    this.acceptFiles = acceptFiles;
    if (regexp!=null) {
      pattern = Pattern.compile(regexp);
    }
  }

  @Override
  public boolean accept(File dir, String name) {
    if (pattern!=null) {
      Matcher m = pattern.matcher(name);
      if (!m.matches()) {
        return false; 
      }
    }
    
    File file = new File(dir, name);
    if (!acceptDirectories && file.isDirectory()) {
      return false;
    }
    if (!acceptFiles && file.isFile()) {
      return false;
    }
    return true;
  }

}
