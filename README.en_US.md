
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

#### IDE
[Spring Tool Suite](https://spring.io/tools)

#### Dependencies
1. [Java 1.8](https://java.com/zh_TW/)
2. [Gradle v2.1.3.RELEASE](https://gradle.org/)
3. [Spring Boot](https://projects.spring.io/spring-boot/)
3. [Spring Retry](https://github.com/spring-projects/spring-retry)

## Local deployment
Code can be deployed on localhost for testing.  Execute this command from '/backend' root folder.
```
gradle bootRun
```
Soon springboot server will be ready.


## Set application.properties
The file is located in /backend/src/main/resources/application.properties
Set the parameter for server port and PostgreSql credentials

```
# Customize server port
server.port=8080

# PostgreSql url
sso.postgresql.url={foo}

# PostgreSql password
sso.postgresql.pwd={foo}

# PostgreSql username
sso.postgresql.user={foo}
```

## SSO Endpoint Parameters
Each API End
### GET
-------------------
#### /users/me
Form : Header
```
username
password
```

#### /user
Form : Header
```
EIName
```

#### /params
Form : None


### POST
------------------
#### /login
Form : Body JSON
```
{
	"username":"foo@bar.com",
	"password":"foobar"
}
```

#### /login/token
Form : Cookie
```
EIToken=foo
refreshToken=bar
```


#### /logout
Form : None
```
None
```


### Deploy Code on Cloud Foundry


#### Step 1. Build .jar file
We need jar file in order to push into Cloud Foundry for the deployment. Jar file can be built by typing :
```
$ gradle build
```

#### Step 2. Edit manifest.yml
```
#manifest.yml
  ---  
applications:
- name: <app-name>
  disk_quota: 1G
    buildpacks:
    - Java_v4-12
  instances: 1
  memory: 1G
  path: path/to/jarfile.jar
  env:      
    org_id: <your_org _id>
```
#### Step 3. Deploy our backend app to WISE-PaaS
```
cf login
cf push -f manifest.yml
```

#### Step 4. Check whether application is running properly.

```
cf logs <Appname> --recent
```




For more information on how to deploy app, please see:
https://github.com/cloudfoundry/java-buildpack
https://docs.cloudfoundry.org/devguide/deploy-apps/manifest.html
