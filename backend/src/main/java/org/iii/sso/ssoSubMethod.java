package org.iii.sso;

import java.io.IOException;

/**
 *
 * @author li.jie
 * updated by avbee 270319
 */

import java.util.Base64;
import java.util.Optional;

import org.iii.sso.AES;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootApplication
public class ssoSubMethod {

	public String srpId = null;
	public String srpSecret = null;
	public static ArrayNode scopes = null;
	public static String srpName = "";

	private static ssoService ssoService;

	public boolean recvSrpIdAndSecret() {
		try {
			final String srpToken = recvSrpToken();

			String orgId = System.getenv("org_id");
			String spaceId = recvApplicationEvn("space_id");

			String url = srpName + "?orgId=" + orgId + "&spaceId=" + spaceId;

			ObjectNode responseJson = ssoService.getSRPToken(srpToken, url);

			if (responseJson.has("error")) {
				String error = responseJson.get("error").toString();
				if (!error.equalsIgnoreCase("not found")) {
					return false;

				}
			} else {

				srpSecret = responseJson.get("srpSecret").asText();
				srpId = responseJson.get("srpId").asText();

				/*
				 * Scopes main contain multiple string value
				 */

				scopes = responseJson.withArray("scopes");

				/*
				 * Debug Function
				 */
//					scopes = null;
//					srpId = "";
//					System.out.println(scopes);

			}
			if (srpId == null) {

				ObjectMapper mapp = new ObjectMapper();
				ObjectNode addSrpBody = mapp.createObjectNode();
				addSrpBody.put("name", srpName);
				addSrpBody.put("orgId", orgId);
				addSrpBody.put("spaceId", spaceId);
				addSrpBody.put("appId", spaceId);
				addSrpBody.putArray("scopes").addAll(scopes);

				/*
				 * Debug Function
				 */
//					System.out.println("addSrpBody : " + addSrpBody);

			}

		} catch (Exception e) {
			// TODO: handle exception
			System.out.printf(e.getMessage());
			return false;

		}

		return true;
	}

	public String recvSSOUrl() {
		String ssoUrl = null;
		ssoUrl = System.getenv("sso_url");
		if (ssoUrl == null) {
			String cfApi = recvApplicationEvn("cf_api");
			/*
			 * String orgName = System.getenv("org_name"); if
			 * (orgName.equalsIgnoreCase("WISE-PaaS-Stage")) { return cfApi.replace("api",
			 * "portal-sso-stage"); } else if (orgName.equalsIgnoreCase("WISE-PaaS-Dev")) {
			 * return cfApi.replace("api", "portal-sso-develop"); } else { return
			 * cfApi.replace("api", "portal-sso"); }
			 */
			return cfApi.replace("api", "portal-sso");
		}
		return ssoUrl;
	}

	public String recvApplicationEvn(String key) {
		try {
			String app = System.getenv("VCAP_APPLICATION");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode appJson = mapper.readTree(app);
			return appJson.get(key).toString();

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	public  String recvSrpToken() {
		try {
			long currentTime = System.currentTimeMillis() / 1000;
			String src = String.valueOf(currentTime) + "-" + srpName;
			System.out.printf("%-32s %s %s\n", "SRP TOKEN SOURCE", ":", src);
			String key = "ssoisno12345678987654321";
			Optional<byte[]> oEncrypt = AES.Encrypt(src, key);

			String base64Str = Base64.getEncoder().encodeToString(oEncrypt.get());
			String base64UrlStr = Base64.getUrlEncoder().encodeToString(oEncrypt.get());
			System.out.printf("%-32s %s %s\n", "BASE64 ENCRYPT", ":", base64Str);
			System.out.printf("%-32s %s %s\n", "BASE64URL ENCRYPT", ":", base64UrlStr);
			return base64UrlStr;
		} catch (Exception e) {
			System.out.print(e.getMessage());
		}
		return null;
	}

	public boolean doValidateToken(String EIToken) throws IOException {

		ObjectNode tokenNode = ssoService.getValidateToken(EIToken);
		if (tokenNode.get("expiresIn").intValue() < 15 * 60) {
			return false;
		}
		return true;

	}

}
