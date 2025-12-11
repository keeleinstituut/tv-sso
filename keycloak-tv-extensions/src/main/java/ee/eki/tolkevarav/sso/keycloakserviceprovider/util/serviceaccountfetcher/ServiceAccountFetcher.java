package ee.eki.tolkevarav.sso.keycloakserviceprovider.util.serviceaccountfetcher;

import org.jboss.logging.Logger;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;

import java.net.URI;
import java.util.Set;

public class ServiceAccountFetcher {

    private static final Logger logger = Logger.getLogger(ServiceAccountFetcher.class);

    private final KeycloakSession keycloakSession;
    private final String clientId;

    public ServiceAccountFetcher(KeycloakSession keycloakSession, String clientId) {
        this.keycloakSession = keycloakSession;
        this.clientId = clientId;
    }

    public String getServiceAccountAccessToken() throws RuntimeException {
        var realm = this.keycloakSession.getContext().getRealm();
        var client = this.keycloakSession.clients().getClientByClientId(realm, this.clientId);
        
        if (client == null) {
            throw new RuntimeException("Client not found: " + clientId);
        }

        try {
            // Get the service account user for this client (Keycloak 26 API)
            UserModel serviceAccountUser = this.keycloakSession.users().getServiceAccount(client);
            if (serviceAccountUser == null) {
                throw new RuntimeException("Service account user not found for client: " + clientId);
            }

            // Create access token using Keycloak's internal APIs
            AccessToken token = new AccessToken();
            token.audience(client.getClientId());
            token.subject(serviceAccountUser.getId());
            token.issuedFor(client.getClientId());
            token.type("Bearer");
            
            URI baseUri = this.keycloakSession.getContext().getUri().getBaseUri();
            token.issuer(Urls.realmIssuer(baseUri, realm.getName()));
            token.issuedNow();
            token.exp((long) (token.getIat() + 600)); // Token valid for 10 minutes

            // Add client roles to the token
            Set<String> clientRoles = this.keycloakSession.roles().getClientRolesStream(client)
                .map(role -> role.getName())
                .collect(java.util.stream.Collectors.toSet());
            AccessToken.Access realmAccess = new AccessToken.Access();
            for (String role : clientRoles) {
                realmAccess.addRole(role);
            }
            token.setRealmAccess(realmAccess);

            // Sign the token using Keycloak's internal signing mechanism
            var activeRsaKey = this.keycloakSession.keys().getActiveRsaKey(realm);
            String signedToken = new JWSBuilder()
                .kid(activeRsaKey.getKid())
                .type("JWT")
                .jsonContent(token)
                .rsa256(activeRsaKey.getPrivateKey());

            return signedToken;

        } catch (Exception e) {
            logger.error("Encountered error when generating access token for service account", e);
            throw new RuntimeException(e);
        }
    }
}
