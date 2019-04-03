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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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

@AutoConfigurationPackage
@Service

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

	public ObjectNode getSRPToken(String srpToken, String endPointUrl) throws Exception {

		try {

			String apiUrl = String.format("%s%s/%s", SSO_API_ENDPOINT, "/srps", endPointUrl);
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-Auth-SRPToken", String.format("%s", srpToken));
			HttpEntity<ObjectNode> entity = new HttpEntity<ObjectNode>(headers);
			RestTemplate restTemplate = new RestTemplate();
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

		ResponseCookie res = new ResponseCookie();
		LoginRes loginRes = new LoginRes();

		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

		String EIToken = "";
		String userName = "";

		try {

			ObjectNode responseNode = getToken(content);

			if (responseNode.has("error")) {
				loginRes.setErrorDescription(responseNode.get("error").toString());

				res.setStrJson(mapper.writeValueAsString(loginRes));
				mapper.readValue(res.getStrJson(), ObjectNode.class);
				return res;

			} else {
				String accessToken = responseNode.get("accessToken").toString();
				String[] tokenSplit = accessToken.split("\\.");
				String decodeStr = new String(Base64.decodeBase64(tokenSplit[1]), "UTF-8");

				ObjectNode tokenInfo = new ObjectMapper().readValue(decodeStr, ObjectNode.class);

				final JsonNode arrNode = new ObjectMapper().readTree(decodeStr);
				String role = arrNode.get("role").asText();
				userName = arrNode.get("firstName").asText();

				if (role.equalsIgnoreCase(role_tenant)) {
					ArrayNode userOrgIdNode = tokenInfo.withArray("groups");

					String orgId = System.getenv("org_id");

					List<String> userOrgId = new ArrayList<>();
					for (JsonNode node : userOrgIdNode) {
						userOrgId.add(node.asText());
					}

					if (!userOrgId.contains(orgId)) {

						loginRes.setErrorDescription(RESP_UNAUTHORIZED);
						res.setStrJson(mapper.writeValueAsString(loginRes));
						return res;
					}

				} else {
					if (SSO.srpId == null) {

						if (!SSO.recvSrpIdAndSecret()) {

							loginRes.setErrorDescription(RESP_SRPIDFAILED);

							res.setStrJson(mapper.writeValueAsString(loginRes));

							return res;
						}
					}

					boolean flag = false;

					ArrayNode scopes = tokenInfo.withArray("scopes");
					for (int i = 0; i < scopes.size(); i++) {

						String strScope = scopes.get(i).toString();
						String[] eachScope = strScope.split("\\.");
						if (eachScope[0].equalsIgnoreCase(SSO.srpId)) {
							flag = true;
						}
					}
					if (flag == false) {
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

		} catch (Exception e) {
			// TODO: handle exception
			loginRes.setErrorDescription(e.getMessage());

			res.setStrJson(mapper.writeValueAsString(loginRes));
			return res;

		}

//		String cfApi = SSO.recvApplicationEvn("cf_api");

		/*
		 * DEBUG
		 */
		String cfApi = "https://api.wise-paas.com";

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
		return res;

	}

	public LoginRes doLoginByToken(String EIToken) throws Exception {
		LoginRes res = new LoginRes();
//		SSO.doValidateToken(EIToken);

		try {

			if (EIToken.isEmpty()) {
				res.setErrorDescription("EITokennotfound");
				return res;

			} else if (!SSO.doValidateToken(EIToken)) {
				ObjectMapper mapp = new ObjectMapper();
				ObjectNode json = mapp.createObjectNode();
				json.put("token", EIToken);
				refreshToken(json);

			}
			String[] tokenSplit = EIToken.split("\\.");
			String decodeStr = new String(Base64.decodeBase64(tokenSplit[1]), "UTF-8");
			JsonNode tokenInfo = new ObjectMapper().readValue(decodeStr, JsonNode.class);

			String role = tokenInfo.get("role").asText();

			if (role.equalsIgnoreCase(role_tenant)) {
				String userOrgId = tokenInfo.withArray("groups").get(0).textValue();
				String orgId = System.getenv("org_id");

				/*
				 * DEBUG
				 */
//				String orgId = "64ebfa4b-454e-4db3-8f91-7db3e169a962";

				if (!userOrgId.equalsIgnoreCase(orgId)) {

					res.setErrorDescription(RESP_UNAUTHORIZED);
					return res;
				}

			} else {
				if (SSO.srpId == null) {
					if (!SSO.recvSrpIdAndSecret()) {
						res.setErrorDescription(RESP_SRPIDFAILED);
						return res;
					}
				}

				boolean flag = false;

				JsonNode scopes = tokenInfo.withArray("scopes");

				for (int i = 0; i < scopes.size(); i++) {

					String strScope = scopes.get(i).toString();
					String[] eachScope = strScope.split("\\.");
					if (eachScope[0].equalsIgnoreCase(SSO.srpId)) {
						flag = true;
					}
				}
				if (flag == false) {
					res.setErrorDescription(RESP_UNAUTHORIZED);
					return res;
				}
			}

		} catch (Exception e) {
			// TODO: handle exception

			res.setErrorDescription(e.getMessage());
			return res;

		}

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

	public ObjectNode refreshToken(ObjectNode tokenPayload) {
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
