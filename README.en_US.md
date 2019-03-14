
# SSO Integration Example

WISE-PaaS' support for SSO allows the users to access different applications with a one-time login.

There are two ways a developer could easily integrate SSO into his or her service as you may discover in the example.

You may download the source codes in the repo or simply Git clone：
```
git clone https://github.com/WISE-PaaS/sso-integration-examples.git
```

## API Documents

The documentation for the API's used in this example can be found [here](https://portal-technical.wise-paas.com/doc/document-portal.html#SSO-2).

## Integrating SSO with Your Frontend Application

The users are required to login via SSO service to retrive **EIToken** cookie to complete identity verification. [Technical Portal](https://portal-technical.wise-paas.com) is one example!
Due to security concerns, the frontend application is not allowed to acquire this cookie from the browser. However, we could still get the user credentials through Ajax.

### [HTML & JavaScript]
#### Step 1. Create index.html, declare variables and import jQuery
```
<html>
<head>
    <title>SSO Integration Guide</title>
</head>
<body>
    <script>
        var ssoUri = 'https://portal-sso.wise-paas.com';
        var redirectUriAfterLogin= '{URI to redirect after successful login}';
        var redirectUriAfterLogout= '{URI to redirect after successful logout}';
    </script>
    <script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
    <!-- Step 2~4 code snippets after this line -->
</body>
</html>
```
Developers may decide if they want to change the `<title>` for their site. Here we use **SSO Integration Guide**.

#### Step 2. Add a login button
```
    <button type="button" id="signInBtn">Sign in</button>

    <script>
        $('#signInBtn').click(function () {
            window.location.href = ssoUri + '/web/signIn.html?redirectUri=' + redirectUriAfterLogin;
        });
    </script>
```
#### Step 3. Add a logout button
```
    <button type="button" id="signOutBtn">Sign out</button>

    <script>
        $('#signOutBtn').click(function () {
            window.location.href = ssoUri + '/web/signOut.html?redirectUri=' + redirectUriAfterLogout;
        });
    </script>
```
#### Step 4. Test the function by adding below code snippets into the page to be displayed after successful login. This page should also import jQuery.
```
    <script>
        $.ajax({
            url: ssoUri  + '/v2.0/users/me',
            method: 'GET',
            xhrFields: {
                withCredentials: true
            }
        }).done(function (user) {
            alert('Hello! ' + user.lastName + ' ' + user.firstName);
        });
    </script>
```
#### Step 5. Deploy frontend app to WISE-PaaS
```
$ cf login
[linux]
$ touch Staticfile
[windows]
$ $null >> Staticfile
$ cf push {name of app} -m 32M
```
For more information on deploying app please see:
https://docs.cloudfoundry.org/buildpacks/staticfile/index.html

## Integrating SSO with Your Backend Application
Here we provide an example in Java to show how to integrate SSO with a backend, or any native application in general.
We first retrieve **EIToken**, then use it to get user credentials.

### [Pre-requisites]
#### Environment
[Spring Tool Suite](https://spring.io/tools)
#### Language and Tools
1. [Java 1.8](https://java.com/zh_TW/)
2. [Gradle](https://gradle.org/)
3. [Spring Boot](https://projects.spring.io/spring-boot/)

### [Java]
#### Step 1. Create a Java file, declare variables and annotate the class with @Service
```
@Service
public class SsoService {

    private static final String SSO_API_ENDPOINT = "https://portal-sso.ali.wise-paas.com.cn/v2.0";

    // Step 2~4 code snippets after this line
}
```
#### Step 2. Add a function to get SSO token
```
    public ObjectNode getToken(ObjectNode auth) {
        String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/auth/native");
        RestTemplate restTemplate = new RestTemplate();
        RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(auth, HttpMethod.POST, URI.create(apiUrl));
        return restTemplate.exchange(req, ObjectNode.class).getBody();
    }
```
#### Step 3. Add a function to refresh SSO token
```
    public ObjectNode refreshToken(ObjectNode tokenPayload) {
        String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/token");
        RestTemplate restTemplate = new RestTemplate();
        RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(tokenPayload, HttpMethod.POST, URI.create(apiUrl));
        return restTemplate.exchange(req, ObjectNode.class).getBody();
    }
```

#### Step 4. Add a function to get user credentials from the SSO token
```
    public ObjectNode getTokenUser(String accessToken) {
        String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/users/me");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", String.format("Bearer %s", accessToken));
        RequestEntity<Void> req = new RequestEntity<Void>(headers, HttpMethod.GET, URI.create(apiUrl));
        return restTemplate.exchange(req, ObjectNode.class).getBody();
    }
```
#### Step 5. Verify our code by creating a new Java file with the following code snippets
```
@RestController
public class SsoController {

    @Autowired
    private SsoService ssoService;

    @Autowired
    private ObjectMapper objMapper;

    // Step 6 code snippets after this line
}
```
#### Step 6. Create a function to get user information via API call using the token
```
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
```
#### Step 7. Build .jar file
We need jar file in order to push into Cloud Foundry for the deployment. Jar file can be built by typing :
```
$ gradle build
```

#### Step 8. Deploy our backend app to WISE-PaaS
```
$ cf login
$ cf push {name of app} -m 512M -p build/libs/backend-1.0.0.jar -b java_buildpack_offline
```
For more information on how to deploy app, please see:
https://github.com/cloudfoundry/java-buildpack
https://docs.cloudfoundry.org/devguide/deploy-apps/manifest.html
