package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.apache.http.client.utils.URIBuilder;
import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

class ClaimsFromTolkevaravApiTokenEnricher {

    private static final String SELECTED_INSTITUTION_ID_HEADER_KEY = "X-Selected-Institution-ID";
    private static final String TARA_SUBJECT_CLAIM_ATTRIBUTE_KEY = "tara_subject_claim";
    private final ConfigurationParameters configuration;
    private final KeycloakSession keycloakSession;
    private final UserSessionModel userSession;
    private final ClientSessionContext clientSessionContext;

    private static final Logger logger = Logger.getLogger(ClaimsFromTolkevaravApiTokenEnricher.class);

    ClaimsFromTolkevaravApiTokenEnricher(ProtocolMapperModel mapperModel,
                                                KeycloakSession keycloakSession,
                                                UserSessionModel userSession,
                                                ClientSessionContext clientSessionContext) {
        this.configuration = ConfigurationParameters.fromModel(mapperModel);
        this.keycloakSession = keycloakSession;
        this.userSession = userSession;
        this.clientSessionContext = clientSessionContext;
    }

    void enrichToken(IDToken token) {
        try {
            if (configuration.isInvalid()) {
                throw new AcceptableMapperException("Mapper configuration is invalid: " + configuration);
            }

            var claims = queryClaimsFromApi(
                retrievePersonalIdentificationCode(),
                retrieveInstitutionId(),
                generateAccessToken()
            );

            logger.infof("Claims received from Tõlkevärav API: %s. Adding it to claims in token.", claims);
            token.getOtherClaims().put("tolkevarav", claims);
        } catch (AcceptableMapperException exception) {
            logger.info("Cancelled enriching of token because of an acceptable scenario: %s.", exception);
        } catch (UnacceptableMapperException | URISyntaxException | IOException exception) {
            logger.error("Cancelled enriching of token because of an unacceptable/unexpected scenario.", exception);
        } catch (InterruptedException interruptedException) {
            logger.fatal("Cancelled enriching of token because of interruption.", interruptedException);
            Thread.currentThread().interrupt();
        }
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

    String retrievePersonalIdentificationCode() throws UnacceptableMapperException, AcceptableMapperException {
        var taraSubjectClaim = userSession.getUser().getFirstAttribute(TARA_SUBJECT_CLAIM_ATTRIBUTE_KEY);

        if (userSession.getUser().getAttributes().containsKey(TARA_SUBJECT_CLAIM_ATTRIBUTE_KEY)
            && (StringUtil.isBlank(taraSubjectClaim) || !taraSubjectClaim.matches("^[eE]{2}\\d{11}"))) {
            throw new UnacceptableMapperException("User has an attribute with the key 'tara_subject_claim', but it’s either blank or not in the expected format: " + taraSubjectClaim);
        }

        if (!userSession.getUser().getAttributes().containsKey(TARA_SUBJECT_CLAIM_ATTRIBUTE_KEY)) {
            throw new AcceptableMapperException("User does not have an attribute with the key 'tara_subject_claim'.");
        }

        logger.infof("Retrieved the 'tara_subject_claim' attribute from user session: %s", taraSubjectClaim);
        return taraSubjectClaim.substring(2);
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

        return new TokenManager()
            .responseBuilder(
                realm,
                selfAuthenticationClient,
                eventBuilder,
                keycloakSession,
                serviceAccountSession,
                clientSessionContext)
            .generateAccessToken()
            .build()
            .getToken();
    }
}
