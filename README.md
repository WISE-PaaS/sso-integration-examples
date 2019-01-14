
# SSO 整合範例

WISE-PaaS 提供單一登入身份驗證方案。使用單一登入，使用者只需登入一次即可使用在 WISE-PaaS 上不同的應用程式。

單一登入提供了兩種整合方式，一種用於前端應用程式，另一種則用於後端（原生）應用程式。

您可以下載本教學的開源代碼庫，或使用 Git clone：
```
git clone https://github.com/WISE-PaaS/sso-integration-examples.git
```

## API 文件

範例程式碼中所使用的 API 可以在此[文件](https://portal-technical.wise-paas.com/doc/document-portal.html#SSO-2)中找到。

## 前端應用程式

以前端的整合方式來說，使用者必須使用單一登入方案進行登入才能獲得 **EIToken** cookie 來完成身份驗證。例如 [Technical Portal](https://portal-technical.wise-paas.com) 就是如此。

此外，由於安全性問題，前端應用程式是無法直接從瀏覽器取得此 cookie 的。但它仍然可以透過 Ajax 取得使用者資訊。

### [HTML]
#### 步驟 1. 建立 index.html，宣告主要變數，並匯入jQuery函式庫
```
<html>
<head>
    <title>SSO Integration Guide</title>
</head>
<body>
    <script>
        var ssoUri = 'https://portal-sso.wise-paas.com';
        var redirectUriAfterLogin= '{登入成功後欲導向的URI}';
        var redirectUriAfterLogout= '{登出成功後欲導向的URI}';
    </script>
    <script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
    <!-- 步驟2~4 語法新增於此行之後 -->
</body>
</html>
```
您可以自行決定是否更改在 `<title>` 中的網頁標題。此範例預設為 **SSO Example**。

#### 步驟 2. 新增登入按鈕，並將登入按鈕整合登入功能
```
	<button type="button" id="signInBtn">Sign in</button>

	<script>
	    $('#signInBtn').click(function () {
	        window.location.href = ssoUri + '/web/signIn.html?redirectUri=' + redirectUriAfterLogin;
	    });
	</script>
```
#### 步驟 3. 新增登出按鈕，並將登出按鈕整合登出功能
```
	<button type="button" id="signOutBtn">Sign out</button>

	<script>
	    $('#signOutBtn').click(function () {
	        window.location.href = ssoUri + '/web/signOut.html?redirectUri=' + redirectUriAfterLogout;
	    });
	</script>
```
#### 步驟4. 驗證整合是否成功，將以下語法新增至登入成功後欲導向的URI之前端網頁，該網頁亦須匯入jQuery函式庫，藉由取得使用者姓名的方式進行驗證
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
#### 步驟5. 將前端應用程式部署至WISE-PaaS雲平台
```
$ cf login
[linux]
$ touch Staticfile
[windows]
$ $null >> Staticfile
$ cf push {應用程式名稱} -m 32M
```
更多部署資訊可參考：
https://docs.cloudfoundry.org/buildpacks/staticfile/index.html

## 後端（原生）應用程式
以後端（原生）的整合方式來說，這裡提供的是一個基於 Java 的範例。本範例將學習如何取得 **EIToken** 並透過它取得使用者資訊。

### [前置條件]
#### 所需環境
[Spring Tool Suite](https://spring.io/tools)
#### 所需技術
1. [Java 1.8](https://java.com/zh_TW/)
2. [Gradle](https://gradle.org/)
3. [Spring Boot](https://projects.spring.io/spring-boot/)

### [Java]
#### 步驟 1. 建立Java檔案，宣告主要變數，並加上@Service註解
```
@Service
public class SsoService {

    private static final String SSO_API_ENDPOINT = "https://portal-sso.wise-paas.com/v2.0";

    // 步驟2~4 程式碼新增於此行之後
}
```
#### 步驟 2. 整合取得令牌功能
```
	public ObjectNode getToken(ObjectNode auth) {
	    String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/auth/native");
	    RestTemplate restTemplate = new RestTemplate();
	    RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(auth, HttpMethod.POST, URI.create(apiUrl));
	    return restTemplate.exchange(req, ObjectNode.class).getBody();
	}
```
#### 步驟 3. 整合重新換發令牌功能
```
	public ObjectNode refreshToken(ObjectNode tokenPayload) {
	    String apiUrl = String.format("%s%s", SSO_API_ENDPOINT, "/token");
	    RestTemplate restTemplate = new RestTemplate();
	    RequestEntity<ObjectNode> req = new RequestEntity<ObjectNode>(tokenPayload, HttpMethod.POST, URI.create(apiUrl));
	    return restTemplate.exchange(req, ObjectNode.class).getBody();
	}
```

#### 步驟 4. 整合取得令牌之使用者資訊功能
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
#### 步驟 5. 驗證整合是否成功，可建立新Java檔案，並加上@RestController註解
```
@RestController
public class SsoController {

    @Autowired
    private SsoService ssoService;

    @Autowired
    private ObjectMapper objMapper;

    // 步驟6 程式碼新增於此行之後
}
```
#### 步驟 6. 藉由新增取得使用者資訊之API進行驗證
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
#### 步驟 7. 將後端應用程式至WISE-PaaS雲平台
```
$ cf login
$ cf push {應用程式名稱} -m 512M -p build/libs/backend-1.0.0.jar -b java_offline_buildpack
```
更多部署資訊請參考：
https://github.com/cloudfoundry/java-buildpack
https://docs.cloudfoundry.org/devguide/deploy-apps/manifest.html
