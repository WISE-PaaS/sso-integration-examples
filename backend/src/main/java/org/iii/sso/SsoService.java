package org.iii.sso;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.apache.tomcat.util.codec.binary.Base64;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import org.springframework.http.RequestEntity;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@AutoConfigurationPackage
@Service
public class SsoService {

	private String RESP_UNAUTHORIZED = "unauthorized";
	private String RESP_SRPIDFAILED = "get srp id failed";
	private String RESP_NULLDOMAIN = "NULL_DOMAIN";

	public static final String SSO_API_ENDPOINT = "https://portal-sso.ali.wise-paas.com.cn/v2.0";

	public static ObjectNode getSRPToken(String srpToken, String endPointUrl) {

		String apiUrl = String.format("%s%s/%s", SSO_API_ENDPOINT, "/srps", endPointUrl);
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Auth-SRPToken", String.format("%s", srpToken));
		HttpEntity<ObjectNode> entity = new HttpEntity<ObjectNode>(headers);
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.exchange(apiUrl, HttpMethod.GET, entity, ObjectNode.class).getBody();

	}

	public Response doLogin(ObjectNode content) throws Exception {

		Response res = new Response();
		SSO_LoginRes loginRes = new SSO_LoginRes();

		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

		String EIToken = "";
		String userName = "";

		try {

			ObjectNode responseNode = getToken(content);

			if (responseNode.has("error")) {
				loginRes.setErrorDescription(responseNode.get("error").toString());

				res.setStrJson(mapper.writeValueAsString(loginRes));
				mapper.readValue(res.getStrJson(), ObjectNode.class);
				System.out.println(res);
				return res;

			} else {
				String accessToken = responseNode.get("accessToken").toString();
				String[] tokenSplit = accessToken.split("\\.");
				String decodeStr = new String(Base64.decodeBase64(tokenSplit[1]), "UTF-8");

				System.out.println(decodeStr);
				ObjectNode tokenInfo = new ObjectMapper().readValue(decodeStr, ObjectNode.class);

				final JsonNode arrNode = new ObjectMapper().readTree(decodeStr);
				String role = arrNode.get("role").asText();
				userName = arrNode.get("firstName").asText();

				if (role.equalsIgnoreCase("tenant")) {
					ArrayNode userOrgIdNode = tokenInfo.withArray("groups");

//					String orgId = System.getenv("org_id");
					String orgId = "64ebfa4b-454e-4db3-8f91-7db3e169a962";

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

//						DEBUG
						if (!SSO.recvSrpIdAndSecret()) {
							loginRes.setErrorDescription(RESP_SRPIDFAILED);

							res.setStrJson(mapper.writeValueAsString(loginRes));

							return res;
						}
					}

					boolean flag = false;

					ArrayNode scopes = tokenInfo.withArray("scopes");

					for (int i = 0; i < scopes.size(); i++) {

						String strScope = scopes.get("accessToken").toString();
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

				EIToken = responseNode.get("accessToken").toString();
				loginRes.setAccessToken(EIToken);
				loginRes.setRefreshToken(responseNode.get("refreshToken").toString());
				loginRes.setTokenType(responseNode.get("tokenType").toString());

			}

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

		if (cfApi.substring(cfApi.indexOf("api.") + 4) == null) {
			loginRes.setErrorDescription(RESP_NULLDOMAIN);
			res.setStrJson(mapper.writeValueAsString(loginRes));
			return res;
		}

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

	public String doLoginByToken(String EIToken) throws Exception {
		SSO_LoginRes res = new SSO_LoginRes();

		try {

			if (EIToken.isEmpty()) {
				res.setErrorDescription("EITokennotfound");
				return res.getErrorDescription();
			} else {
				String[] tokenSplit = EIToken.split("\\.");
				String decodeStr = new String(Base64.decodeBase64(tokenSplit[1]), "UTF-8");
				JsonNode tokenInfo = new ObjectMapper().readValue(decodeStr, JsonNode.class);

				String role = tokenInfo.get("role").asText();
				System.out.println(role);

				if (role.equalsIgnoreCase("tenant")) {
					String userOrgId = tokenInfo.withArray("groups").get(0).textValue();
					String orgId = System.getenv("org_id");

					/*
					 * DEBUG
					 */
//					String orgId = "64ebfa4b-454e-4db3-8f91-7db3e169a962";

					if (!userOrgId.equalsIgnoreCase(orgId)) {

						res.setErrorDescription(RESP_UNAUTHORIZED);
						return res.getErrorDescription();
					}

				} else {
					if (SSO.srpId == null) {
						if (!SSO.recvSrpIdAndSecret()) {
							res.setErrorDescription(RESP_SRPIDFAILED);
							return res.getErrorDescription();
						}
					}
				}
				boolean flag = false;

				JsonNode scopes = tokenInfo.withArray("scopes");
				System.out.println("scopes  :" + tokenInfo);
				for (int i = 0; i < scopes.size(); i++) {

					String strScope = scopes.get("accessToken").toString();
					String[] eachScope = strScope.split("\\.");
					if (eachScope[0].equalsIgnoreCase(SSO.srpId)) {
						flag = true;
					}
				}
				if (flag == false) {
					res.setErrorDescription(RESP_UNAUTHORIZED);
					return res.getErrorDescription();
				}

			}

		} catch (Exception e) {
			// TODO: handle exception

			res.setErrorDescription(e.getMessage());

			return res.getErrorDescription();

		}

		return res.getErrorDescription();
	}
	
	/*
	 * Need to have access to SQL database. -Postpone- 
	 */
	
//public String patchUser( String EIToken,  String content) throws Exception{
//	
//	String email;
//	
//	SSO_PatchUserRes patchUserRes = new SSO_PatchUserRes();
//	try {
//
//		ObjectNode conJson = new ObjectMapper().readValue(content, ObjectNode.class);
//		email=conJson.get("email").textValue();
//		if(SSO.srpId==null) {
//			if(!SSO.recvSrpIdAndSecret()) {
//				patchUserRes.setErrorDescription(RESP_SRPIDFAILED);
//				return patchUserRes.getErrorDescription();
//			}
//		}
//		String urlPatch = SSO.recvSSOUrl()+"/v2.0/users/"+email+"/scopes";
//		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
//
//		 ObjectNode body = mapper.createObjectNode();
//		body.put("srpId", SSO.srpId);
//		body.put("srpSecret", SSO.srpSecret);
//		body.put("action", "append");
//		body.putArray("scopes");
//		body.put("scopes", "user");
//		StringEntity se=new StringEntity(body.toString())
//				
//		
//
//	}
//}

	public SSO_EIName doGetUserName(String EIName) {
		SSO_EIName resEIName = new SSO_EIName();

		try {
			resEIName.setUserName(EIName);
			return resEIName;
		} catch (Exception e) {
			// TODO: handle exception
			resEIName.setErrorDescription(e.getMessage());
			return resEIName;
		}

	}

	public Response doLogout() {
		Response res = new Response();

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

	public static ObjectNode getToken(ObjectNode auth) {
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
