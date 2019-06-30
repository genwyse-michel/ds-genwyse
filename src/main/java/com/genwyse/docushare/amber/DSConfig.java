package com.genwyse.docushare.amber;

import com.xerox.docushare.DSException;
import com.xerox.docushare.DSSession;

public class DSConfig extends jp.co.fujixerox.docushare.amber.config.DSConfig {
  /**
   * 
   */
  private static final long serialVersionUID = -1468968092493373820L;

  public DSConfig ()
  {
    super();
  }
  
  public String getUrlAmberBase() {
    String s = getThreadURLBase();
    if (s!=null) {
      return s;
    } else {
      return ((String)get(url_amber_base, "docushare"));
    }
  }

  public String getUrlWebdavBase(DSSession dssession) throws DSException {
    String s = getThreadURLBase();
    if (s!=null) {
      return s;
    } else {
      return getString(url_webdav_base, dssession);
    }
  }
  
  /* Docushare ne transmet pas les informations sur la requête lors de la 
   * génération des pages par amber. Les chemins sont donc reconstitués en
   * utilisant la valeur en configuration, fixée à l'installation de DocuShare.
   * => on ne peut pas avoir 2 applications tomcat au sein du même serveur tomcat.
   * => utilisation d'une classe DSWebServlet dédiée avec gestion de cette information
   * => enregistrement de l'URL dans les données du Thread au début du traitement
   * des requêtes (on ne peut pas modifier les données de l'objet DSConfig car
   * il peut y avoir un changement de thread entre le début de traitement de la
   * requête et la génération de la page). 
   * => modification du fonctionnement de DSConfig: l'url racine est d'abord
   * recherchée dans les données du Thread.
   * 
   *  NB: Pour modifier le DSConfig utilisé il a fallu remplacer ConfigManager dans
   *  amber.jar.
   */
  //
  private static ThreadLocal<String> baseURL = new ThreadLocal<String>() {
    protected synchronized String initialValue() {
      return null;
    }
  };
  
  public void setThreadURLBase (String s) {
    baseURL.set(s);
  }
  
  public String getThreadURLBase () {
    return baseURL.get();
  }
  
  
}
