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
  
  /* Docushare ne transmet pas les informations sur la requ�te lors de la 
   * g�n�ration des pages par amber. Les chemins sont donc reconstitu�s en
   * utilisant la valeur en configuration, fix�e � l'installation de DocuShare.
   * => on ne peut pas avoir 2 applications tomcat au sein du m�me serveur tomcat.
   * => utilisation d'une classe DSWebServlet d�di�e avec gestion de cette information
   * => enregistrement de l'URL dans les donn�es du Thread au d�but du traitement
   * des requ�tes (on ne peut pas modifier les donn�es de l'objet DSConfig car
   * il peut y avoir un changement de thread entre le d�but de traitement de la
   * requ�te et la g�n�ration de la page). 
   * => modification du fonctionnement de DSConfig: l'url racine est d'abord
   * recherch�e dans les donn�es du Thread.
   * 
   *  NB: Pour modifier le DSConfig utilis� il a fallu remplacer ConfigManager dans
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
