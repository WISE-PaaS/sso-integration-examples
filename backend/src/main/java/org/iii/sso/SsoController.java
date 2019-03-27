package org.iii.sso;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class SsoController {

	@Autowired
	private SsoService ssoService;

	@Autowired
	private ObjectMapper objMapper;

	@RequestMapping(method = RequestMethod.GET, value = "/users/me")
	public ResponseEntity<ObjectNode> getTokenUser(@RequestParam(value = "username", required = true) String username,
			@RequestParam(value = "password", required = true) String password) {
		ObjectNode auth = objMapper.createObjectNode().put("username", username).put("password", password);
		ObjectNode tokenPackage = SsoService.getToken(auth);
		ObjectNode tokenPayload = objMapper.createObjectNode().put("token", tokenPackage.get("refreshToken").asText());
		tokenPackage = ssoService.refreshToken(tokenPayload);
		return new ResponseEntity<ObjectNode>(ssoService.getTokenUser(tokenPackage.get("accessToken").asText()),
				HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/login")
	public ResponseEntity<ObjectNode> login(@RequestBody ObjectNode Response, HttpServletResponse response)
			throws Exception {

		Response getLoginToken = ssoService.doLogin(Response);

		response.addCookie(getLoginToken.getCookie1());
		response.addCookie(getLoginToken.getCookie2());

		ObjectNode json = new ObjectMapper().readValue(getLoginToken.getStrJson(), ObjectNode.class);
		return new ResponseEntity<ObjectNode>(json, HttpStatus.OK);

	}

	@RequestMapping(method = RequestMethod.POST, value = "/login/token")
	public ResponseEntity<String> loginByToken(@CookieValue(value = "EIToken", required = true) String EIToken)
			throws Exception {

		return new ResponseEntity<String>(ssoService.doLoginByToken(EIToken), HttpStatus.ACCEPTED);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/user")
	public ResponseEntity<SSO_EIName> getUsername(@CookieValue(value = "EIName", required = true) String EIName)
			throws Exception {

		return new ResponseEntity<SSO_EIName>(ssoService.doGetUserName(EIName), HttpStatus.ACCEPTED);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/logout")
	public ResponseEntity<Response> doLogout() throws Exception {
		return new ResponseEntity<Response>(ssoService.doLogout(), HttpStatus.ACCEPTED);
	}
	
	/*
	 * Testing request  
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/test")
	public ResponseEntity<loginInput> update(@RequestBody loginInput linput) {

		if (linput != null) {
			linput.setUsername("aa");
		}

		System.out.println(linput);
		System.out.println(linput.toString());

		return new ResponseEntity<loginInput>(linput, HttpStatus.ACCEPTED);
	}
}
