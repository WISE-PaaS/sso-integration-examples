package org.iii.sso.classDefinition;

public class EINameTemplate {
    
    String ErrorDescription;	
    String UserName;

    public EINameTemplate() {
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