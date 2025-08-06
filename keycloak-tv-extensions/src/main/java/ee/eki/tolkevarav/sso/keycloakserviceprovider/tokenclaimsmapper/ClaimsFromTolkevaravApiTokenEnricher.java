package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.eki.tolkevarav.sso.keycloakserviceprovider.util.auditlogclient.AuditLogClient;
import ee.eki.tolkevarav.sso.keycloakserviceprovider.util.auditlogclient.AuditLogMessage;
import ee.eki.tolkevarav.sso.keycloakserviceprovider.util.serviceaccountfetcher.ServiceAccountFetcher;
import org.apache.http.client.utils.URIBuilder;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.PersonalIdentificationCodeParser.parseAssumingEePrefix;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

class ClaimsFromTolkevaravApiTokenEnricher {

    private static final String SELECTED_INSTITUTION_ID_HEADER_KEY = "X-Selected-Institution-ID";
    private final ConfigurationParameters configuration;
    private final KeycloakSession keycloakSession;
    private final UserSessionModel userSession;
    private final ServiceAccountFetcher serviceAccountFetcher;

    // private final AuditLogClient auditLogClient;

    private static final Logger logger = Logger.getLogger(ClaimsFromTolkevaravApiTokenEnricher.class);

    ClaimsFromTolkevaravApiTokenEnricher(ProtocolMapperModel mapperModel,
                                                KeycloakSession keycloakSession,
                                                UserSessionModel userSession) {
        this.configuration = ConfigurationParameters.fromModel(mapperModel);
        this.keycloakSession = keycloakSession;
        this.userSession = userSession;
        this.serviceAccountFetcher = new ServiceAccountFetcher(keycloakSession, this.configuration.selfAuthenticationClientId());
        // this.auditLogClient = new AuditLogClient(keycloakSession);
    }

    public void close() {
        // try {
        //     this.auditLogClient.close();
        // } catch (IOException | TimeoutException e) {
        //     logger.error("Encountered error closing AuditLogClient", e);
        // }
    }

    void enrichToken(IDToken token) throws TokenEnrichmentException, URISyntaxException, IOException, InterruptedException {
        if (configuration.isInvalid()) {
            throw new TokenEnrichmentException("Mapper configuration is invalid: " + configuration);
        }

        var claimsFromApi = queryClaimsFromApi(
            retrievePersonalIdentificationCode(),
            retrieveInstitutionId(),
            this.serviceAccountFetcher.getServiceAccountAccessToken()
        );

        logger.infof("Claims received from Tõlkevärav API: %s", claimsFromApi);

        if (claimsFromApi.isEmpty()) {
            token.getOtherClaims().put("tolkevarav", Map.of());
            logger.info("Tõlkevärav claims were set to an empty object.");
        } else if (token.getOtherClaims().get("tolkevarav") instanceof Map<?, ?> existingClaims
                && claimsFromApi.get("userId").equals(existingClaims.get("userId"))) {
            //noinspection unchecked
            ((Map<String, Object>) existingClaims).putAll(claimsFromApi);
            logger.info("Merged claims from API into existing Tõlkevärav claims.");
        } else {
            token.getOtherClaims().put("tolkevarav", claimsFromApi);
            logger.info("Tõlkevärav claims were set to what was received from API.");
        }

        setTolkevaravInfoToUserSessionNotes(claimsFromApi);
    }

    Map<String, Object> queryClaimsFromApi(String personalIdentificationCode, String institutionId, String accessToken)
            throws URISyntaxException, IOException, InterruptedException, TokenEnrichmentException {
        logger.debugf("Querying Tõlkevärav API claims endpoint (%s) for claims with PIC=%s and institutionID=%s",
            configuration.claimsEndpointURL(),
            personalIdentificationCode,
            institutionId
        );

        var uriBuilder = new URIBuilder(configuration.claimsEndpointURL())
            .addParameter("personal_identification_code", personalIdentificationCode);

        if (institutionId != null) {
            uriBuilder.addParameter("institution_id", institutionId);
        }

        var request = HttpRequest
            .newBuilder(uriBuilder.build())
            .GET()
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .build();

        var response = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
            .send(request, ofString());

        if (response.statusCode() == 404) {
            logger.info("Response from API was 404, indicating no such user exists.");
            return Map.of();
        }

        if (response.statusCode() != 200) {
            throw new TokenEnrichmentException("Response status from Tõlkevärav API was neither 200 or 404: " + response);
        }

        if (StringUtil.isBlank(response.body())) {
            throw new TokenEnrichmentException("Response from Tõlkevärav API was status 200, but body blank: " + response);
        }

        return new ObjectMapper().readValue(response.body(), new TypeReference<>() {});
    }

    String retrievePersonalIdentificationCode() throws TokenEnrichmentException {
        return parseAssumingEePrefix(userSession.getUser().getUsername());
    }

    String retrieveInstitutionId() {
        var key = buildInstitutionIdSessionNoteKey();
        var institutionIdFromSession = userSession.getNote(key);
        var institutionIdFromHeaders = keycloakSession.getContext()
            .getRequestHeaders()
            .getHeaderString(SELECTED_INSTITUTION_ID_HEADER_KEY);

        if (StringUtil.isNotBlank(institutionIdFromHeaders)) {
            logger.infof("Reading institution ID from request headers and saving it in session: %s.", institutionIdFromHeaders);
            userSession.setNote(key, institutionIdFromHeaders);
            return institutionIdFromHeaders;
        }

        if (StringUtil.isNotBlank(institutionIdFromSession)) {
            logger.infof("Reading institution ID from user session since it’s not in request headers: %s.", institutionIdFromSession);
            return institutionIdFromSession;
        }

        return null;
    }

    void setTolkevaravInfoToUserSessionNotes(Map<String, Object> claimsFromApi) {
        String selectedInstitutionIdFromNotes = userSession.getNote("TV_SELECTED_INSTITUTION_ID");
        String selectedInstitutionIdFromClaim = ((Map<String, String>) claimsFromApi.getOrDefault("selectedInstitution", new HashMap<>())).get("id");

        boolean isChanged = !Optional.ofNullable(selectedInstitutionIdFromClaim).orElse("").equals(Optional.ofNullable(selectedInstitutionIdFromNotes).orElse(""));

        // if (isChanged && selectedInstitutionIdFromNotes != null) {
        //     var message = new AuditLogMessage()
        //             .fillUserInfo(userSession)
        //             .fillInstitutionInfo(userSession)
        //             .eventType("LOG_OUT");

        //     try {
        //         this.auditLogClient.send(message);
        //     } catch (IOException e) {
        //         logger.error("Encountered error sending messages with AuditLogClient", e);
        //         throw new RuntimeException(e);
        //     }
        // }

        userSession.setNote("TV_USER_PERSONAL_IDENTIFICATION_CODE", ((String) claimsFromApi.get("personalIdentificationCode")));
        userSession.setNote("TV_USER_ID", ((String) claimsFromApi.get("userId")));
        userSession.setNote("TV_USER_FORENAME", ((String) claimsFromApi.get("forename")));
        userSession.setNote("TV_USER_SURNAME", ((String) claimsFromApi.get("surname")));

        userSession.setNote("TV_SELECTED_INSTITUTION_ID", selectedInstitutionIdFromClaim);
        userSession.setNote("TV_SELECTED_INSTITUTION_NAME", ((Map<String, String>) claimsFromApi.getOrDefault("selectedInstitution", new HashMap<>())).get("name"));
        userSession.setNote("TV_INSTITUTION_USER_ID", ((String) claimsFromApi.get("institutionUserId")));
        userSession.setNote("TV_DEPARTMENT_ID", ((Map<String, String>) claimsFromApi.getOrDefault("department", new HashMap<>())).get("id"));
        userSession.setNote("TV_DEPARTMENT_NAME", ((Map<String, String>) claimsFromApi.getOrDefault("department", new HashMap<>())).get("name"));

        // if (isChanged && selectedInstitutionIdFromClaim != null) {
        //     var message = new AuditLogMessage()
        //             .fillUserInfo(userSession)
        //             .fillInstitutionInfo(userSession)
        //             .eventType("LOG_IN");

        //     try {
        //         this.auditLogClient.send(message);
        //     } catch (IOException e) {
        //         logger.error("Encountered error sending messages with AuditLogClient", e);
        //         throw new RuntimeException(e);
        //     }
        // }
    }

    String buildInstitutionIdSessionNoteKey() {
        return ClaimsFromTolkevaravApiTokenEnricher.class.getName()
            + ".SelectedInstitutionId."
            + userSession.getId()
            + "."
            + configuration.mapperId();
    }
}
