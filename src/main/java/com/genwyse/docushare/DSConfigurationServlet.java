package com.genwyse.docushare;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class DSConfigurationServlet extends HttpServlet {
  /**
   * Permet d'obtenir depuis une page (via requête Ajax) une information de configuration.
   * Les propriétés sont préfixées suivant le paramétrage du servlet pour limiter les propriétés
   * accessibles (éviter que des informations telles que des logins soient accessibles par ex.).
   */
  private static final long serialVersionUID = 5766769579640886516L;
  private static Logger logger = Logger.getLogger(DSConfigurationServlet.class);

  private static String paramConfiguration = "configuration";
  protected DSConfiguration dsConfiguration = null;
  private String configurationPrefix = ""; // Pour limiter l'accès aux propriétés préfixées

  public void init(ServletConfig config) throws ServletException {
    String config_path = config.getInitParameter(paramConfiguration);
    logger.info ("Demarrage du servlet, chargement de la configuration "+config_path);
    dsConfiguration = new DSConfiguration(config_path);
    if (dsConfiguration!=null) {
      configurationPrefix = dsConfiguration.getProperty("servlet.config.prefix", "");
    }
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String prop_name = request.getParameter("prop");
    String prop_value = "";
    if (dsConfiguration!=null && prop_name!=null && !"".equals(prop_name)) {
      String real_prop_name = configurationPrefix + prop_name;
      prop_value = dsConfiguration.getProperty(real_prop_name, "");
    }
    response.setHeader("Cache-Control","no-cache");
    response.setHeader("Pragma","no-cache");
    response.setContentType("text/plain; charset=utf-8");
    
    PrintWriter writer = response.getWriter();
    
    writer.println(prop_value);
    writer.println();
    writer.flush();
  }
}
