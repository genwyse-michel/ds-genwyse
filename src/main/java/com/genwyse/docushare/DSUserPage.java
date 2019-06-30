package com.genwyse.docushare;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xerox.docushare.DSException;
import com.xerox.docushare.amber.pages.common.SessionPage;
import com.xerox.docushare.impl.util.DSLogger;

public abstract class DSUserPage extends SessionPage {
  
  /**
   * 
   */
  private static final long serialVersionUID = 4308657011750110303L;
  protected DSLogger mLogger = (DSLogger)DSLogger.getInstance(DSUserPage.class);
  private static final String propPropFile = "genwyse/genwyse.properties";
  protected static DSConfiguration dsConfiguration = new DSConfiguration(propPropFile);

  @Override
  protected boolean hasURLCheck() {
    return false;
  }

  protected boolean preprocess(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, DSException
  {
    boolean result = true;
    if (!super.preprocess(aRequest, aResponse)) {
      return false;
    }

    authenticate();
    return result;
  }
  
  protected void authenticate()
  {
    this.mDSSession = getDSSessionFromCookies();
    if (this.mDSSession == null) {
      throw redirect(this.mLoginPage, new Exception(getMessage("InvalidSessionError")));
    }
  }
  
  protected void gotoPage(HttpServletResponse response, String address) throws ServletException, IOException {
    response.sendRedirect(address);
  }
  
}
