package org.iii.sso;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author li.jie
 * updated by avbee 270319
 */
public class PCFUtil {
	public static String postgresUrl = "";
	public static String postgresUsername = "";
	public static String postgresPassword = "";

	public static void GetPostgresEnv() {
		try {
			String services = System.getenv("VCAP_SERVICES");
			String postgreServiceName = System.getenv("postgresql_service_name");

//            JSONObject serJson = new JSONObject(services); 
			final JsonNode serJson = new ObjectMapper().readTree(services);
			// 根据用户提供的postgresql_service_name进行匹配
			if (serJson.has(postgreServiceName)) {

//                JSONObject creJson = serJson.getJSONArray(postgreServiceName).getJSONObject(0).getJSONObject("credentials");
				JsonNode creJson = serJson.withArray(postgreServiceName).get(0).get("credentials");

				postgresUrl = "jdbc:postgresql://" + creJson.get("host").textValue() + ":"
						+ creJson.get("port").textValue() + "/" + creJson.get("database").textValue();
				postgresUsername = creJson.get("username").textValue();
				postgresPassword = creJson.get("password").textValue();
			}
			// 如果没有找到相应的postgresql_service_name，则根据postgresql的前缀进行匹配
			else {
				Iterator<String> it = serJson.fieldNames();
				while (it.hasNext()) {
					String key = it.next();
					if (key.indexOf("postgresql") != -1) {
						JsonNode creJson = serJson.withArray(key).get(0).get("credentials");
						postgresUrl = "jdbc:postgresql://" + creJson.get("host").textValue() + ":"
								+ creJson.get("port").textValue() + "/" + creJson.get("database").textValue();
						postgresUsername = creJson.get("username").textValue();
						postgresPassword = creJson.get("password").textValue();
					}
				}
			}
			// 如果以上两种都没有读到，则读取环境变量POSTGRESQL的值
			if (postgresUrl.isEmpty() || postgresUsername.isEmpty() || postgresPassword.isEmpty()) {
				String postStr = System.getenv("POSTGRESQL");
				JsonNode postJson = new ObjectMapper().readTree(postStr);
				JsonNode creJson = postJson.get("credentials");
				postgresUrl = "jdbc:postgresql://" + creJson.get("host").textValue() + ":"
						+ creJson.get("port").textValue() + "/" + creJson.get("database").textValue();
				postgresUsername = creJson.get("username").textValue();
				postgresPassword = creJson.get("password").textValue();
			}
		} catch (Exception e) {
			System.out.printf(e.getMessage());
		}
	}

}