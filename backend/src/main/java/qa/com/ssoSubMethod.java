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

import qa.com.classDefinition.UserRoleByScopes;
import qa.com.ssoException.CannotAcquireDataException;

@Component
public class ssoSubMethod {

	public String srpId = null;
	public String srpSecret = null;
	public static ArrayNode scopes = null;
	public static String srpName = "";

	final String RESP_AUTHORIZED = "Authorized";
	final String RESP_UNAUTHORIZED = "Unauthorized";
	final String RESP_SRPIDFAILED = "get srp id failed";

	final String role_tenant = "tenant";
	final String role_admin = "admin";
	final String role_srpUser = "srpuser";
	final String role_developer = "developer";

	final int tokenMinValidityTime = 15 * 60 /* minutes * seconds */;

	@Autowired
	private ssoService ssoService;

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ssoSubMethod.class);
	private static final String VCAP_APPLICATION = "VCAP_APPLICATION";

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

			if (srpId.equals(null)) {
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

	public String recvSSOUrl() throws IOException, CannotAcquireDataException {
		LOGGER.info("initiate recvSSOUrl");

		String ssoUrl = null;

		try {
			ssoUrl = System.getenv("sso_url");
			if (ssoUrl.equals(null)) {
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
		} catch (Exception e) {
			throw new CannotAcquireDataException(e.getMessage());
		}
	}

	public String recvApplicationEvn(String key) throws IOException, CannotAcquireDataException {
		try {

			LOGGER.info("initiate recvApplicationEvn :" + key);

			String app = System.getenv(VCAP_APPLICATION);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode appJson = mapper.readTree(app);

			LOGGER.info("recvApplicationEvn success :" + key);

			/*
			 * Bugs: when using .toString() will return string include double quotes(")
			 * Change: .textValue()
			 */

			return appJson.get(key).textValue();
		} catch (Exception e) {

			throw new CannotAcquireDataException("failed to get environment " + VCAP_APPLICATION);
		}
	}

	public String recvSrpToken() throws CannotAcquireDataException {
		LOGGER.info("initiate recvSrpToken");

		try {
			long currentTime = System.currentTimeMillis() / 1000;

			String src = String.valueOf(currentTime) + "-" + srpName;

			System.out.printf("%-32s %s %s\n", "SRP TOKEN SOURCE", ":", src);

			String key = "ssoisno12345678987654321";

			Optional<byte[]> oEncrypt = AES.Encrypt(src, key);

			String base64UrlStr = Base64.getUrlEncoder().encodeToString(oEncrypt.get());

			/*
			 * Debug
			 */
//			System.out.printf("%-32s %s %s\n", "BASE64 ENCRYPT", ":", base64Str);
//			System.out.printf("%-32s %s %s\n", "BASE64URL ENCRYPT", ":", base64UrlStr);

			return base64UrlStr;

		} catch (Exception e) {
			throw new CannotAcquireDataException(e.getMessage());

		}
	}

	public boolean doValidateToken(String EIToken) throws Exception {
		LOGGER.info("initiate doValidateToken");

		LOGGER.info("initiate recvSrpToken");
		ObjectMapper mapp = new ObjectMapper();
		ObjectNode json = mapp.createObjectNode().put("token", EIToken);

		ObjectNode myNode = ssoService.validateToken(json);

		/* If token is expired will receive error in JSON response */
		if (myNode.has("error")) {

			/* Initiate logout */

			return false;
		}

		/*
		 * If token is not expired but have minimum valid time, initiate refresh token
		 */
		if (myNode.get("expiresIn").intValue() < tokenMinValidityTime) {

			LOGGER.info("doValidateToken.tokenExpirationTime \"if\" token need to be refreshed");

			return true;
		}

		LOGGER.info("doValidateToken success");

		return true;
	}

	public UserRoleByScopes getUserRoleByScopes(String decodeStr) throws Exception {
		try {
			/*
			 * 1. If there is orgid in cfscope and the current org is the same, then need to
			 * verify that the permission is tenant, if it is tenant, then the verification
			 * ends, the user has the highest authority of the dashboard, both admin. 2. If
			 * it is not tenant, then check if the user is developer. If it is developer,
			 * then check if the space corresponding to developer is consistent with the
			 * current space. If it is consistent, first give the user a minimum permission.
			 * In the dashboard, use developer here. Distinguish as a sign. 3. Cfscope
			 * traversal, if it is tenant, the verification is finished in the first step,
			 * if not, then get the current app registered srpid, get the user’s permission
			 * to the corresponding srpid app from the scope. (ps: For developer, if the
			 * relevant permissions are not found in step 3, the second part also sets the
			 * default viewer permissions for it) 4. Then give the permission to the user of
			 * this operation.
			 * 
			 * In general, these verifications are done in the middleware of the filter, and
			 * all authenticated interfaces need to be verified.
			 */

			LOGGER.info("initiate getUserRoleByScopes");

			UserRoleByScopes userRoleByScopes = new UserRoleByScopes();
			userRoleByScopes.setRoleFlag(false);

			final JsonNode arrNode = new ObjectMapper().readTree(decodeStr);

			JsonNode cfScope = arrNode.withArray("cfScopes");

			System.out.println(cfScope);
			String role = arrNode.get("role").asText();

			/* Accessing object inside cfScope */
			for (int i = 0; i < cfScope.size(); i++) {

				LOGGER.info("ssoSubMethod 'for' Accessing object inside jsonArray");

				/* Verified if current orgId equal with cfscope guid */
				if (System.getenv("org_id").equalsIgnoreCase(arrNode.get("cfScopes").get(i).get("guid").asText())) {

					/* Verified if role is tenant */
					LOGGER.info("ssoSubMethod.userRoleByScopes 'if' verified if role is tenant ");
					if (role.equalsIgnoreCase(role_tenant)) {

						LOGGER.info("ssoSubMethod.userRoleByScopes 'if'  role is tenant ");
						userRoleByScopes.setDesc(RESP_AUTHORIZED);
						userRoleByScopes.setLoginFlag(true);
						return userRoleByScopes;
					}

					LOGGER.info("ssoSubMethod.userRoleByScopes 'if'  role is NOT tenant ");
					throw new CannotAcquireDataException(RESP_UNAUTHORIZED);
//					userRoleByScopes.setDesc(RESP_UNAUTHORIZED);
//					userRoleByScopes.setLoginFlag(false);
//					return userRoleByScopes;
				}

				/* Verification of srpId for other users */
				else if (srpId.equals(null)) {

					LOGGER.info("ssoSubMethod.srpId 'else' is null");

					if (!recvSrpIdAndSecret()) {

						LOGGER.info("ssoSubMethod.recvSrpIdAndSecret 'if' is false");

						throw new CannotAcquireDataException(RESP_SRPIDFAILED);
//						userRoleByScopes.setDesc(RESP_SRPIDFAILED);
//						userRoleByScopes.setLoginFlag(false);
//						return userRoleByScopes;

					}

					/* At this stage srpId should already present */
				}
				if (role.equalsIgnoreCase(role_developer)) {

					LOGGER.info("ssoSubMethod.role 'if' equal developer ");

					/* Matching role with space */
					for (int j = 0; j < arrNode.get("cfScopes").get(i).get("spaces").size(); j++) {

						/* Iterate for all possible elements inside spaces */
						if (arrNode.get("cfScopes").get(i).get("spaces").get(j).textValue()
								.equalsIgnoreCase(recvApplicationEvn("space_id"))) {

							/*
							 * When some element inside cfScopes.spaces match space_id will return
							 * authorized
							 */
							userRoleByScopes.setDesc(RESP_AUTHORIZED);
							userRoleByScopes.setLoginFlag(true);
							return userRoleByScopes;
						}

					}

				}
			}

			/* Initiate when role is not tenant and there is no spaceid inside cfScopes */
			JsonNode scopes = arrNode.withArray("scopes");
			if (!role.equalsIgnoreCase(role_tenant)) {

				LOGGER.info("ssoSubMethod.userRoleByScopesscopes.scopes is true");
				for (int k = 0; k < scopes.size(); k++) {

					/* Iterate for all possible elements inside scopes */
					String[] scopesElement = scopes.get(k).textValue().split("\\.");

					if (scopesElement[0].equalsIgnoreCase(srpId)) {

						/* When some element of scopes matched with srpId then returns authorized */
						LOGGER.info("userRoleByScopes.userRoleByScopesscopes.scopesElement is true");
						userRoleByScopes.setDesc(RESP_AUTHORIZED);
						userRoleByScopes.setLoginFlag(true);
						return userRoleByScopes;
					}

				}
			}
			/* No element of scopes match srpId then returns unauthorized */
			LOGGER.info("ssoSubMethod.userRoleByScopesscopes.scopesElement is false");
			throw new CannotAcquireDataException(RESP_UNAUTHORIZED);
//			userRoleByScopes.setDesc(RESP_UNAUTHORIZED);
//			userRoleByScopes.setLoginFlag(false);
//			return userRoleByScopes;

		} catch (Exception e) {
			throw new CannotAcquireDataException(e.getMessage());
		}
	}
}
