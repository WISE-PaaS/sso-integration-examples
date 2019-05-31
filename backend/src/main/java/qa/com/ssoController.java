
/**
*
* @author 
* updated by avbee 300519
*/

package qa.com;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import qa.com.classDefinition.EINameTemplate;
import qa.com.classDefinition.ResponseCookie;

@RestController
public class ssoController {

	@Autowired
	private ssoService ssoService;

	/* GET METHODS */
	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "/users/me")
	public ResponseEntity<ObjectNode> getTokenUser(@CookieValue(value = "EIToken", required = true) String EIToken)
			throws Exception {

		return new ResponseEntity<ObjectNode>(ssoService.getTokenUser(EIToken), HttpStatus.OK);
	}

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "/user")
	public ResponseEntity<EINameTemplate> getUsername(@CookieValue(value = "EIName", required = true) String EIName)
			throws Exception {

		return new ResponseEntity<EINameTemplate>(ssoService.doGetUserName(EIName), HttpStatus.ACCEPTED);
	}

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "/params")
	public ResponseEntity<ObjectNode> getParams() throws Exception {

		return new ResponseEntity<ObjectNode>(ssoService.getParams(), HttpStatus.ACCEPTED);
	}

	/* POST METHODS */
	@CrossOrigin
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

	@CrossOrigin
	@RequestMapping(method = RequestMethod.POST, value = "/login/token")
	public ResponseEntity<ObjectNode> loginByToken(@CookieValue(value = "EIToken", required = true) String EIToken,
			@CookieValue(value = "refreshToken", required = true) String refreshToken) throws Exception {

		ObjectNode res = new ObjectMapper().convertValue(ssoService.doLoginByToken(EIToken, refreshToken),
				ObjectNode.class);

		return new ResponseEntity<ObjectNode>(res, HttpStatus.ACCEPTED);
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

	@CrossOrigin
	@RequestMapping(method = RequestMethod.POST, value = "/logout")
	public ResponseEntity<ResponseCookie> doLogout() throws Exception {

		return new ResponseEntity<ResponseCookie>(ssoService.doLogout(), HttpStatus.ACCEPTED);
	}

}