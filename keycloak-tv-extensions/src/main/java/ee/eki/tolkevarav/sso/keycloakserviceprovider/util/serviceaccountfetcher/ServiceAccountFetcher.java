package ee.eki.tolkevarav.sso.keycloakserviceprovider.util.serviceaccountfetcher;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        var httpClient = HttpClient.newHttpClient();

        try {

            String baseUri = this.keycloakSession.getContext().getUri().getBaseUri().toString();
            var tokenEndpoint = new URI("%srealms/%s/protocol/openid-connect/token".formatted(baseUri, realm.getName()));

            logger.info("Requesting token from endpoint: " + tokenEndpoint);

            var auth = client.getClientId() + ":" + client.getSecret();
            var encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            var body = "grant_type=client_credentials";
            var request = HttpRequest
                    .newBuilder(tokenEndpoint)
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .setHeader("Authorization", "Basic " + encodedAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var jsonResponse = JsonSerialization.readValue(response.body(), JsonNode.class);
            String accessTokenJwt = jsonResponse.get("access_token").asText();
            TokenVerifier.create(accessTokenJwt, AccessToken.class).getToken();
            return accessTokenJwt;

        } catch (IOException | InterruptedException | VerificationException | URISyntaxException e) {
            logger.error("Encountered error when fetching access token for service account", e);
            throw new RuntimeException(e);
        }
    }
}
