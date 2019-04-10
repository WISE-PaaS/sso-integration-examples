package qa.com.classDefinition;

public class UserRoleByScopes {

	public String getUserRole() {
		return userRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}

	public String getScopeRole() {
		return scopeRole;
	}

	public void setScopeRole(String scopeRole) {
		this.scopeRole = scopeRole;
	}

	public boolean isRoleFlag() {
		return roleFlag;
	}

	public void setRoleFlag(boolean roleFlag) {
		this.roleFlag = roleFlag;
	}

	public boolean isStatusFlag() {
		return statusFlag;
	}

	public void setStatusFlag(boolean statusFlag) {
		this.statusFlag = statusFlag;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}

	public void setLoginFlag(boolean loginFlag) {
		this.loginFlag = loginFlag;
	}

	public boolean isLoginFlag() {
		return loginFlag;
	}

	private boolean loginFlag;
	private String userRole;
	private String scopeRole;
	private String desc;

	private boolean roleFlag;
	private boolean statusFlag;

}
