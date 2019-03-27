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

public  class SSO_LoginRes {

    String tokenType;
    String accessToken; 
    String refreshToken;
    String ErrorDescription;	
    
    SSO_LoginRes() {   
        ErrorDescription = "OK";
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
       
    }

    

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getErrorDescription() {
        return ErrorDescription;
    }

    public void setErrorDescription(String ErrorDescription) {
        this.ErrorDescription = ErrorDescription;
    }
    
}
