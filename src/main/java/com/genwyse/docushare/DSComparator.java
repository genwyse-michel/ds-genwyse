package com.genwyse.docushare;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Date;

import org.apache.log4j.Logger;

import com.xerox.docushare.DSAuthorizationException;
import com.xerox.docushare.DSException;
import com.xerox.docushare.DSObject;

public class DSComparator implements java.util.Comparator<DSObject>
{
	private static final Logger logger = Logger.getLogger(DSComparator.class);

	private String propName;
	private int sortDirection;
	private Comparator<Object> actualComparator;
	
  public DSComparator(String prop_name, Collator comparator, boolean upwards)
  {
    RuleBasedCollator coll = (RuleBasedCollator)comparator;
    String comparator_rules = coll.getRules();
    try {
      comparator = new RuleBasedCollator("<" + comparator_rules.substring(1));
    }
    catch (ParseException e)
    {
    }
    this.propName = prop_name;
    this.actualComparator = coll;
    this.sortDirection = upwards ? 1 : -1;
  }
  
	public int compare(DSObject o1, DSObject o2) {
		if (logger.isTraceEnabled()) logger.trace(o1.getHandle()+","+o2.getHandle());
	  Object prop1;
    try {
	    prop1 = o1.get(propName);
		  Object prop2 = o2.get(propName);
	  	final String default_prop = "modified_date";
		  
		  if ((prop1 == null) && (prop2 == null)) {
		  	// On n'a pas les propriétés à comparer, comparer les dates
		  	long d1 = ((Date) o1.get(default_prop)).getTime ();
		  	long d2 = ((Date) o2.get(default_prop)).getTime ();
		  	return actualComparator.compare(d1, d2);
		  }
	  
		  if (prop1==null) {
		  	return -sortDirection;
		  }
		  
		  else if (prop2==null) {
		  	return sortDirection;
		  }
		  
		  else if (prop1.toString().length()==0 && prop2.toString().length()==0) {
		  	// Les 2 propriétés existent mais sont vides, => date
		  	long d1 = ((Date) o1.get(default_prop)).getTime ();
		  	long d2 = ((Date) o2.get(default_prop)).getTime ();
		  	return sortDirection*actualComparator.compare(d1, d2);
		  }
		  else {
		  	return sortDirection*actualComparator.compare(prop1, prop2);
		  }
    } catch (DSAuthorizationException e) {
    	logger.error("Erreur en comparant deux objets ",e);
    } catch (DSException e) {
    	logger.error("Erreur en comparant deux objets ",e);
    }
    return 0;
  }
}
