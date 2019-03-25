package org.iii.sso;

import java.net.URI;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.tomcat.util.codec.binary.Base64;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import org.springframework.http.RequestEntity;

import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@AutoConfigurationPackage
@Service
public class SsoService {

	public static final String SSO_API_ENDPOINT = "https://portal-sso.ali.wise-paas.com.cn/v2.0";

	public static ObjectNode getSRPToken(String srpToken, String endPointUrl) {

		String apiUrl = String.format("%s%s/%s", SSO_API_ENDPOINT, "/srps", endPointUrl);
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Auth-SRPToken", String.format("%s", srpToken));
		HttpEntity<ObjectNode> entity = new HttpEntity<ObjectNode>(headers);
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.exchange(apiUrl, HttpMethod.GET, entity, ObjectNode.class).getBody();

	}

	public loginToken login(ObjectNode content) throws Exception {
		loginToken returnValues = new loginToken();
		SSO_LoginRes loginRes = new SSO_LoginRes();

		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		String strJson = "";
		String EIToken = "";
		String userName = "";

		try {

			ObjectNode responseNode = getToken(content);
			System.out.println("respoonse: "+responseNode);

			if (responseNode.has("error")) {
				loginRes.setErrorDescription(responseNode.get("error").toString());

				returnValues.strJson = mapper.writeValueAsString(loginRes);

				return returnValues;

			} else {
				String accessToken = responseNode.get("accessToken").toString();
				String[] tokenSplit = accessToken.split("\\.");
				String decodeStr = new String(Base64.decodeBase64(tokenSplit[1]), "UTF-8");

				ObjectNode tokenInfo = new ObjectMapper().readValue(decodeStr, ObjectNode.class);
				String role = tokenInfo.get("role").toString();
				userName = tokenInfo.get("firstname").toString();

				if (role.equalsIgnoreCase("tenant")) {
					String userOrgId = tokenInfo.withArray("groups").toString();
					String orgId = System.getenv("org_id");

					if (!userOrgId.equalsIgnoreCase(orgId)) {
						loginRes.setErrorDescription("Unauthorized");
//							strJson = mapper.writeValueAsString(loginRes);
//							return new ResponseEntity<ObjectNode>(strJson, HttpStatus.OK);
						returnValues.strJson = mapper.writeValueAsString(loginRes);

						return returnValues;
					}

				} else {

					if (SSO.srpId == null) {
						
						if (!SSO.recvSrpIdAndSecret()) {
							loginRes.setErrorDescription("Get srp id failed");
//								strJson = mapper.writeValueAsString(loginRes);
//								return new ResponseEntity<ObjectNode>(strJson, HttpStatus.OK);
							returnValues.strJson = mapper.writeValueAsString(loginRes);

							return returnValues;
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
						loginRes.setErrorDescription("Unauthorized");
//							strJson = mapper.writeValueAsString(loginRes);
//							return new ResponseEntity<ObjectNode>(strJson, HttpStatus.OK);
						returnValues.strJson = mapper.writeValueAsString(loginRes);

						return returnValues;
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


			returnValues.strJson = mapper.writeValueAsString(loginRes);
			return returnValues;

		}
		strJson = mapper.writeValueAsString(loginRes);

		Cookie[] cookie = new Cookie[2];

		String cfApi = SSO.recvApplicationEvn("cf_api");
		String domain = cfApi.substring(cfApi.indexOf("api.") + 4);
		cookie[0] = new Cookie("EIToken", EIToken);
		cookie[0].setDomain(domain);
		cookie[0].setMaxAge(60 * 60);
		cookie[0].setPath("/");
		cookie[0].setSecure(true);
		cookie[0].setHttpOnly(true);

		cookie[0] = new Cookie("EIName", userName);
		cookie[1].setDomain(domain);
		cookie[1].setMaxAge(60 * 60);
		cookie[1].setPath("/");
		cookie[1].setSecure(true);

		returnValues.strJson = mapper.writeValueAsString(loginRes);
		returnValues.cookie = cookie;
		return returnValues;

	}

	public Response loginByToken(HttpHeaders headers)throws Exception{
		Response returnValues = new Response();
		
		SSO_Response res = new SSO_Response();
		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		try {
			
			String EIToken = "";
			String cookies = headers.getFirst(HttpHeaders.SET_COOKIE);
		}catch (Exception e) {
			// TODO: handle exception
			res.setErrorDescription(e.getMessage());
			returnValues.strJson = mapper.writeValueAsString(res);
			return returnValues;
			
		}
		returnValues.strJson = mapper.writeValueAsString(res);
		return returnValues;
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



