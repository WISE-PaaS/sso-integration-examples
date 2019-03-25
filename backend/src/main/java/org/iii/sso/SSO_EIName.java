package org.iii.sso;

public class SSO_EIName {
    
    String ErrorDescription;	
    String UserName;

    public SSO_EIName() {
        ErrorDescription = "OK";
    }

    public String getErrorDescription() {
        return ErrorDescription;
    }

    public void setErrorDescription(String ErrorDescription) {
        this.ErrorDescription = ErrorDescription;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String UserName) {
        this.UserName = UserName;
    }
    
    
}