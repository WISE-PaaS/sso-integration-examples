
/*  * To change this license header, choose License Headers in Project Properties.  * To change this template file, choose Tools | Templates  * and open the template in the editor.  */ 

package org.iii.sso.classDefinition;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

/** * * @author li.jie */
//@XmlRootElement

public class PatchUserRes implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5829383334657185537L;
	String ErrorDescription;
	String UserId;
	String UserName;
	String Role;

	public PatchUserRes() {
		ErrorDescription = "OK";
	}

	public String getErrorDescription() {
		return ErrorDescription;
	}

	public void setErrorDescription(String ErrorDescription) {
		this.ErrorDescription = ErrorDescription;
	}

	public String getUserId() {
		return UserId;
	}

	public void setUserId(String UserId) {
		this.UserId = UserId;
	}

	public String getUserName() {
		return UserName;
	}

	public void setUserName(String UserName) {
		this.UserName = UserName;
	}

	public String getRole() {
		return Role;
	}

	public void setRole(String Role) {
		this.Role = Role;
	}
}
