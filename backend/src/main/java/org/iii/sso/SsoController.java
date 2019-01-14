package org.iii.sso;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class SsoController {

  @Autowired
  private SsoService ssoService;

  @Autowired
  private ObjectMapper objMapper;

  // p.14程式碼新增於此行之後
  @RequestMapping(method = RequestMethod.GET, value = "/users/me")
  public ResponseEntity<ObjectNode> getTokenUser(@RequestParam(value = "username", required = true) String username,
      @RequestParam(value = "password", required = true) String password) {
    ObjectNode auth = objMapper.createObjectNode().put("username", username).put("password", password);
    ObjectNode tokenPackage = ssoService.getToken(auth);
    ObjectNode tokenPayload = objMapper.createObjectNode().put("token", tokenPackage.get("refreshToken").asText());
    tokenPackage = ssoService.refreshToken(tokenPayload);
    return new ResponseEntity<ObjectNode>(ssoService.getTokenUser(tokenPackage.get("accessToken").asText()),
        HttpStatus.OK);
  }

}
