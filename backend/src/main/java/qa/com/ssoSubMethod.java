package qa.com;

import java.io.IOException;

/**
 *
 * @author li.jie
 * updated by avbee 270319
 */

import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import qa.com.AES;

@Component
public class ssoSubMethod {

	public String srpId = null;
	public String srpSecret = null;
	public static ArrayNode scopes = null;
	public static String srpName = "";

	final String TOKEN_VALIDATION_OK = "TOKEN_VALIDATION_OK";

	@Autowired
	private ssoService ssoService;

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ssoSubMethod.class);

	public boolean recvSrpIdAndSecret() throws Exception {
		LOGGER.info("Initiate recvSrpIdAndSecret");
		try {
			LOGGER.info("Initiate recvSrpToken");
			final String srpToken = recvSrpToken();
			LOGGER.info("recvSrpToken success");

			String orgId = System.getenv("org_id");

			LOGGER.info("Initiate recvApplicationEvn");
			String spaceId = recvApplicationEvn("space_id");
			LOGGER.info("Initiate recvApplicationEvn");

			String url = srpName + "?orgId=" + orgId + "&spaceId=" + spaceId;

			LOGGER.info("Initiate recvSrpIdAndSecret.getSRPToken");
			ObjectNode responseJson = ssoService.getSRPToken(srpToken, url);
			LOGGER.info("recvSrpIdAndSecret.getSRPToken success");

			if (responseJson.has("error")) {
				LOGGER.info("recvSrpIdAndSecret.responseJson has error");

				String error = responseJson.get("error").toString();
				if (!error.equalsIgnoreCase("not found")) {
					LOGGER.info("recvSrpIdAndSecret.responseJson has error : not found");

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
				LOGGER.info("recvSrpIdAndSecret.srpId is null");

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

			}

		} catch (Exception e) {
			// TODO: handle exception
			System.out.printf(e.getMessage());
			return false;

		}
		LOGGER.info("recvSrpIdAndSecret success");
		return true;
	}

	public String recvSSOUrl() throws IOException {
		LOGGER.info("initiate recvSSOUrl");

		String ssoUrl = null;
		ssoUrl = System.getenv("sso_url");
		if (ssoUrl == null) {
			LOGGER.info("recvSSOUrl.ssoUrl is null");
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
		LOGGER.info("recvSSOUrl success");
		return ssoUrl;
	}

	public String recvApplicationEvn(String key) throws IOException {
		LOGGER.info("initiate recvApplicationEvn :" + key);

		String app = System.getenv("VCAP_APPLICATION");
		ObjectMapper mapper = new ObjectMapper();
		JsonNode appJson = mapper.readTree(app);

		LOGGER.info("recvApplicationEvn success :" + key);

		return appJson.get(key).toString();
	}

	public String recvSrpToken() {
		LOGGER.info("initiate recvSrpToken");

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
		LOGGER.info("recvSrpToken success");
		return null;
	}

	public boolean doValidateToken(String EIToken) throws Exception {
		LOGGER.info("initiate doValidateToken");

		try {
			LOGGER.info("initiate recvSrpToken");
			ObjectMapper mapp = new ObjectMapper();
			ObjectNode json = mapp.createObjectNode().put("token", EIToken);

			ObjectNode myNode = ssoService.validateToken(json);
			if (myNode.get("expiresIn").intValue() < 15 * 60) {
				return false;
			}
			LOGGER.info(TOKEN_VALIDATION_OK);

		} catch (Exception e) {
			// TODO: handle exception
//			System.out.println(e);
			return false;
		}

		LOGGER.info("doValidateToken success");
		return true;
	}

}
