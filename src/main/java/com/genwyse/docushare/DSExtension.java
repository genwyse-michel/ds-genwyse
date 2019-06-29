package com.genwyse.docushare;

import java.util.Map;

import org.apache.log4j.Logger;

import com.xerox.docushare.DSAuthorizationException;
import com.xerox.docushare.DSException;
import com.xerox.docushare.DSHandle;
import com.xerox.docushare.DSObject;
import com.xerox.docushare.DSSession;
import com.xerox.docushare.object.DSCollection;
import com.xerox.docushare.object.DSDocument;

public class DSExtension {
	private static final Logger logger = Logger.getLogger(DSExtension.class);
	
	protected DSSession dsSession = null;
	public DSExtension (DSSession the_session) {
		logger.debug ("Création de DSExtension");
		dsSession = the_session;
	}
	
	public String info () 
	{
		String class_name = this.getClass().getCanonicalName();
		String version = "$Revision$";
		return class_name + version;
	}
	
	public DSObject getObject (String object_id) throws DSAuthorizationException, DSException
	{
		DSHandle h = new DSHandle(object_id);
		DSObject o = dsSession.getObject(h);
		return o;
	}
	
	public DSCollection getCollection (String object_id) throws Exception
	{
		DSObject obj = getObject(object_id);
		if (obj==null) return null;
		
		if (!DSCollection.class.isInstance(obj)) {
			String err = "Paramètre parent incorrect (doit être un handle de collection):"+object_id;
			logger.error(err);
			throw new Exception(err);
		}
		
		DSCollection coll = (DSCollection) obj;
		return coll;
	}
	
	public DSDocument getDocument (String object_id) throws Exception
	{
		DSObject obj = getObject(object_id);
		if (obj==null) return null;
		
		if (!DSDocument.class.isInstance(obj)) {
			String err = "Paramètre parent incorrect (doit être un handle de document):"+object_id;
			logger.error(err);
			throw new Exception(err);
		}
		
		DSDocument doc = (DSDocument) obj;
		return doc;
	}
	
	public String getArg(Map<String,Object>  argmap, String arg, String def)
	{
		String s = (String) argmap.get(arg);
		if (s==null) s = def;
		return s;
	}
	
	public int getArg(Map<String,Object>  argmap, String arg, int def)
	{
		String s = (String) argmap.get(arg);
		if (s!=null) {
			int v = Integer.parseInt(s);
			return v;
		}
		else {
			return def;
		}
	}
	

}
