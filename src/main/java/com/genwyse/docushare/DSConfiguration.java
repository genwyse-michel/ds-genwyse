package com.genwyse.docushare;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.genwyse.tools.Properties;
import com.xerox.docushare.impl.util.DSUtil;

public class DSConfiguration extends Properties {
  /**
   * 
   */
  private static final long serialVersionUID = 1953113053454454621L;
  private static Logger logger = Logger.getLogger(DSConfiguration.class);
  protected File propertyFile = null; 
  
  public DSConfiguration (String config_file) {
    this (config_file, "../../config/");
  }
  
  public static File findConfigFile (String config_name, String default_root) 
  {
    File config_file = new File(config_name);
    if (!config_file.isFile()) {
      try {
        config_file = new File (DSUtil.getConfigLocation(config_name));
      } catch (Exception e) {
        logger.error("DSConfiguration: Pas de config path");
        config_file = new File (default_root, config_name);
      }
    }
    return config_file;
  }
  
  public static File findConfigDir (String config_name, String default_root) 
  {
    File config_file = new File(config_name);
    if (!config_file.isDirectory()) {
      try {
        config_file = new File (DSUtil.getConfigLocation(config_name));
      } catch (Exception e) {
        logger.error("DSConfiguration: Pas de config path");
        config_file = new File (default_root, config_name);
      }
    }
    return config_file;
  }

  public DSConfiguration (String config_name, String default_root)
  {
    this(findConfigFile(config_name, default_root));
  }
  
  public DSConfiguration (File config_file)
  {
    super(300, true); // Rechargement toutes les 5 mn
    propertyFile = config_file;
    loadProperties(propertyFile);
    try {
      logger.info("Le fichier de configuration est:" + propertyFile.getCanonicalPath());
    } catch (IOException e) {}
  }
  
  public String getAbsolutePath ()
  {
    if (propertyFile!=null) {
      return propertyFile.getAbsolutePath();
    }
    else {
      return "";
    }
  }
}
