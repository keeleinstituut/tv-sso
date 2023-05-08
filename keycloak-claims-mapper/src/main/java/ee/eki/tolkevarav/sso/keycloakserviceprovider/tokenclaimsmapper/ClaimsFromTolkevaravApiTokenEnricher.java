package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.apache.http.client.utils.URIBuilder;
import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.IDToken;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.PersonalIdentificationCodeParser.parseAssumingEePrefix;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

class ClaimsFromTolkevaravApiTokenEnricher {

    private static final String SELECTED_INSTITUTION_ID_HEADER_KEY = "X-Selected-Institution-ID";
    private final ConfigurationParameters configuration;
    private final KeycloakSession keycloakSession;
    private final UserSessionModel userSession;

    private static final Logger logger = Logger.getLogger(ClaimsFromTolkevaravApiTokenEnricher.class);

    ClaimsFromTolkevaravApiTokenEnricher(ProtocolMapperModel mapperModel,
                                                KeycloakSession keycloakSession,
                                                UserSessionModel userSession) {
        this.configuration = ConfigurationParameters.fromModel(mapperModel);
        this.keycloakSession = keycloakSession;
        this.userSession = userSession;
    }

    void enrichToken(IDToken token) throws AcceptableMapperException, UnacceptableMapperException, URISyntaxException, IOException, InterruptedException {
        if (configuration.isInvalid()) {
            throw new UnacceptableMapperException("Mapper configuration is invalid: " + configuration);
        }

        var personalIdentificationCode = retrievePersonalIdentificationCode();
        token.getOtherClaims().put(
            "tolkevarav",
            Map.of("personalIdentificationCode", personalIdentificationCode)
        );

        var claims = queryClaimsFromApi(
            personalIdentificationCode,
            retrieveInstitutionId(),
            generateAccessToken()
        );

        logger.infof("Claims received from Tõlkevärav API: %s. Adding it to claims in token.", claims);
        token.getOtherClaims().put("tolkevarav", claims);
    }

    String queryClaimsFromApi(String personalIdentificationCode, String institutionId, String accessToken)
            throws URISyntaxException, IOException, InterruptedException, UnacceptableMapperException {
        logger.debugf("Querying Tõlkevärav API claims endpoint (%s) for claims with PIC=%s and institutionID=%s",
            configuration.claimsEndpointURL(),
            personalIdentificationCode,
            institutionId
        );

        var uri = new URIBuilder(configuration.claimsEndpointURL())
            .addParameter("personal_identification_code", personalIdentificationCode)
            .addParameter("selected_institution_id", institutionId)
            .build();

        var request = HttpRequest
            .newBuilder(uri)
            .GET()
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .build();

        var response = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
            .send(request, ofString());

        if (response.statusCode() != 200) {
            throw new UnacceptableMapperException("Response from Tõlkevärav API was not status 200: " + response);
        }

        if (StringUtil.isBlank(response.body())) {
            throw new UnacceptableMapperException("Response from Tõlkevärav API was blank: " + response);
        }

        return response.body();
    }

    String retrievePersonalIdentificationCode() throws UnacceptableMapperException {
        return parseAssumingEePrefix(userSession.getUser().getUsername());
    }

    String retrieveInstitutionId() throws AcceptableMapperException, UnacceptableMapperException {
        var key = buildInstitutionIdSessionNoteKey();

        var institutionIdFromSession = userSession.getNote(key);
        var institutionIdFromHeaders = keycloakSession.getContext()
            .getRequestHeaders()
            .getHeaderString(SELECTED_INSTITUTION_ID_HEADER_KEY);

        if (userSession.getNotes().containsKey(key) && StringUtil.isBlank(institutionIdFromSession)) {
            throw new UnacceptableMapperException("Institution ID was present in the session, but was blank.");
        }

        if (keycloakSession.getContext().getRequestHeaders().getRequestHeaders().containsKey(SELECTED_INSTITUTION_ID_HEADER_KEY)
            && StringUtil.isBlank(institutionIdFromHeaders)) {
            throw new UnacceptableMapperException("Institution ID was present in the header, but was blank.");
        }


        if (!keycloakSession.getContext().getRequestHeaders().getRequestHeaders().containsKey(SELECTED_INSTITUTION_ID_HEADER_KEY)
            && !userSession.getNotes().containsKey(key)) {
            throw new AcceptableMapperException("Institution ID is missing in both session and request headers.");
        }

        if (StringUtil.isBlank(institutionIdFromHeaders)) {
            logger.infof("Reading institution ID from user session since it’s not in request headers: %s.", institutionIdFromSession);
            return institutionIdFromSession;
        } else {
            logger.infof("Reading institution ID from request headers and saving it in session: %s.", institutionIdFromHeaders);
            userSession.setNote(key, institutionIdFromHeaders);
            return institutionIdFromHeaders;
        }
    }

    String buildInstitutionIdSessionNoteKey() {
        return ClaimsFromTolkevaravApiTokenEnricher.class.getName()
            + ".SelectedInstitutionId."
            + userSession.getId()
            + "."
            + configuration.mapperId();
    }

    String generateAccessToken() {
        var realm = keycloakSession.getContext().getRealm();
        var selfAuthenticationClient = keycloakSession.clients().getClientByClientId(realm, configuration.selfAuthenticationClientId());
        var authSession = new AuthenticationSessionManager(keycloakSession)
            .createAuthenticationSession(realm, false)
            .createAuthenticationSession(selfAuthenticationClient);
        var connection = keycloakSession.getContext().getConnection();
        var serviceAccount = keycloakSession.users().getServiceAccount(selfAuthenticationClient);
        var eventBuilder = new EventBuilder(realm, keycloakSession, connection);
        var serviceAccountSession = keycloakSession.sessions().createUserSession(
            realm,
            serviceAccount,
            serviceAccount.getUsername(),
            null,
            null,
            false,
            null,
            null
        );
        var selfAuthenticationClientSessionContext = TokenManager.attachAuthenticationSession(
            keycloakSession, serviceAccountSession, authSession
        );

        return new TokenManager()
            .responseBuilder(
                realm,
                selfAuthenticationClient,
                eventBuilder,
                keycloakSession,
                serviceAccountSession,
                selfAuthenticationClientSessionContext)
            .generateAccessToken()
            .build()
            .getToken();
    }
}
