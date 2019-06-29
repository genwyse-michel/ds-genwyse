package com.genwyse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class Properties extends java.util.Properties {
	/**
   * 
   */
  private static final long serialVersionUID = 8634205148847963495L;

	private static final Logger logger = Logger.getLogger(Properties.class);
	
	private File propertyFile = null;
	private long lastConfigFileCheckTime = 0;
	private long deltaConfigFileCheckTime = 3600000;
  protected String propPrefix = "";
  private boolean autoReload = false;
  private String encoding = "ISO-8859-1";

  public File getPropertyFile() {
    return propertyFile;
  }

  public Properties (long delta_check)
  {
    this (null, -1, false);
  }
  
  public Properties (long delta_check, boolean auto_reload)
  {
    this (null, -1, auto_reload);
  }
  
  public Properties (File prop_file)
  {
    this (prop_file, null, -1, false);
  }
  
  public Properties (File prop_file, String encoding)
  {
    this (prop_file, encoding, -1, false);
  }
  
  public Properties (File prop_file, long delta_check)
  {
    this (prop_file, null, delta_check, false);
  }
  
  public Properties (File prop_file, long delta_check, boolean auto_reload)
  {
    this(prop_file, null, delta_check, auto_reload);
  }
  
	public Properties (File prop_file, String encoding, long delta_check, boolean auto_reload)
	{
    setEncoding(encoding);
		propertyFile = prop_file;
		setDelta (delta_check);
		setAutoReload(auto_reload);
		logger.debug("Le fichier de configuration est:" + prop_file + " Tempo="+deltaConfigFileCheckTime);
	}
	
  public String getPropPrefix ()
  {
    return propPrefix;
  }
  
  public void setPropPrefix (String pref)
  {
    propPrefix = pref;
  }
  
	public void setDelta (long delta_check)
	{
		if (delta_check>=0 && deltaConfigFileCheckTime != delta_check) {
			deltaConfigFileCheckTime = delta_check;
			logger.debug("Le délai est: " + deltaConfigFileCheckTime);
		}		
	}
	
	public boolean reloadProperties ()
	{
		boolean res = false;
  	java.util.Date now = new java.util.Date();
  	long now_time = now.getTime();
  	// Pas de reload si delai = 0
  	if (deltaConfigFileCheckTime>0 && now_time-lastConfigFileCheckTime>deltaConfigFileCheckTime) {
  		logger.debug("Reload " + propertyFile.getAbsolutePath() + ": last=" + lastConfigFileCheckTime + " now=" + now_time);
  		res = loadProperties ();
  		if (res) {
        lastConfigFileCheckTime = now_time;
  		}
  	}
  	return res;
	}
	
  public boolean loadProperties (File prop_file)
  {
    propertyFile = prop_file;
    return loadProperties ();
  }
  public boolean loadProperties ()
  {
		// Load the property file
		if (propertyFile!= null && propertyFile.exists()) {
	
	    InputStreamReader rdr = null;
	    try {
	      rdr = new InputStreamReader(new FileInputStream(propertyFile), encoding);
        load(rdr);
        logger.debug("Load property file " + propertyFile.getName());
        return true;
      } catch (IOException e) {
        logger.error("ExtensionsBeanSetupTask could not load properties; ignoring", e);
        return false;
      } finally {
        if (rdr != null) try { 
          rdr.close(); 
        } catch (IOException e) { } 
      }
    }
		else {
			return false;
		}
  }
  
  public List<String> findPropertyNames(String propPrefix) {
    if (this.propPrefix.length()>0) {
      propPrefix = this.propPrefix + propPrefix;
    }
    List<String> propNames = new LinkedList<String> ();
    Enumeration<?> propEnum = this.propertyNames();
    while (propEnum.hasMoreElements()) {
      String propName = propEnum.nextElement().toString();
      if (propName.startsWith(propPrefix)) {
        propNames.add(propName);
      }
    }
    return propNames;
  }
  
  public String getProperty (String prop)
  {
    if (autoReload) {
      reloadProperties();
    }
    if (propPrefix.length()==0) {
      return super.getProperty(prop);
    }
    String s = super.getProperty(propPrefix + prop);
    if (s==null) {
      s = super.getProperty(prop);
    }
    return s;
  }
  
  public String getProperty (String prop, String def)
  {
    if (this==null) return def;
    String s = (String) getProperty(prop);
    if (s==null) return def;
    return (String) getProperty(prop);
  }
  
  public long getProperty (String prop, long def)
  {
  	String s = getProperty (prop, (String) null);
  	if (s==null) {
  		return def;
  	}
  	long v = Long.parseLong(s);
  	return v;
  }
  
  public int getProperty (String prop, int def)
  {
    String s = getProperty (prop, (String) null);
    if (s==null) {
      return def;
    }
    int v = Integer.parseInt(s);
    return v;
  }
  
  public float getProperty (String prop, float def)
  {
    String s = getProperty (prop, (String) null);
    if (s==null) {
      return def;
    }
    float v = Float.parseFloat(s);
    return v;
  }
  
  public double getProperty (String prop, double def)
  {
    String s = getProperty (prop, (String) null);
    if (s==null) {
      return def;
    }
    double v = Double.parseDouble(s);
    return v;
  }
  
  public boolean isAutoReload() {
    return autoReload;
  }

  public void setAutoReload(boolean autoReload) {
    this.autoReload = autoReload;
  }
  
  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    if (encoding!=null && !"".equals(encoding) && !encoding.equals(this.encoding)) {
      this.encoding = encoding;
      loadProperties ();
    }
  }

  public static void main (String [] args)
  {
  	logger.addAppender(new org.apache.log4j.ConsoleAppender(new org.apache.log4j.PatternLayout()));
  	logger.setLevel(org.apache.log4j.Level.ALL);
  	logger.info("Logger initialized");
  	Properties props = new Properties(new File("test.properties"));
  	props.setDelta(20000);
  	props.reloadProperties();
  	while (true) {
	  	try {
		    Thread.sleep(10000);
	    } catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
	    }
	  	props.reloadProperties();
  	}
  }

}
