package qa.com.classDefinition;

import javax.servlet.http.Cookie;

public class ResponseCookie {
	private String strJson;
	private Cookie cookie1;
	private Cookie cookie2;

	public String getStrJson() {
		return strJson;
	}

	public void setStrJson(String string) {
		this.strJson = string;
	}

	public Cookie getCookie1() {
		return cookie1;
	}

	public Cookie getCookie2() {
		return cookie2;
	}

	public void setCookie1(Cookie cookie1) {
		this.cookie1 = cookie1;
	}

	public void setCookie2(Cookie cookie2) {
		this.cookie2 = cookie2;
	}
}
