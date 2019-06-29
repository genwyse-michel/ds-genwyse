package com.genwyse.docushare.rules.operator;

import com.dtrules.infrastructure.RulesException;
import com.dtrules.interpreter.IRObject;
import com.dtrules.interpreter.RInteger;
import com.dtrules.interpreter.RString;
import com.dtrules.interpreter.operators.ROperator;
import com.dtrules.session.DTState;
import com.genwyse.docushare.rules.RulesSession;
import com.xerox.docushare.DSAuthorizationException;
import com.xerox.docushare.DSException;
import com.xerox.docushare.DSHandle;
import com.xerox.docushare.DSObject;
import com.xerox.docushare.DSSession;
import com.xerox.docushare.object.DSCollection;

public class GetProperty extends ROperator {
	public GetProperty(){
    super("getProperty");
    alias("gp");
	}
	
	private IRObject toRulesObject(String s)
	{
		return RString.newRString(s);
	}
	
	private IRObject toRulesObject(int v)
	{
		return RInteger.getRIntegerValue(v);
	}
	
	public void execute(DTState state) throws RulesException {
		String handle = state.datapop().stringValue();
		String prop_name = state.datapop().stringValue();
		
		RulesSession session = (RulesSession) state.getSession();
		DSSession ds_session = session.getDSSession();
		
		try {
			IRObject result_object = null;
	    DSObject obj = ds_session.getObject(new DSHandle (handle));
	    if ("dsclass".equals(prop_name)) {
	    	result_object = toRulesObject(obj.getDSClass().getName());
	    }
	    else if ("childCount".equals(prop_name)) {
	    	if (obj.isTypeOf("Collection")) {
	    		DSCollection coll = (DSCollection) obj;
	    		result_object = toRulesObject(coll.getChildCount());
	    	}
	    	else {
	    		result_object = toRulesObject(0);
	    	}
	    }
	    else {
	    	String prop_value = (String) obj.get(prop_name);
	    	if (prop_value==null) {
	    		result_object = toRulesObject("");
	    	}
	    	else {
	    		result_object = toRulesObject(prop_value);
	    	}
	    }
	    if (result_object==null) {
	    	result_object = toRulesObject("");
	    }
	    state.datapush(result_object);
	    return;
    } catch (DSAuthorizationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    } catch (DSException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    }
		// En cas d'erreur, rendre une chaine vide (?)
		state.datapush(RString.newRString(""));
	}

}
