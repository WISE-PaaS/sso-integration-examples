/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iii.sso;

/**
 *
 * @author li.jie
 */
public class SSO_Response {
    String ErrorDescription;
    
    public SSO_Response(){
        ErrorDescription = "OK";
    }

    public String getErrorDescription() {
        return ErrorDescription;
    }

    public void setErrorDescription(String ErrorDescription) {
        this.ErrorDescription = ErrorDescription;
    }
    
}
