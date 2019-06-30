/** 
 * Copyright 2004-2010 DTRules.com, Inc.
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");  
 * you may not use this file except in compliance with the License.  
 * You may obtain a copy of the License at  
 *   
 *      http://www.apache.org/licenses/LICENSE-2.0  
 *   
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
 * See the License for the specific language governing permissions and  
 * limitations under the License.  
 **/ 
package com.genwyse.docushare.rules;

import com.genwyse.docushare.rules.operator.GetProperty;

import com.dtrules.compiler.excel.util.Excel2XML;


/**
 * @author Paul Snow
 *
 */
public class Compile {

    /**
     * In Eclipse, System.getProperty("user.dir") returns the project
     * directory.  We add a slash to insure the path ends with a slash.
     */
    public static String path    = System.getProperty("user.dir")+"/test/rules/";
    
    /**
     * Routine to compile decision tables.
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception { 
        try {
            //new TestOp();
        	new GetProperty ();
        	Excel2XML.compile(path,"DTRules.xml","TestDocuShare","test/rules/repository");
                       
        } catch ( Exception ex ) {
            System.out.println("Failed to convert the Excel files");
            ex.printStackTrace();
            throw ex;
        }
        
        
        
     }
}
