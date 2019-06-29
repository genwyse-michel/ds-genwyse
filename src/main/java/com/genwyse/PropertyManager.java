package com.genwyse;

import java.io.File;

import org.apache.log4j.Logger;

public class PropertyManager {
	@SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(PropertyManager.class);
	
	public static Properties getProperties (File prop_file) {
		Properties props = new Properties(prop_file);
		props.reloadProperties();
		return props;
	}
}
