
/**
*
* @author 
* updated by avbee 270319
*/

package qa.com;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
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

import qa.com.classDefinition.EINameTemplate;
import qa.com.classDefinition.ResponseCookie;
import qa.com.classDefinition.loginInput;
import qa.com.db.PostgreSql;

@RestController
public class ssoController {

	@Autowired
	private ssoService ssoService;
	@Autowired
	private ObjectMapper objMapper;
	@Autowired
	private PostgreSql postgres;

	/* GET METHODS */

	@RequestMapping(method = RequestMethod.GET, value = "/users/me")
	public ResponseEntity<ObjectNode> getTokenUser(@RequestParam(value = "username", required = true) String username,
			@RequestParam(value = "password", required = true) String password) throws Exception {

		ObjectNode tokenPackage = ssoService
				.getToken(objMapper.createObjectNode().put("username", username).put("password", password));
		ObjectNode tokenPayload = objMapper.createObjectNode().put("token", tokenPackage.get("refreshToken").asText());
		tokenPackage = ssoService.refreshToken(tokenPayload);

		return new ResponseEntity<ObjectNode>(ssoService.getTokenUser(tokenPackage.get("accessToken").asText()),
				HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/user")
	public ResponseEntity<EINameTemplate> getUsername(@CookieValue(value = "EIName", required = true) String EIName)
			throws Exception {

		return new ResponseEntity<EINameTemplate>(ssoService.doGetUserName(EIName), HttpStatus.ACCEPTED);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/params")
	public ResponseEntity<ObjectNode> getParams() throws Exception {

		return new ResponseEntity<ObjectNode>(ssoService.getParams(), HttpStatus.ACCEPTED);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/test")
	public ResponseEntity<String> noupdate(HttpServletRequest req, HttpServletResponse resp)
			throws ClassNotFoundException, SQLException, IOException {

//		postgres.getConn("jdbc:mysql://localhost/test", "username", "password");

		return new ResponseEntity<String>("OK", HttpStatus.ACCEPTED);
	}

	/* POST METHODS */

	@RequestMapping(method = RequestMethod.POST, value = "/login")
	public ResponseEntity<ObjectNode> login(@RequestBody ObjectNode Response, HttpServletResponse response)
			throws Exception {

		ResponseCookie getLoginToken = ssoService.doLogin(Response);
		if (getLoginToken.getCookie1() != null) {
			response.addCookie(getLoginToken.getCookie1());
			response.addCookie(getLoginToken.getCookie2());
		}
		ObjectNode json = new ObjectMapper().readValue(getLoginToken.getStrJson(), ObjectNode.class);

		return new ResponseEntity<ObjectNode>(json, HttpStatus.OK);

	}

	@RequestMapping(method = RequestMethod.POST, value = "/add")
	public ResponseEntity<ObjectNode> addUser(@RequestBody ObjectNode Response,
			@CookieValue(value = "EIToken", required = true) String EIToken) throws Exception {

		ObjectNode json = new ObjectMapper().readValue(ssoService.patchUser(EIToken, Response), ObjectNode.class);

		return new ResponseEntity<ObjectNode>(json, HttpStatus.OK);

	}

	@RequestMapping(method = RequestMethod.POST, value = "/delete")
	public ResponseEntity<ObjectNode> deleteUser(@RequestBody ObjectNode Response,
			@CookieValue(value = "EIToken", required = true) String EIToken) throws Exception {

		ObjectNode json = new ObjectMapper().readValue(ssoService.deleteUser(EIToken, Response), ObjectNode.class);

		return new ResponseEntity<ObjectNode>(json, HttpStatus.OK);

	}

	@RequestMapping(method = RequestMethod.POST, value = "/list")
	public ResponseEntity<ObjectNode> userList() throws Exception {

		ObjectNode json = new ObjectMapper().readValue(ssoService.doGetUserList(), ObjectNode.class);

		return new ResponseEntity<ObjectNode>(json, HttpStatus.OK);

	}

	@RequestMapping(method = RequestMethod.POST, value = "/login/token")
	public ResponseEntity<ObjectNode> loginByToken(@CookieValue(value = "EIToken", required = true) String EIToken,
			@CookieValue(value = "refreshToken", required = true) String refreshToken) throws Exception {

		ObjectNode res = new ObjectMapper().convertValue(ssoService.doLoginByToken(EIToken, refreshToken),
				ObjectNode.class);

		return new ResponseEntity<ObjectNode>(res, HttpStatus.ACCEPTED);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/logout")
	public ResponseEntity<ResponseCookie> doLogout() throws Exception {

		return new ResponseEntity<ResponseCookie>(ssoService.doLogout(), HttpStatus.ACCEPTED);
	}

	/*
	 * Testing request
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/test")
	public ResponseEntity<loginInput> update(@RequestBody loginInput linput) throws IOException {

		if (linput != null) {
			linput.setUsername("aa");
		}

		System.out.println(linput);
		System.out.println(linput.toString());

		return new ResponseEntity<loginInput>(linput, HttpStatus.ACCEPTED);
	}

}