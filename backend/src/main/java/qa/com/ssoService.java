package qa.com;

/**
*
* @author 
* updated by avbee 270319
*/

import java.net.URI;

import javax.servlet.http.Cookie;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.databind.node.ObjectNode;

import qa.com.classDefinition.EINameTemplate;
import qa.com.classDefinition.LoginRes;
import qa.com.classDefinition.PCFUtil;
import qa.com.classDefinition.PatchUserRes;
import qa.com.classDefinition.ResponseCookie;
import qa.com.classDefinition.User;
import qa.com.classDefinition.UserRoleByScopes;
import qa.com.db.PostgreSql;
import qa.com.ssoException.CannotAcquireDataException;
import qa.com.ssoSubMethod;

@Service
@Component
public class ssoService {

	final String RESP_AUTHORIZED = "Authorized";
	final String RESP_UNAUTHORIZED = "Unauthorized";
	final String RESP_SRPIDFAILED = "get srp id failed";
	final String RESP_TOKEN_NOT_FOUND = "EIToken not found";
	final String RESP_TOKEN_EXPIRED = "EIToken expired";

	private String SSO_API_ENDPOINT = "https://portal-sso.ali.wise-paas.com.cn/v2.0";

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ssoService.class);

	public ObjectNode getSRPToken(String srpToken, String endPointUrl) throws Exception {

		LOGGER.info("Initiate getSRPToken.srpToken");

		String apiUrl = String.format("%s%s/%s", SSO_API_ENDPOINT, "/srps", endPointUrl);
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Auth-SRPToken", String.format("%s", srpToken));
		HttpEntity<ObjectNode> entity = new HttpEntity<ObjectNode>(headers);
		RestTemplate restTemplate = new RestTemplate();

		LOGGER.info("getSRPToken.srpToken success");

		return restTemplate.exchange(apiUrl, HttpMethod.GET, entity, ObjectNode.class).getBody();

	}

	public ObjectNode getParams() throws Exception {

		LOGGER.info("Initiate getSRPToken.srpToken");

		String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/params");
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<ObjectNode> entity = new HttpEntity<ObjectNode>(headers);
		RestTemplate restTemplate = new RestTemplate();

		LOGGER.info("getParams success");

		return restTemplate.exchange(apiUrl, HttpMethod.GET, entity, ObjectNode.class).getBody();

	}

	@Autowired
	private ssoSubMethod SSO;

	public ResponseCookie doLogin(ObjectNode content) throws Exception {

		LOGGER.info("Initiate doLogin");
		ResponseCookie res = new ResponseCookie();
		LoginRes loginRes = new LoginRes();

		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

		String EIToken = "";
		String userName = "";

		try {

			LOGGER.info("Initiate doLogin.getToken");
			ObjectNode responseNode = getToken(content);

			if (responseNode.has("error")) {

				LOGGER.info(" doLogin.getToken 'if' responseNode has error ");

				throw new CannotAcquireDataException(responseNode.get("error").toString());

			} else {

				LOGGER.info("initiate doLogin.getToken 'else' - responseNode has no error ");
				String cfApi = SSO.recvApplicationEvn("cf_api");
				String domain = cfApi.substring(cfApi.indexOf("api.") + 4);

				String accessToken = responseNode.get("accessToken").toString();
				String[] tokenSplit = accessToken.split("\\.");
				System.out.println("tokensplit" + tokenSplit);

				String decodeStr = new String(Base64.decodeBase64(tokenSplit[1]), "UTF-8");

				UserRoleByScopes roleAccess = SSO.getUserRoleByScopes(decodeStr);

				if (Boolean.logicalAnd(roleAccess.isLoginFlag(), true)) {
					EIToken = responseNode.get("accessToken").toString();
					loginRes.setAccessToken(EIToken);
					loginRes.setRefreshToken(responseNode.get("refreshToken").toString());
					loginRes.setTokenType(responseNode.get("tokenType").toString());

					LOGGER.info(domain);
					Cookie cookie1 = new Cookie("EIToken", EIToken);
					cookie1.setDomain(domain);
					cookie1.setMaxAge(60 * 60);
					cookie1.setPath("/");
					cookie1.setSecure(true);
					cookie1.setHttpOnly(true);

					Cookie cookie2 = new Cookie("EIName", userName);
					cookie2.setDomain(domain);
					cookie2.setMaxAge(60 * 60);
					cookie2.setPath("/");
					cookie2.setSecure(true);

					res.setStrJson(mapper.writeValueAsString(loginRes));
					res.setCookie1(cookie1);
					res.setCookie2(cookie2);

					loginRes.setErrorDescription(roleAccess.getDesc());
					res.setStrJson(mapper.writeValueAsString(loginRes));
					return res;
				}

				loginRes.setErrorDescription(roleAccess.getDesc());
				res.setStrJson(mapper.writeValueAsString(loginRes));

				return res;
			}

		} catch (Exception e) {
			// TODO: handle exception
			throw new CannotAcquireDataException(e.getMessage());

		}

	}

	public LoginRes doLoginByToken(String EIToken, String refreshToken) throws Exception {

		LOGGER.info("initiate doLoginByToken");

		LoginRes loginRes = new LoginRes();

		try {

			if (EIToken.isEmpty()) {

				LOGGER.info("doLoginByToken.EIToken is empty");

				throw new CannotAcquireDataException(RESP_TOKEN_NOT_FOUND);

			} else if (!SSO.doValidateToken(EIToken)) {

				LOGGER.info("doLoginByToken.doValidateToken is false - initiate logout");
				doLogout();

				throw new CannotAcquireDataException(RESP_TOKEN_EXPIRED);

			}

			ObjectMapper mapp = new ObjectMapper();
			ObjectNode json = mapp.createObjectNode();
			json.put("token", refreshToken);
			LOGGER.info("initiate doLoginByToken.refreshToken");
			refreshToken(json);
			LOGGER.info("doLoginByToken.refreshToken success");

			String[] tokenSplit = EIToken.split("\\.");
			String decodeStr = new String(Base64.decodeBase64(tokenSplit[1]), "UTF-8");

			UserRoleByScopes roleAccess = SSO.getUserRoleByScopes(decodeStr);

			if (Boolean.logicalAnd(roleAccess.isLoginFlag(), true)) {

				loginRes.setErrorDescription(roleAccess.getDesc());
				return loginRes;
			}
		} catch (Exception e) {
			// TODO: handle exception
			LOGGER.warn(e.toString());

			throw new CannotAcquireDataException(e.getMessage());

		}

		LOGGER.info("doLoginByToken success");

		return loginRes;
	}

	public String patchUser(String EIToken, ObjectNode conJson) throws Exception {

		String email;

		PatchUserRes patchUserRes = new PatchUserRes();
		try {

			email = conJson.get("email").textValue();

			/*
			 * If SSO.srpId contains null, method will throws error, therefore errorHandler
			 * for string null in SSO.srpID should be empty string -> \""
			 */
			if (SSO.srpId.equals("")) {
				if (!SSO.recvSrpIdAndSecret()) {
					patchUserRes.setErrorDescription(RESP_SRPIDFAILED);
					return patchUserRes.toString();
				}
			}
			String urlPatch = SSO.recvSSOUrl() + "/v2.0/users/" + email + "/scopes";
			ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

			ObjectNode body = mapper.createObjectNode();
			body.put("srpId", SSO.srpId);
			body.put("srpSecret", SSO.srpSecret);
			body.put("action", "append");
			body.putArray("scopes");
			body.put("scopes", "user");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
			headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + EIToken);
			HttpEntity<String> he = new HttpEntity<String>(body.toString(), headers);
			RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
			ObjectNode resJson = restTemplate.patchForObject(urlPatch, he, ObjectNode.class);

			if (resJson.has("error")) {
				if (resJson.get("message").get("exist").asInt() != -1
						&& resJson.get("message").get("scope").asInt() != -1) {

				} else {
					patchUserRes.setErrorDescription(resJson.get("error").textValue());
					return patchUserRes.toString();
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			patchUserRes.setErrorDescription(e.getMessage());
			return patchUserRes.toString();
		}
		PCFUtil.GetPostgresEnv();
		/*
		 * PostgreSql library is not present on this code. No PostgreSql library on old
		 * version of SsoService also.
		 */

		PostgreSql sql = new PostgreSql();
		sql.insertUser(PCFUtil.postgresUrl, PCFUtil.postgresUsername, PCFUtil.postgresPassword, email);
		return patchUserRes.toString();

	}

	public String deleteUser(String EIToken, ObjectNode conJson) throws Exception {

		String email;

		PatchUserRes patchUserRes = new PatchUserRes();
		try {

			email = conJson.get("email").textValue();
			if (SSO.srpId.equals("")) {
				if (!SSO.recvSrpIdAndSecret()) {
					patchUserRes.setErrorDescription(RESP_SRPIDFAILED);
					return patchUserRes.toString();
				}
			}
			String urlPatch = SSO.recvSSOUrl() + "/v2.0/users/" + email + "/scopes";
			ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

			ObjectNode body = mapper.createObjectNode();
			body.put("srpId", SSO.srpId);
			body.put("srpSecret", SSO.srpSecret);
			body.put("action", "append");
			body.putArray("scopes");
			body.put("scopes", "user");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
			headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + EIToken);
			HttpEntity<String> he = new HttpEntity<String>(body.toString(), headers);
			RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
			ObjectNode resJson = restTemplate.patchForObject(urlPatch, he, ObjectNode.class);

			if (resJson.has("error")) {

				patchUserRes.setErrorDescription(resJson.get("error").textValue());
				return patchUserRes.toString();

			}

		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			patchUserRes.setErrorDescription(e.getMessage());
			return patchUserRes.toString();
		}
		PCFUtil.GetPostgresEnv();

		PostgreSql sql = new PostgreSql();
		sql.deleteUser(PCFUtil.postgresUrl, PCFUtil.postgresUsername, PCFUtil.postgresPassword, email);
		return patchUserRes.toString();

	}

	private ClientHttpRequestFactory clientHttpRequestFactory() {
		// is it ok to create a new instance of HttpComponentsClientHttpRequestFactory
		// everytime?
		int timeout = 5;
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setReadTimeout(timeout); // setting timeout as read timeout
		factory.setConnectTimeout(timeout); // setting timeout as connect timeout

		return factory;
	}

	public EINameTemplate doGetUserName(String EIName) {
		EINameTemplate resEIName = new EINameTemplate();

		try {
			resEIName.setUserName(EIName);
			return resEIName;

		} catch (Exception e) {
			// TODO: handle exception
			resEIName.setErrorDescription(e.getMessage());
			return resEIName;
		}

	}

	public String doGetUserList() {
		User allUsers = new User();

		try {
			PCFUtil.GetPostgresEnv();
			PostgreSql sql = new PostgreSql();
			allUsers = sql.getUserList(PCFUtil.postgresUrl, PCFUtil.postgresUsername, PCFUtil.postgresPassword);
		} catch (Exception e) {
			// TODO: handle exception
			allUsers.setErrorDescription(e.getMessage());

		}
		return allUsers.getErrorDescription();
	}

	public ResponseCookie doLogout() throws CannotAcquireDataException {
		ResponseCookie res = new ResponseCookie();

		try {
			String cfApi = SSO.recvApplicationEvn("cf_api");
			String domain = cfApi.substring(cfApi.indexOf("api.") + 4);
			Cookie cookie1 = new Cookie("EIToken", "");
			cookie1.setDomain(domain);
			cookie1.setMaxAge(0);
			cookie1.setPath("/");
			cookie1.setSecure(true);
			cookie1.setHttpOnly(true);

			Cookie cookie2 = new Cookie("EIName", "");
			cookie2.setDomain(domain);
			cookie2.setMaxAge(0);
			cookie2.setPath("/");
			cookie2.setSecure(true);

		} catch (Exception e) {
			// TODO: handle exception
			throw new CannotAcquireDataException(e.getMessage());
		}
		return res;

	}

	public ObjectNode validateToken(ObjectNode json) throws Exception {

		String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/tokenvalidation");
		RestTemplate restTemplate = new RestTemplate();

		RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(json, HttpMethod.POST, URI.create(apiUrl));

		return restTemplate.exchange(req, ObjectNode.class).getBody();

	}

	public ObjectNode getToken(ObjectNode auth) {
		String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/auth/native");
		RestTemplate restTemplate = new RestTemplate();
		RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(auth, HttpMethod.POST, URI.create(apiUrl));
		return restTemplate.exchange(req, ObjectNode.class).getBody();
	}

	public ObjectNode refreshToken(ObjectNode tokenPayload) throws Exception {

		LOGGER.info("initiate refreshToken");
		String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/token");
		RestTemplate restTemplate = new RestTemplate();
		RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(tokenPayload, HttpMethod.POST,
				URI.create(apiUrl));
		return restTemplate.exchange(req, ObjectNode.class).getBody();

	}

	public ObjectNode getTokenUser(String accessToken) {
		String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/users/me");
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", String.format("Bearer %s", accessToken));
		RequestEntity<Void> req = new RequestEntity<Void>(headers, HttpMethod.GET, URI.create(apiUrl));
		return restTemplate.exchange(req, ObjectNode.class).getBody();
	}

	public ObjectNode addSrp(ObjectNode srpDetail, String srpToken) {
		String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/srps");
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Auth-SRPToken", String.format("%s", srpToken));
		RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(srpDetail, headers, HttpMethod.POST,
				URI.create(apiUrl));
		return restTemplate.exchange(req, ObjectNode.class).getBody();
	}

}
