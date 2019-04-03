package qa.com.classDefinition;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.ArrayList;
import java.util.List;
/**
*
* @author li.jie
* updated by avbee 270319
*/

public class User {
    
    int TotalCount;
    String ErrorDescription;	
    public List<UserInfo> UserList;
    
    public User()
    {
        ErrorDescription = "OK";
        UserList = new ArrayList<UserInfo>();
    }
    public int getTotalCount() {
        return TotalCount;
    }
    public void setTotalCount(int TotalCount) {
        this.TotalCount = TotalCount;
    }
    public String getErrorDescription() {
        return ErrorDescription;
    }
    public void setErrorDescription(String ErrorDescription) {
        this.ErrorDescription = ErrorDescription;
    }
    public List<UserInfo> getUserList() {
        return UserList;
    }
    public void setUserList(List<UserInfo> UserList) {
        this.UserList = UserList;
    }
    
    
}