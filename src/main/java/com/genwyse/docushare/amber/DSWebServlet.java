package com.genwyse.docushare.amber;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xerox.docushare.DSCredential;
import com.xerox.docushare.DSException;
import com.xerox.docushare.amber.util.AmberUtility;
import com.xerox.docushare.monitor.MonitorException;
import com.xerox.docushare.monitor.ServiceStatus;

import jp.co.fujixerox.docushare.amber.ConfigManager;
import jp.co.fujixerox.docushare.amber.vdf.VDFManager;

 
public class DSWebServlet extends jp.co.fujixerox.docushare.amber.DSWebServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7148696159711868328L;
	
	private String amberLocal = "local";
	private String amberApplication = null;
	private String urlBase;

	public DSWebServlet () throws RemoteException, MalformedURLException, DSException, MonitorException {
		super();
	}
	
	public void init() throws ServletException
	{
    setThreadURLBase();
		super.init();
		jp.co.fujixerox.docushare.amber.config.DSConfig config = ConfigManager.getInstance().getDSConfig();
		String templatesPath = config.getTemplatesPath();
		if ((templatesPath == null) || (!(new File(templatesPath).exists()))) {
			throw new ServletException("Templates path not found.");
		}
		
		String [] vdf_paths = new String [] { amberLocal, "system" };
		
		this.vdfManager = new VDFManager(new File(templatesPath), vdf_paths);
		
		AmberUtility amberUtility = AmberUtility.getInstance();
		amberUtility.setVDFManager(this.vdfManager);
		
	}
	public void init(ServletConfig config) throws ServletException
	{ 
    String amber_local = config.getInitParameter("amber.local");
    if (amber_local!=null && !"".equals(amber_local)) {
    	amberLocal = amber_local;
    }
    
    String amber_application = config.getInitParameter("amber.application");
    if (amber_application!=null && !"".equals(amber_application)) {
    	amberApplication = amber_application;
      urlBase = "/" + amberApplication + "/";
    }
		super.init(config);
	}
	
	private void setThreadURLBase ()
	{
    DSConfig cfg = (DSConfig) ConfigManager.getInstance().getDSConfig();
    cfg.setThreadURLBase(urlBase);
	}
	
	public void service(HttpServletRequest request, HttpServletResponse response)
	 throws ServletException, IOException
	{
	  setThreadURLBase();
	  super.service(request, response);
	}
  public boolean startService(String sPassword, ServiceStatus ssStartState, DSCredential dsCredential) throws Exception
  {
    setThreadURLBase();
    
    // TODO: Voir où est défini startService()
    //return super.startService(sPassword, ssStartState, dsCredential);
    throw new Exception ("ERREUR: TODO: startService()");
  }

}
