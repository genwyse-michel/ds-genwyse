package com.genwyse.docushare.rules;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.dtrules.entity.IREntity;
import com.dtrules.infrastructure.RulesException;
import com.dtrules.interpreter.RInteger;
import com.dtrules.interpreter.RName;
import com.dtrules.interpreter.RString;
import com.dtrules.session.DTState;
import com.dtrules.session.RSession;
import com.dtrules.session.RuleSet;
import com.dtrules.session.RulesDirectory;
import com.genwyse.docushare.rules.operator.GetProperty;
import com.xerox.docushare.DSAuthenticationException;
import com.xerox.docushare.DSFactory;
import com.xerox.docushare.DSServer;
import com.xerox.docushare.DSSession;

import excel.util.Excel2XML;

public class RulesTable {
	private static Logger logger = Logger.getLogger(RulesTable.class);
	String rootPath = "";
	String dtName = "";
	String rulesetName = "";
	String repositoryPath = "test/rules/repository";
	LinkedList<String> decisionTables = new LinkedList<String>();
	long compileTime = 0;
	
	protected RuleSet        rs = null;
	
	public RulesTable (String dtrules_path, String ruleset_name, String [] decision_tables, String repository_path) throws Exception {
		File dtrules_file = new File (dtrules_path);
		rootPath = dtrules_file.getParent()+File.separator;
		dtName = dtrules_file.getName();
		rulesetName = ruleset_name;
		repositoryPath = repository_path;
		
		//RulesDirectory rd       = new RulesDirectory(rootPath,dtName);
    //RName          rsName   = RName.getRName(rulesetName);
    //rs       = rd.getRuleSet(rsName);
    
    // Crée les opérateurs
    new GetProperty ();
    
    // Définit et compile les tables de décision
    for (String table: decision_tables) {
    	addDecisionTable (table);
    }
    
    // Compile et charge les règles
    compile (null);
    loadRuleset ();
	}
	
	public void addDecisionTable(String tbl)
	{
		decisionTables.add(tbl);
	}
	
	public void compile (String repository_path) throws Exception {
		logger.info("Compilation de "+rulesetName);
		Excel2XML.compile(rootPath,dtName,rulesetName,repository_path==null ? repositoryPath : repository_path);
		compileTime = new Date ().getTime();
		
  }
	
	public void loadRuleset ()
	{
		RulesDirectory rd = new RulesDirectory(rootPath,dtName);
    RName rs_name   = RName.getRName(rulesetName);
    rs = rd.getRuleSet(rs_name);
	}
	
	public void checkUpToDate () throws Exception
	{
		boolean recompile = false;
		for(String table: decisionTables) {
			File table_file = new File (new File (rootPath, rs.getExcel_dtfolder()), table + ".xls");
			long mod_time = table_file.lastModified();
			if (mod_time>compileTime) {
				recompile = true;
				break;
			}
		}
		if (recompile) {
			compile (null);
			// Recharge les règles après compilation
			loadRuleset ();
		}
	}
	
	public RulesSession createSession (DSSession ds_session) throws RulesException {
		RulesSession session = new RulesSession(ds_session, rs);
    return session;
	}
	
  public void executeDecisionTables(RulesSession session)throws RulesException{
  	for(String table : decisionTables){
  		session.execute(table);
  	}
  	
  }
  
	/**
	 * @param args
	 */
	public static void main(String[] args) {
  	logger.addAppender(new org.apache.log4j.ConsoleAppender(new org.apache.log4j.PatternLayout()));
  	logger.setLevel(org.apache.log4j.Level.ALL);
  	logger.info("Logger initialized");
		try {
			RulesTable t = new RulesTable("test/rules/DTRules.xml", "TestDocuShare", new String[]{"TestDocuShare"}, null);
			int iarg = 0;
			DSSession sess = doDocushareLogin(args[iarg++],args[iarg++],args[iarg++],args[iarg++]);
			RulesSession session = t.createSession (sess);
	    session.getState().setState(DTState.DEBUG);
	    //t.addDecisionTable("TestDocuShare");
			
						
			// Entités de test
			IREntity e = ((RSession)session).createEntity(null,"addMenuCheck");
      e.put(RName.getRName("addMenuCheck.parentHandle"), RString.newRString("Collection-65"));
      e.put(RName.getRName("addMenuCheck.childClass"), RString.newRString("Coll_service"));
      session.getState().entitypush(e);
      t.executeDecisionTables(session);     
      logger.info(e.get("parentHandle") + " " + e.get("childClass") + " canAdd:"+session.getState().find("addMenuCheck.canAdd"));
      
      e.put(RName.getRName("canAdd"), RInteger.getRIntegerValue(-1));
      e.put(RName.getRName("addMenuCheck.parentHandle"), RString.newRString("Coll_projet-17"));
      e.put(RName.getRName("addMenuCheck.childClass"), RString.newRString("Collection"));
      session.getState().entitypush(e);
      t.executeDecisionTables(session);     
      logger.info(e.get("parentHandle") + " " + e.get("childClass") + " canAdd:"+session.getState().find("addMenuCheck.canAdd"));
      
      e.put(RName.getRName("canAdd"), RInteger.getRIntegerValue(-1));
      e.put(RName.getRName("addMenuCheck.parentHandle"), RString.newRString("Coll_projet-17"));
      e.put(RName.getRName("addMenuCheck.childClass"), RString.newRString("project_document"));
      session.getState().entitypush(e);
      t.executeDecisionTables(session);     
      logger.info(e.get("parentHandle") + " " + e.get("childClass") + " canAdd:"+session.getState().find("addMenuCheck.canAdd"));
      
      
    } catch (Exception e) {
    	logger.error("Erreur dans le test:",e);
    }
		
	}
	
  private static DSSession doDocushareLogin (String ds_user, String ds_pwd, String ds_port, String ds_host)
  {
          DSServer ds_server;
          if (ds_host==null || "".equals(ds_host)) {
              ds_host = "localhost";
          }
          DSSession ds_session = null;
          try {
            while (ds_session==null) {
              try {
                if (ds_port == null || "".equals(ds_port)) {
                	ds_server = DSFactory.createServer(ds_host);
                }
                else {
                	int port = Integer.valueOf(ds_port);
                	ds_server = DSFactory.createServer(ds_host, port);
                }
                ds_session = ds_server.createSession( "DocuShare", ds_user, ds_pwd);
                logger.info("Connexion à DocuShare sous l'utilisateur "+ds_user);
                return ds_session;
              } catch (DSAuthenticationException e) {
                      return null;
              }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
         return null;
  }

}
