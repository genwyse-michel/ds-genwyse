package jp.co.fujixerox.docushare.amber.object;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.log4j.Logger;

import jp.co.fujixerox.docushare.amber.DSWebException;
import jp.co.fujixerox.docushare.amber.VDFSession;

import com.genwyse.Properties;
import com.genwyse.PropertyManager;
import com.xerox.docushare.DSException;
import com.xerox.docushare.impl.util.DSUtil;


public abstract class VDFExtensionImpl extends VDFBaseProxyObject
{
	private static final Logger logger = Logger.getLogger(VDFExtensionImpl.class);
	private static Properties properties = null;
	private static Class<VDFExtensionImpl> extensionClass = null;
	
	private static final String propClass= "extension.class";
	private static final String propReload= "extension.reload";
	private static final String propPropFile = "genwyse.properties";
	
	public static VDFExtensionImpl loadVDFExtension (VDFSession the_session, VDFRequest the_request) throws DSException, DSWebException
	{
		if (properties==null) {
			String config_path = ""; 
			try {
				config_path = DSUtil.getConfigLocation(propPropFile);
				File prop_file = new File (config_path);

				properties = PropertyManager.getProperties(prop_file);
				logger.info("Le fichier de configuration est:" + config_path);
			} catch (DSException e) {
				logger.error("Le fichier de configuration " + config_path + " n'a pas été trouvé");
			}
		}
		
		long reload_tempo = properties.getProperty(propReload, -1);
		if (reload_tempo>=0) {
			properties.setDelta(reload_tempo);
		}
		
		loadExtensionClass();
		
		VDFExtensionImpl extension_object = createObject(the_session, the_request);
		if (extension_object!=null) {
			logger.debug("Extension: "+extension_object.getClass().getName());
		}
		return extension_object;
	}
	
  public VDFExtensionImpl(VDFSession the_session, VDFRequest the_request)
      throws DSException, DSWebException
  {
	  super(the_session);
	  logger.debug ("Création de VDFExtensionImpl");
  }

  @SuppressWarnings("unchecked")
  private static void loadExtensionClass()
  {
		if (extensionClass==null || properties.reloadProperties()) {
		  String class_name = (String) properties.get(propClass);
		  logger.info ("Class Name="+class_name);
		  
		  if (class_name != null && ! "".equals(class_name)) {
		  	try {
		  		logger.debug("Chargement de "+class_name);
		  		ClassLoader parent_classloader = Thread.currentThread().getContextClassLoader();
		  		ClassLoader my_classloader = new URLClassLoader(new URL[] {}, parent_classloader);
		  		
		  		extensionClass = (Class<VDFExtensionImpl>) my_classloader.loadClass(class_name);

	      } catch (ClassNotFoundException e) {
	      	logger.error ("Classe non trouvée: "+class_name);
	      }
		  }
	  }
  }
  
  public static VDFExtensionImpl createObject (VDFSession the_session, VDFRequest the_request)
  {
		try {
			Constructor<VDFExtensionImpl> constructor = extensionClass.getConstructor(new Class[] { VDFSession.class,  VDFRequest.class});
	    return constructor.newInstance(the_session, the_request);
    } catch (IllegalArgumentException e) {
    	logger.error (e);
    } catch (InstantiationException e) {
    	logger.error (e);
    } catch (IllegalAccessException e) {
    	logger.error (e);
    } catch (InvocationTargetException e) {
    	logger.error (e);
    } catch (SecurityException e) {
    	logger.error (e);
    } catch (NoSuchMethodException e) {
    	logger.error (e);
    }
		return null;
  }
  
  public static void main (String[] args)
  {
  	logger.addAppender(new org.apache.log4j.ConsoleAppender(new org.apache.log4j.PatternLayout()));
  	logger.setLevel(org.apache.log4j.Level.ALL);
  	logger.info("Logger initialized");
  	String docushare_home = "/usr/local/Xerox/DocuShare";
  	System.setProperty("com.xerox.docushare.config", docushare_home + "/config");
  	File ds_lib_dir = new File(docushare_home, "lib");
  	File tomcat_lib_dir = new File(docushare_home, "tomcat/lib");
  	
  	try {
    	ClassLoader parent_classloader = Thread.currentThread().getContextClassLoader();
    	@SuppressWarnings("deprecation")
      ClassLoader my_classloader = new URLClassLoader(new URL[] {ds_lib_dir.toURL(), tomcat_lib_dir.toURL()}, parent_classloader);
    	Thread.currentThread().setContextClassLoader(my_classloader);
    	//VDFSession vdf_session = new VDFSession(null, null);
    	VDFExtensionImpl ext = loadVDFExtension (null, null);
	    System.out.println(ext.toExternalForm());
    } catch (DSException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    } catch (DSWebException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    } catch (MalformedURLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    }
  	
  }
	
}