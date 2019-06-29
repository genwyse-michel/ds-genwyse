package com.genwyse.docushare.rules;

import com.dtrules.infrastructure.RulesException;
import com.dtrules.session.RSession;
import com.dtrules.session.RuleSet;
import com.xerox.docushare.DSSession;

public class RulesSession extends RSession {
	
	private DSSession dsSession = null;

	public RulesSession(DSSession ds_session, RuleSet _rs) throws RulesException {
	  super(_rs);
	  dsSession = ds_session;
  }
	
	public DSSession getDSSession () { return dsSession; }

}
