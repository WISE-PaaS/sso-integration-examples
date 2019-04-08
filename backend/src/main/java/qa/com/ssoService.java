package qa.com;

/**
*
* @author 
* updated by avbee 270319
*/

import java.net.URI;

import java.util.ArrayList;
import java.util.List;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import qa.com.classDefinition.EINameTemplate;
import qa.com.classDefinition.LoginRes;
import qa.com.classDefinition.PCFUtil;
import qa.com.classDefinition.PatchUserRes;
import qa.com.classDefinition.ResponseCookie;
import qa.com.classDefinition.User;
import qa.com.db.PostgreSql;
import qa.com.ssoSubMethod;

@Service
@Component
public class ssoService {

	final String RESP_UNAUTHORIZED = "unauthorized";
	final String RESP_SRPIDFAILED = "get srp id failed";

	/*
	 * VARIABLE FOR ROLES. BE AWARE THAT ALL CHARACTERS ARE INTENTIONALLY CONVERT TO
	 * LOWERCASE !
	 */

	final String role_tenant = "tenant";
	final String role_admin = "admin";
	final String role_srpUser = "srpuser";
	final String role_developer = "developer";

	public static final String SSO_API_ENDPOINT = "https://portal-sso.ali.wise-paas.com.cn/v2.0";

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ssoService.class);

	public ObjectNode getSRPToken(String srpToken, String endPointUrl) throws Exception {

		try {
			LOGGER.info("Initiate getSRPToken.srpToken");

			String apiUrl = String.format("%s%s/%s", SSO_API_ENDPOINT, "/srps", endPointUrl);
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-Auth-SRPToken", String.format("%s", srpToken));
			HttpEntity<ObjectNode> entity = new HttpEntity<ObjectNode>(headers);
			RestTemplate restTemplate = new RestTemplate();

			LOGGER.info("getSRPToken.srpToken success");

			return restTemplate.exchange(apiUrl, HttpMethod.GET, entity, ObjectNode.class).getBody();

		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			return null;
		}
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

				loginRes.setErrorDescription(responseNode.get("error").toString());

				res.setStrJson(mapper.writeValueAsString(loginRes));
				mapper.readValue(res.getStrJson(), ObjectNode.class);
				return res;

			} else {
				LOGGER.info(" initiate doLogin.getToken 'else' - responseNode has no error ");

				String accessToken = responseNode.get("accessToken").toString();
				String[] tokenSplit = accessToken.split("\\.");
				String decodeStr = new String(Base64.decodeBase64(tokenSplit[1]), "UTF-8");

				ObjectNode tokenInfo = new ObjectMapper().readValue(decodeStr, ObjectNode.class);

				final JsonNode arrNode = new ObjectMapper().readTree(decodeStr);
				String role = arrNode.get("role").asText();
				userName = arrNode.get("firstName").asText();

				System.out.println(tokenInfo);

				LOGGER.info("doLogin.getToken 'else' success ");
				/* verification of OrgID for tenant */
				if (role.equalsIgnoreCase(role_tenant)) {

					LOGGER.info("doLogin.role 'if' has tenant ");

//					System.out.println("Verification of OrgID for tenant");
					ArrayNode userOrgIdNode = tokenInfo.withArray("groups");

					String orgId = System.getenv("org_id");

					List<String> userOrgId = new ArrayList<>();
					for (JsonNode node : userOrgIdNode) {
						userOrgId.add(node.asText());
					}
//					System.out.println("userOrgId");

					if (!userOrgId.contains(orgId)) {

						LOGGER.info("doLogin.userOrgId 'if' is false ");

						loginRes.setErrorDescription(RESP_UNAUTHORIZED);
						res.setStrJson(mapper.writeValueAsString(loginRes));
						return res;
					}

					LOGGER.info("doLogin.userOrgId 'if' contains orgId ");

				} else {

					LOGGER.info("doLogin.role 'if' is not tenant");

					/* verification of srpId for other users */
					if (SSO.srpId == null) {

						LOGGER.info("doLogin.SSO.srpId 'else' is null");

						if (!SSO.recvSrpIdAndSecret()) {

							LOGGER.info("doLogin.SSO.recvSrpIdAndSecret 'if' is false");

							loginRes.setErrorDescription(RESP_SRPIDFAILED);
							res.setStrJson(mapper.writeValueAsString(loginRes));

							return res;
						}
					}

					LOGGER.info("doLogin.SSO.srpId 'else' is not null");

					boolean flag = false;

					ArrayNode scopes = tokenInfo.withArray("scopes");
					for (int i = 0; i < scopes.size(); i++) {

						LOGGER.info("doLogin.scopes loop attempt  " + i + "/" + scopes.size());

						String strScope = scopes.get(i).toString();
						String[] eachScope = strScope.split("\\.");
						if (eachScope[0].equalsIgnoreCase(SSO.srpId)) {

							LOGGER.info("doLogin.scopes flag is true");

							flag = true;
						}
					}
					if (flag == false) {

						LOGGER.info("doLogin.scopes flag is false");

						loginRes.setErrorDescription(RESP_UNAUTHORIZED);
						res.setStrJson(mapper.writeValueAsString(loginRes));

						return res;
					}
				}
			}
			EIToken = responseNode.get("accessToken").toString();
			loginRes.setAccessToken(EIToken);
			loginRes.setRefreshToken(responseNode.get("refreshToken").toString());
			loginRes.setTokenType(responseNode.get("tokenType").toString());

			LOGGER.info("doLogin.getToken success");
		} catch (Exception e) {
			// TODO: handle exception
			loginRes.setErrorDescription(e.getMessage());

			res.setStrJson(mapper.writeValueAsString(loginRes));
			return res;

		}

		String cfApi = SSO.recvApplicationEvn("cf_api");

		/*
		 * DEBUG
		 */
//		String cfApi = "https://api.wise-paas.com";

		String domain = cfApi.substring(cfApi.indexOf("api.") + 4);

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

		LOGGER.info("doLogin success");

		return res;

	}

	public LoginRes doLoginByToken(String EIToken) throws Exception {
		LOGGER.info("initiate doLoginByToken");

		LoginRes res = new LoginRes();

		try {

			if (EIToken.isEmpty()) {

				LOGGER.info("doLoginByToken.EIToken is empty");

				res.setErrorDescription("EITokennotfound");
				return res;

			} else if (!SSO.doValidateToken(EIToken)) {

				LOGGER.info("doLoginByToken.doValidateToken is false");

				ObjectMapper mapp = new ObjectMapper();
				ObjectNode json = mapp.createObjectNode();
				json.put("token", EIToken);

				LOGGER.info("initiate doLoginByToken.refreshToken");
				refreshToken(json);
				LOGGER.info("doLoginByToken.refreshToken success");

			}

			String[] tokenSplit = EIToken.split("\\.");
			String decodeStr = new String(Base64.decodeBase64(tokenSplit[1]), "UTF-8");
			JsonNode tokenInfo = new ObjectMapper().readValue(decodeStr, JsonNode.class);

			String role = tokenInfo.get("role").asText();

			/* verification of OrgID for tenant */
			if (role.equalsIgnoreCase(role_tenant)) {

				LOGGER.info("doLoginByToken.role 'if' has tenant ");

				String userOrgId = tokenInfo.withArray("groups").get(0).textValue();
				String orgId = System.getenv("org_id");

				/*
				 * DEBUG
				 */
//				String orgId = "64ebfa4b-454e-4db3-8f91-7db3e169a962";

				if (!userOrgId.equalsIgnoreCase(orgId)) {

					LOGGER.info("doLoginByToken.userOrgId 'if' is false ");

					res.setErrorDescription(RESP_UNAUTHORIZED);
					return res;
				}

			} else {
				/* verification of srpId for other users */
				if (SSO.srpId == null) {

					LOGGER.info("doLoginByToken.SSO.srpId  'if' is null ");

					if (!SSO.recvSrpIdAndSecret()) {

						LOGGER.info("doLoginByToken.SSO.recvSrpIdAndSecret  'if' is false ");

						res.setErrorDescription(RESP_SRPIDFAILED);
						return res;
					}
				}

				boolean flag = false;

				JsonNode scopes = tokenInfo.withArray("scopes");

				for (int i = 0; i < scopes.size(); i++) {

					LOGGER.info("doLoginByToken.scopes loop attempt  " + i + "/" + scopes.size());

					String strScope = scopes.get(i).toString();
					String[] eachScope = strScope.split("\\.");
					if (eachScope[0].equalsIgnoreCase(SSO.srpId)) {

						LOGGER.info("doLoginByToken.scopes flag is true");

						flag = true;
					}
				}
				if (flag == false) {

					LOGGER.info("doLoginByToken.scopes flag is false");

					res.setErrorDescription(RESP_UNAUTHORIZED);
					return res;
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
			LOGGER.warn(e.toString());
			res.setErrorDescription(e.getMessage());
			return res;

		}

		LOGGER.info("doLoginByToken success");

		return res;
	}

	public String patchUser(String EIToken, ObjectNode conJson) throws Exception {

		String email;

		PatchUserRes patchUserRes = new PatchUserRes();
		try {

			email = conJson.get("email").textValue();
			if (SSO.srpId == null) {
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
			if (SSO.srpId == null) {
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
		/*
		 * PostgreSql library is not present on this code. No PostgreSql library on old
		 * version of SsoService also.
		 */

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

	public ResponseCookie doLogout() {
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
			return res;
		}
		return res;

	}

	public ObjectNode validateToken(ObjectNode json) throws Exception {
		try {

			String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/tokenvalidation");
			RestTemplate restTemplate = new RestTemplate();

			RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(json, HttpMethod.POST, URI.create(apiUrl));

			return restTemplate.exchange(req, ObjectNode.class).getBody();
		} catch (Exception e) {

			// TODO: handle exception
			return null;
		}
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
