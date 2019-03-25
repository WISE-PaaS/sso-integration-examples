package org.iii.sso;

import org.apache.tomcat.util.codec.binary.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootApplication
public class API_SSO {

	@Autowired
	private SsoService ssoService;

	public ResponseEntity<String> login(ObjectNode content) throws Exception {

		// public login(String content) throws Exception{
		SSO_LoginRes loginRes = new SSO_LoginRes();

		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		String strJson = "";
		String EIToken = "";
		String userName = "";

		try {
//			String strJSON = "";
			ObjectNode responseNode = ssoService.getToken(content);

			if (responseNode.has("error")) {
				loginRes.setErrorDescription(responseNode.get("error").toString());
				strJson = mapper.writeValueAsString(loginRes);
				return new ResponseEntity<String>(strJson, HttpStatus.OK);

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
						strJson = mapper.writeValueAsString(loginRes);
						return new ResponseEntity<String>(strJson, HttpStatus.OK);
					}

				} else {

					if (SSO.srpId == null) {
						if (!SSO.recvSrpIdAndSecret(strJson)) {
							loginRes.setErrorDescription("Get srp id failed");
							strJson = mapper.writeValueAsString(loginRes);
							return new ResponseEntity<String>(strJson, HttpStatus.OK);
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
						strJson = mapper.writeValueAsString(loginRes);
						return new ResponseEntity<String>(strJson, HttpStatus.OK);
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
			strJson = mapper.writeValueAsString(loginRes);
			return new ResponseEntity<String>(strJson, HttpStatus.OK);
		}

//
//		strJSON = gson.toJson(loginRes);
//
//		NewCookie[] cookies = new NewCookie[2];
//		String cfApi = SSO.GetApplicationEvn("cf_api");
//		String domain = cfApi.substring(cfApi.indexOf("api.") + 4);
//		cookies[0] = new NewCookie("EIToken", EIToken, "/", domain, "", 60 * 60, true, true);
//		cookies[1] = new NewCookie("EIName", username, "/", domain, "", 60 * 60, true);
//		return Response.status(HttpServletResponse.SC_OK).entity(strJSON).cookie(cookies).build();
		return null;
	}
	
	public ResponseEntity<String> loginByToken(HttpHeaders header){
		
		return null;
	}
}
