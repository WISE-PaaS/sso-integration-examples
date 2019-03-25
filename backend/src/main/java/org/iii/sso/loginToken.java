package org.iii.sso;

import javax.servlet.http.Cookie;

public class loginToken {
	private String strJson;
	private Cookie[] cookie;
	public String getStrJson() {
		return strJson;
	}
	public void setStrJson(String strJson) {
		this.strJson = strJson;
	}
	public Cookie[] getCookie() {
		return cookie;
	}
	public void setCookie(Cookie[] cookie) {
		this.cookie = cookie;
	}
}
