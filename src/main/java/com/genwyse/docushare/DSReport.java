package com.genwyse.docushare;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

import com.genwyse.email.Email;
import com.genwyse.email.EmailServer;
import com.genwyse.poi.Report;
import com.xerox.docushare.impl.util.DSUtil;
/*
 * Gestion d'un modèle de rapport et envoi par email.
 * 
 * Le rapport doit être construit à partir du modèle par une classe d'implémentation.
 * 
 * TODO: ceci est issu des devs CPoR et a réorganiser avant utilisation :
 *  - configuration serveur email par fichier de config DSConfiguration ou config tomcat (?)
 *  - spécialiser en rapport Excel avec méthodes génériques
 *  - construction du corps de l'email en HTML:
 *      - résumé
 *      - tableau de résultats
 */
public class DSReport {
  private static Logger logger = Logger.getLogger(DSReport.class);
  
  public DSReport ()
  {
  }

  protected Report initializeReport (String report_model_resource) throws IOException
  {
    // Récupère le modèle, soit dans le répertoire config de DS, soit par rapport à la racine de l'appli tomcat
    String report_model_path;
    try {
      report_model_path = DSUtil.getConfigLocation(report_model_resource);
    } catch (Exception e) {
      report_model_path = "../../config/" + report_model_resource;
    }
    Report report = new Report (report_model_path);
    logger.debug("Initialisation d'un rapport "+report_model_path);
    return report;
  }
  
  protected static void sendEmail (DSConfiguration ds_config, String prop_prefix, String email_to, String email_subject, String email_text, String html_data)
  {
    sendEmail (ds_config, prop_prefix, email_to, email_subject, email_text, html_data, null);
  }
  
  protected static EmailServer createMailServer (DSConfiguration ds_config, String prop_prefix)
  {
    String email_host = ds_config.getProperty(prop_prefix+"server");
    String email_smtp_port = ds_config.getProperty(prop_prefix+"smtp.port","25");
    String email_user = ds_config.getProperty(prop_prefix+"user");
    String email_pass = ds_config.getProperty(prop_prefix+"pass");
    String email_useSSL = ds_config.getProperty(prop_prefix+"useSSL");
    String email_debug = ds_config.getProperty(prop_prefix+"debug");
    EmailServer email_server = new EmailServer (email_host, email_smtp_port, email_user, email_pass, "1".equals(email_useSSL));
    email_server.setDebug("1".equals(email_debug));
    return email_server;
  }
  
  protected static Email createEmail (DSConfiguration ds_config, String prop_prefix, EmailServer email_server, String email_to, String email_subject) throws MessagingException
  {
    String email_from = ds_config.getProperty(prop_prefix+"from");
    Email email = email_server.createEmail(email_from, email_to, email_subject);
    return email;
  }
  
  protected void sendEmail (DSConfiguration ds_config, String prop_prefix, String email_to, String email_subject, String email_text, Report report)
  {
    sendEmail (ds_config, prop_prefix, email_to, email_subject, email_text, null, report);
  }
  
  protected static void sendEmail (DSConfiguration ds_config, String prop_prefix, String email_to, String email_subject, String email_text, String html_content, Report report)
  {
    File report_file = null;
    
    if (email_to!=null) {
      // Envoi du wb par email
      EmailServer email_server = createMailServer (ds_config, prop_prefix);

      try {
        Email report_email = createEmail (ds_config, prop_prefix, email_server, email_to, email_subject);
        if (email_text!=null) {
          report_email.addContent(email_text);
        }
        if (report!=null) {
          SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
          String tmp_dir = ds_config.getProperty("cpor.tmp");
          report_file = new File(tmp_dir + File.separator + report.getReportName() + "-" + df.format(new Date()) + report.getReportExt());
          report.save (report_file);
          report_email.addContentFile("text/csv", report_file.getName(), report_file);
        }
        
        if (html_content!=null) {
          report_email.addContentHtml (html_content);
        }
        report_email.send();
      } catch (MessagingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    // Supprimer le fichier (sauf si on a demandé à le conserver)
    if (report_file!=null) {
      if (!"1".equals(ds_config.getProperty("cpor.referentiel.keep"))) {
        report_file.delete();
      }
    }
  }
  
}
