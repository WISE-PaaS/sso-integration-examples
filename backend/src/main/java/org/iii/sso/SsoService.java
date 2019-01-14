package org.iii.sso;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class SsoService {

  private static final String SSO_API_ENDPOINT = "https://portal-sso.wise-paas.com/v2.0";

  // p.10~p.12 程式碼新增於此行之後
  public ObjectNode getToken(ObjectNode auth) {
    String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/auth/native");
    RestTemplate restTemplate = new RestTemplate();
    RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(auth, HttpMethod.POST, URI.create(apiUrl));
    return restTemplate.exchange(req, ObjectNode.class).getBody();
  }

  public ObjectNode refreshToken(ObjectNode tokenPayload) {
    String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/token");
    RestTemplate restTemplate = new RestTemplate();
    RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(tokenPayload, HttpMethod.POST, URI.create(apiUrl));
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

}
