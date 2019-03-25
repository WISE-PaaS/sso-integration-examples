package org.iii.sso;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	//
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

//	@RequestMapping(method = RequestMethod.POST, value = "/login/token")
//	public ResponseEntity<String> login(@RequestParam(value = "content", required = true) String content,
//			@RequestBody HttpServletResponse response) throws Exception {
//		ObjectNode json = new ObjectMapper().readValue(content, ObjectNode.class);
//		System.out.println(json);
//		String strJson = ssoService.login(json).strJson;
//		Cookie[] cookie = ssoService.login(json).cookie;
//		if (cookie != null) {
//			response.addCookie(cookie[0]);
//			response.addCookie(cookie[1]);
//
//			/*
//			 * EIToken=eyJhbGciOiJIUzUxMiJ9.
//			 * eyJpc3MiOiJhbGktd2lzZXBhYXMiLCJpYXQiOjE1NTMyMTkyNjUsImV4cCI6MTU1MzIyMjg2NSwidXNlcklkIjoiNjYwZjZlNTItZTc3OS00ZWUwLTk3ZmUtNTc2MzY4OGYwZTM4IiwidWFhSWQiOiIxNzE4OTI1NS0wZGIwLTQxYjYtOGExNi1lNWQwYzg4MmJjYzQiLCJjcmVhdGlvblRpbWUiOjE1NTI0NzE1NzQwMDAsImxhc3RNb2RpZmllZFRpbWUiOjE1NTMxNTQ3NTAwMDAsInVzZXJuYW1lIjoiRnJhbnNpc2N1cy5CaW1vQGFkdmFudGVjaC5jb20udHciLCJmaXJzdE5hbWUiOiJGcmFuc2lzY3VzIiwibGFzdE5hbWUiOiJCaW1vIiwiY291bnRyeSI6IlRXIiwicm9sZSI6InRlbmFudCIsImdyb3VwcyI6WyI2NGViZmE0Yi00NTRlLTRkYjMtOGY5MS03ZGIzZTE2OWE5NjIiLCJGcmFuc2lzY3VzLkJpbW9AYWR2YW50ZWNoLmNvbS50dyJdLCJjZlNjb3BlcyI6W3siZ3VpZCI6IjY0ZWJmYTRiLTQ1NGUtNGRiMy04ZjkxLTdkYjNlMTY5YTk2MiIsInNzb19yb2xlIjoidGVuYW50Iiwic3BhY2VzIjpbIioiXX1dLCJzY29wZXMiOltdLCJzdGF0dXMiOiJhY3RpdmUiLCJvcmlnaW4iOiJTU08iLCJvdmVyUGFkZGluZyI6ZmFsc2UsInN5c3RlbSI6ZmFsc2UsInJlZnJlc2hUb2tlbiI6ImZhYzg4MjgzLTVlZDAtNDQ0Ni1hZGQyLWM1NDk1Yjg4OWU3ZiJ9
//			 * .
//			 * KdY47370ONq8D1dps5XgNAZTZqzKakzT6n5dgdMwtnNGdyzyjPlCRIte4gh2TbcHcDKfPVdz_BTghY76Z3gIng;
//			 * EIName=Fransiscus
//			 */
//		}
//		return new ResponseEntity<String>(strJson, HttpStatus.OK);
//
//	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/login/token")
	public ResponseEntity<String> login(
			@RequestBody loginInput linput, @RequestBody HttpServletResponse response) throws Exception {
		ObjectNode json = new ObjectMapper().readValue(linput, ObjectNode.class);
		System.out.println(linput);
		
		return new ResponseEntity<String>(linput, HttpStatus.OK);

	}

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
