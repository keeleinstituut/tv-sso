package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.*;
import ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.TestIdentity;
import org.apache.http.message.BasicNameValuePair;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ClaimsAssertions.*;
import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.*;
import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.TestIdentity.*;
import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.Util.buildContainerUriAuthority;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.*;
import static org.testcontainers.utility.DockerImageName.parse;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Testcontainers
class MapperAndPreconfiguredKeycloakIntegrationTests {
    public static final Network CONTAINER_NETWORK = Network.newNetwork();

    @SuppressWarnings({"unused", "resource"})
    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres")
        .withNetwork(CONTAINER_NETWORK)
        .withNetworkAliases("postgres")
        .withDatabaseName("keycloak")
        .withUsername("keycloak")
        .withPassword("secret");

    @Container
    private static final MockServerContainer MOCK_TOLKEVARAV_CONTAINER = new MockServerContainer(parse("mockserver/mockserver:5.15.0"))
        .withNetwork(CONTAINER_NETWORK)
        .withNetworkAliases("mock-tolkevarav")
        .withStartupTimeout(ofSeconds(180));

    @Container
    private static final LocalKeycloakContainer KEYCLOAK_CONTAINER = new LocalKeycloakContainer()
        .withNetwork(CONTAINER_NETWORK)
        .withNetworkAliases("sso")
        .withPostgresHost("postgres")
        .withDatabaseName("keycloak")
        .withDatabaseUser("keycloak")
        .withDatabasePassword("secret");

    private final AuthFlowsPerformer authenticationPerformer = new AuthFlowsPerformer(
        buildContainerUriAuthority(KEYCLOAK_CONTAINER, 8080),
        buildContainerUriAuthority(MOCK_TOLKEVARAV_CONTAINER, 1080)
    );

    @Test
    void givenAuthorizationCodeFlow_tokensShouldContainCustomClaims_whenInstitutionIsSelected()
        throws ParseException, IOException, java.text.ParseException, URISyntaxException, UnsuccessfulResponseException {
        try (var mockTaraContainer = buildMockTaraContainer(IDENTITY_A)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);
            var tokens = authenticationPerformer.performAuthorizationCodeFlow(
                new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, SUCCESSFUL_RESPONSE_INSTITUTION_ID.getValue())
            );

            var accessTokenClaims = JWTParser
                .parse(tokens.getAccessToken().getValue())
                .getJWTClaimsSet()
                .getClaims();
            assertThat(accessTokenClaims).satisfies(containsStandardAndCustomAccessTokenClaims(IDENTITY_A));

            var idTokenClaims = tokens.getIDToken().getJWTClaimsSet().getClaims();
            assertThat(idTokenClaims).satisfies(containsStandardAndCustomIdTokenClaims(IDENTITY_A));

            var refreshedAccessToken = authenticationPerformer.requestAccessTokenWithRefreshToken(tokens.getRefreshToken());
            var refreshedAccessTokenClaims = JWTParser.parse(refreshedAccessToken.getValue())
                .getJWTClaimsSet()
                .getClaims();
            assertThat(refreshedAccessTokenClaims).satisfies(containsStandardAndCustomAccessTokenClaims(IDENTITY_A));
        }
    }

    @Test
    void givenAuthorizationCodeFlow_tokensShouldContainPicAndTvApiNotQueried_whenInstitutionNotSelected()
        throws ParseException, IOException, java.text.ParseException, URISyntaxException, UnsuccessfulResponseException {
        try (var mockTaraContainer = buildMockTaraContainer(IDENTITY_B)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var initialApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();

            var tokens = authenticationPerformer.performAuthorizationCodeFlow();

            var accessTokenClaims = JWTParser
                .parse(tokens.getAccessToken().getValue())
                .getJWTClaimsSet()
                .getClaims();
            assertThat(accessTokenClaims).satisfies(containsStandardAccessTokenClaimsAndPic(IDENTITY_B));

            var idTokenClaims = tokens.getIDToken().getJWTClaimsSet().getClaims();
            assertThat(idTokenClaims).satisfies(containsStandardIdTokenClaimsAndPic(IDENTITY_B));

            var refreshedAccessToken = authenticationPerformer.requestAccessTokenWithRefreshToken(tokens.getRefreshToken());
            var refreshedAccessTokenClaims = JWTParser.parse(refreshedAccessToken.getValue())
                .getJWTClaimsSet()
                .getClaims();
            assertThat(refreshedAccessTokenClaims).satisfies(containsStandardAccessTokenClaimsAndPic(IDENTITY_B));

            var finalApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();
            assertThat(finalApiRequestsCount).isEqualTo(initialApiRequestsCount);
        }
    }

    @Test
    void givenQueryingUserinfoEndpoint_responseShouldContainCustomClaims_whenInstitutionIsSelected()
        throws ParseException, IOException, URISyntaxException, UnsuccessfulResponseException {
        try (var mockTaraContainer = buildMockTaraContainer(IDENTITY_C)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var accessToken = authenticationPerformer
                .performAuthorizationCodeFlow(
                    new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, SUCCESSFUL_RESPONSE_INSTITUTION_ID.getValue()))
                .getAccessToken();

            var userinfoClaims = authenticationPerformer
                .requestUserinfoWithAccessToken(accessToken)
                .toJWTClaimsSet()
                .getClaims();
            assertThat(userinfoClaims).satisfies(containsStandardAndCustomUserinfoClaims(IDENTITY_C));
        }
    }

    @Test
    void givenQueryingUserinfoEndpoint_responseShouldContainPicAndTvApiNotQueried_whenInstitutionNotSelected()
        throws ParseException, IOException, URISyntaxException, UnsuccessfulResponseException {
        try (var mockTaraContainer = buildMockTaraContainer(IDENTITY_D)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var initialApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();

            var accessToken = authenticationPerformer.performAuthorizationCodeFlow().getAccessToken();

            var userinfoClaims = authenticationPerformer.requestUserinfoWithAccessToken(accessToken).toJSONObject();
            assertThat(userinfoClaims).satisfies(containsStandardUserinfoClaimsAndPic(IDENTITY_D));

            var finalApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();
            assertThat(finalApiRequestsCount).isEqualTo(initialApiRequestsCount);
        }
    }

    @Test
    void givenAuthorizationCodeFlow_flowShouldQueryTvApiAndFail_whenInvalidInstitutionIsSelected() throws IOException {
        try (var mockTaraContainer = buildMockTaraContainer(IDENTITY_E)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var initialApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();

            assertThatThrownBy(() -> authenticationPerformer.performAuthorizationCodeFlow(
                    new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, "?")
                ))
                .satisfies(ClaimsAssertions::isUnsuccessfulResponseExceptionWithStatus500);

            var finalApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();
            assertThat(finalApiRequestsCount).isGreaterThan(initialApiRequestsCount);
        }
    }

    @Test
    void givenAuthorizationCodeFlow_flowShouldQueryTvApiAndFail_whenRequestToTvApiTimesout() throws IOException {
        try (var mockTaraContainer = buildMockTaraContainer(IDENTITY_E)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);
            MOCK_TOLKEVARAV_CONTAINER.stop();

            assertThatThrownBy(() -> authenticationPerformer.performAuthorizationCodeFlow(
                    new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, SUCCESSFUL_RESPONSE_INSTITUTION_ID.getValue())
                ))
                .satisfies(ClaimsAssertions::isUnsuccessfulResponseExceptionWithStatus500);
        } finally {
            MOCK_TOLKEVARAV_CONTAINER.start();
            updateMockTolkevaravUriInPerformer(MOCK_TOLKEVARAV_CONTAINER);
        }
    }

    @Test
    void givenAuthorizationCodeFlow_flowShouldQueryTvApiAndFail_whenResponseFromTvApiIsEmpty() throws IOException {
        try (var mockTaraContainer = buildMockTaraContainer(IDENTITY_E)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var initialApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();

            assertThatThrownBy(() -> authenticationPerformer.performAuthorizationCodeFlow(
                    new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, EMPTY_RESPONSE_INSTITUTION_ID.getValue())
                ))
                .satisfies(ClaimsAssertions::isUnsuccessfulResponseExceptionWithStatus500);

            var finalApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();
            assertThat(finalApiRequestsCount).isGreaterThan(initialApiRequestsCount);
        }
    }

    @Test
    void givenAuthorizationCodeFlow_flowShouldFailWithoutQueryingTvApi_whenSelectedInstitutionIsBlank() throws IOException {
        try (var mockTaraContainer = buildMockTaraContainer(IDENTITY_E)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var initialApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();

            assertThatThrownBy(() -> authenticationPerformer.performAuthorizationCodeFlow(
                    new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, "")
                ))
                .satisfies(ClaimsAssertions::isUnsuccessfulResponseExceptionWithStatus500);

            var finalApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();
            assertThat(finalApiRequestsCount).isEqualTo(initialApiRequestsCount);

        }
    }

    private void updateMockTaraUriInPerformer(MockTaraContainer mockTaraContainer) {
        authenticationPerformer.setMockTaraUriAuthority(buildContainerUriAuthority(mockTaraContainer, 8080));
    }

    private void updateMockTolkevaravUriInPerformer(MockServerContainer mockTolkevaravContainer) {
        authenticationPerformer.updateMockingTolkevaravHelper(buildContainerUriAuthority(mockTolkevaravContainer, 1080));
    }

    @SuppressWarnings("resource")
    private MockTaraContainer buildMockTaraContainer(TestIdentity identity) throws JsonProcessingException {
        return new MockTaraContainer()
            .withNetwork(CONTAINER_NETWORK)
            .withNetworkAliases("mock-tara")
            .withEnv("JSON_CONFIG", buildOidcServerMockConfig(identity.getTaraSubjectClaim()));
    }

    private String buildOidcServerMockConfig(String subjectClaim) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(Map.of(
            "interactiveLogin", false,
            "httpServer", "NettyWrapper",
            "tokenCallbacks", List.of(Map.of(
                "issuerId", "default",
                "requestMappings", List.of(Map.of(
                    "requestParam", "client_id",
                    "match", "*",
                    "claims", Map.of("sub", subjectClaim)
                ))
            ))
        ));
    }
}
