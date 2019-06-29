package com.genwyse.docushare.servlet;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class Request {
	private static Logger logger = Logger.getLogger(Request.class);	
	
	private HttpServletRequest request = null;
	
	public Request(HttpServletRequest request) {
		this.request = request;
	}
	
	public HttpServletRequest getRequest () {
		return request;
	}
	
	public String getParameter (String name, String def) {
		String value = request.getParameter(name);
		String result = def;
		
		if (value!=null) {
			result =  value;
		}
		if (logger.isDebugEnabled()) {
			logger.debug(name + "=" + result);
		}
		return result;
	}
	
	public int getParameter (String name, int def) {
		String value = request.getParameter(name);
		Integer result = def;
		if (value!=null) {
			result =  Integer.parseInt(value);
		}
		if (logger.isDebugEnabled()) {
			logger.debug(name + "=" + result);
		}
		return result;
	}
	public boolean getParameter (String name, boolean def) {
		String value = request.getParameter(name);
		Boolean result = def;
		if (value!=null) {
			result =  "1".equals(value) || "y".equals(value) || "Y".equals(value) || Boolean.parseBoolean(value);
		}
		if (logger.isDebugEnabled()) {
			logger.debug(name + "=" + result);
		}
		return result;
	}
}
