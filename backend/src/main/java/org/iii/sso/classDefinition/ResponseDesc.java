/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iii.sso.classDefinition;

/**
 *
 * @author li.jie
 */
public class ResponseDesc {
    String ErrorDescription;
    
    public ResponseDesc(){
        ErrorDescription = "OK";
    }

    public String getErrorDescription() {
        return ErrorDescription;
    }

    public void setErrorDescription(String ErrorDescription) {
        this.ErrorDescription = ErrorDescription;
    }
    
}
