# Tõlkevärav’s Keycloak-based SSO

## Keycloak Configuration

How a Keycloak realm should be configured for the SSO to work as expected for a given public client:
1. An identity provider should be created for Tara.
   * Tara requires that client id and secret be sent in the "Basic" format.
   * An example of identity provider configuration as a screenshot of Keycloak UI:
     * ![](https://github.com/keeleinstituut/tv-sso/blob/github-media/identity-provider-tara.jpg)
   * An example of identity provider configuration as JSON:
     ```json
     {
       "alias": "oidc",
       "displayName": "TARA (Test)",
       "internalId": "e21535ca-d522-4985-aaba-f1e3720a621d",
       "providerId": "oidc",
       "enabled": true,
       "updateProfileFirstLoginMode": "on",
       "trustEmail": false,
       "storeToken": false,
       "addReadTokenRoleOnCreate": false,
       "authenticateByDefault": false,
       "linkOnly": false,
       "firstBrokerLoginFlowAlias": "first broker login",
       "config": {
         "userInfoUrl": "https://tara-test.ria.ee/oidc/profile",
         "validateSignature": "true",
         "hideOnLoginPage": "false",
         "tokenUrl": "https://tara-test.ria.ee/oidc/token",
         "acceptsPromptNoneForwardFromClient": "false",
         "clientId": "eki-tolkevarav-dev",
         "uiLocales": "false",
         "jwksUrl": "https://tara-test.ria.ee/oidc/jwks",
         "backchannelSupported": "false",
         "issuer": "https://tara-test.ria.ee",
         "useJwksUrl": "true",
         "loginHint": "false",
         "pkceEnabled": "false",
         "clientAuthMethod": "client_secret_basic",
         "authorizationUrl": "https://tara-test.ria.ee/oidc/authorize",
         "disableUserInfo": "false",
         "syncMode": "IMPORT",
         "clientSecret": "**********",
         "passMaxAge": "false",
         "allowedClockSkew": "0"
       }
     }       
     ```
2. The "first broker login" authentication flow should be modified such that the "Review Profile" step is disabled.
   * An example of modified default "first broker login" flow as a screenshot of Keycloak UI:
     * ![](https://github.com/keeleinstituut/tv-sso/blob/github-media/first-broker-login-authentication-flow.jpg)
   * An example of modified default "first broker login" flow as JSON:
     ```json
     {
       "id": "14a08b01-3391-4236-bb28-1742d4d9d9c8",
       "alias": "first broker login",
       "description": "Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account",
       "providerId": "basic-flow",
       "topLevel": true,
       "builtIn": true,
       "authenticationExecutions": [
         {
           "authenticatorConfig": "review profile config",
           "authenticator": "idp-review-profile",
           "authenticatorFlow": false,
           "requirement": "DISABLED",
           "priority": 10,
           "autheticatorFlow": false,
           "userSetupAllowed": false
         },
         {
           "authenticatorFlow": true,
           "requirement": "REQUIRED",
           "priority": 20,
           "autheticatorFlow": true,
           "flowAlias": "User creation or linking",
           "userSetupAllowed": false
         }
       ]
     }
     ```
3. The "browser" authentication flow should be modified such that "forms" step is disabled and the "Identity Provider Redirector" step’s default identity provider is set to the identity provider created in step 1.
    * An example of the modified default "browser" flow as a screenshot of Keycloak UI: 
      * ![](https://github.com/keeleinstituut/tv-sso/blob/github-media/browser-authentication-flow.jpg)
      * ![](https://github.com/keeleinstituut/tv-sso/blob/github-media/identity-provider-redirector.png.jpg)
    * An example of the modified default "browser" flow as "authenticator config" as JSON:
      ```json
      {
        "id": "e788c28f-53f9-4561-a37f-c8c9c1540745",
        "alias": "browser",
        "description": "browser based authentication",
        "providerId": "basic-flow",
        "topLevel": true,
        "builtIn": true,
        "authenticationExecutions": [
          {
            "authenticator": "auth-cookie",
            "authenticatorFlow": false,
            "requirement": "ALTERNATIVE",
            "priority": 10,
            "autheticatorFlow": false,
            "userSetupAllowed": false
          },
          {
            "authenticator": "auth-spnego",
            "authenticatorFlow": false,
            "requirement": "DISABLED",
            "priority": 20,
            "autheticatorFlow": false,
            "userSetupAllowed": false
          },
          {
            "authenticatorConfig": "Redirect to \"oidc\" identity provider",
            "authenticator": "identity-provider-redirector",
            "authenticatorFlow": false,
            "requirement": "ALTERNATIVE",
            "priority": 25,
            "autheticatorFlow": false,
            "userSetupAllowed": false
          },
          {
            "authenticatorFlow": true,
            "requirement": "DISABLED",
            "priority": 30,
            "autheticatorFlow": true,
            "flowAlias": "forms",
            "userSetupAllowed": false
          }
        ]
      }
      ```
      ```json
      {
        "id": "0e031533-a156-4cca-b3c4-b8bbaf876947",
        "alias": "Redirect to \"oidc\" identity provider",
        "config": {
          "defaultProvider": "oidc"
        }
      }
      ```
4. An internal client should be created for sole purpose of generating service account access tokens.
    * An example of client configuration as a screenshot of Keycloak UI:
      * ![](https://github.com/keeleinstituut/tv-sso/blob/github-media/internal-client.jpg)
    * An example of client configuration as JSON:
      ```json
        {
        "id": "d7814189-8585-49bc-9e0c-74e9a12e0644",
        "clientId": "internal-service-account-client",
        "name": "Internal Service Account Client",
        "description": "",
        "rootUrl": "",
        "adminUrl": "",
        "baseUrl": "",
        "surrogateAuthRequired": false,
        "enabled": true,
        "alwaysDisplayInConsole": false,
        "clientAuthenticatorType": "client-secret",
        "secret": "**********",
        "redirectUris": [],
        "webOrigins": [],
        "notBefore": 0,
        "bearerOnly": false,
        "consentRequired": false,
        "standardFlowEnabled": false,
        "implicitFlowEnabled": false,
        "directAccessGrantsEnabled": false,
        "serviceAccountsEnabled": true,
        "publicClient": false,
        "frontchannelLogout": true,
        "protocol": "openid-connect",
        "attributes": {
          "oidc.ciba.grant.enabled": "false",
          "client.secret.creation.time": "1681745199",
          "backchannel.logout.session.required": "true",
          "post.logout.redirect.uris": "",
          "display.on.consent.screen": "false",
          "oauth2.device.authorization.grant.enabled": "false",
          "backchannel.logout.revoke.offline.tokens": "false"
        },
        "authenticationFlowBindingOverrides": {},
        "fullScopeAllowed": true,
        "nodeReRegistrationTimeout": -1,
        "defaultClientScopes": [],
        "optionalClientScopes": []
      }
      ``` 
5. A mapper of type "Query Tõlkevärav API" should attached to the public client and configured such that:
    * The configuration field "Add to access token" is`true`.
    * The configuration field "Internal client ID" corresponds to the client created in step 6.
    * The configuration field "Claims endpoint URL" is set to the endpoint in Tõlkevärav API which returns JWT claims.
    * An example of mapper configuration as a screenshot of Keycloak UI:
      * ![](https://github.com/keeleinstituut/tv-sso/blob/github-media/query-tolkevarav-mapper.jpg)
    * An example of mapper configuration as JSON:
      ```json
      {
        "id": "b2141c39-1f75-4c71-9d70-e10df7ce437b",
        "name": "tolkevarav-dev-claims-mapper",
        "protocol": "openid-connect",
        "protocolMapper": "claims-from-tolkevarav-api-mapper",
        "consentRequired": false,
        "config": {
          "userinfo.token.claim": "true",
          "id.token.claim": "true",
          "access.token.claim": "true",
          "client-id": "internal-service-account-client",
          "claims-endpoint-url": "https://dev.tolkevarav.eki.ee/api/jwt-claims",
          "abort-if-unexpected-error": "true"
        }
      }
      ```

## SSO Usage After Configuration

Assuming Keycloak has been configured as it is expected to be, it should behave as follows:
* JWT provided by the realm have a custom claim under the key `"tolkevarav"`. The value of this claim is a JSON object. This object has the following keys:
  * If the user’s institution is not yet selected, it has the following structure:
    ```json
    {
      "personalIdentificationCode": "",
      "userId": "",
      "forename": "",
      "surname": ""
    }
    ```
  * If the user’s institution has been selected, it has the following structure:
    ```json
    {
      "personalIdentificationCode": "",
      "userId": "",
      "institutionUserId": "",
      "forename": "",
      "surname": "",
      "selectedInstitution": {
        "id": "",
        "name": ""
      },
      "privileges": ["", "", ""]
    }
    ```
* When the client wants to set an institution as selected, the authorization code flow should be performed, in which the final request (to the token endpoint) should contain the header `X-Selected-Institution-ID` with its value being the ID of the institution that the current user wants to select.
  * To authenticate the user without selecting an institution, then perform the authorization code flow without the header mentioned above.



