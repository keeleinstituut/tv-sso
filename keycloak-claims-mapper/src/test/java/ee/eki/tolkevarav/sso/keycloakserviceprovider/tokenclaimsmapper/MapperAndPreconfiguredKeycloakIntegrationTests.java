package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.*;
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
        .withStartupTimeout(ofSeconds(300));

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
    void tolkevaravClaimsShouldContainExpectedInstitutionUserData_whenInstitutionIsSelected()
        throws ParseException, IOException, java.text.ParseException, URISyntaxException, UnsuccessfulResponseException {
        try (var mockTaraContainer = buildMockTaraContainer(SUCCESSFUL_INSTITUTION_USER_RESPONSE_IDENTITY)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);
            var tokens = authenticationPerformer.performAuthorizationCodeFlow(
                new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, EXAMPLE_INSTITUTION_ID.getValue())
            );

            var accessTokenClaims = JWTParser
                .parse(tokens.getAccessToken().getValue())
                .getJWTClaimsSet()
                .getClaims();
            assertThat(accessTokenClaims).satisfies(containsStandardAccessTokenClaimsAndInstitutionUserData(SUCCESSFUL_INSTITUTION_USER_RESPONSE_IDENTITY));

            var idTokenClaims = tokens.getIDToken().getJWTClaimsSet().getClaims();
            assertThat(idTokenClaims).satisfies(containsStandardIdTokenClaimsAndInstitutionUserData(SUCCESSFUL_INSTITUTION_USER_RESPONSE_IDENTITY));

            var refreshedAccessToken = authenticationPerformer.requestAccessTokenWithRefreshToken(tokens.getRefreshToken());
            var refreshedAccessTokenClaims = JWTParser.parse(refreshedAccessToken.getValue())
                .getJWTClaimsSet()
                .getClaims();
            assertThat(refreshedAccessTokenClaims).satisfies(containsStandardAccessTokenClaimsAndInstitutionUserData(SUCCESSFUL_INSTITUTION_USER_RESPONSE_IDENTITY));

            var userinfoClaims = authenticationPerformer.requestUserinfoWithAccessToken(tokens.getAccessToken()).toJSONObject();
            assertThat(userinfoClaims).satisfies(containsStandardUserinfoClaimsAndInstitutionUserData(SUCCESSFUL_INSTITUTION_USER_RESPONSE_IDENTITY));
        }
    }

    @Test
    void tolkevaravClaimsShouldContainExpectedUserData_whenInstitutionNotSelected()
        throws ParseException, IOException, java.text.ParseException, URISyntaxException, UnsuccessfulResponseException {
        try (var mockTaraContainer = buildMockTaraContainer(SUCCESSFUL_USER_RESPONSE_IDENTITY)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var tokens = authenticationPerformer.performAuthorizationCodeFlow();

            var accessTokenClaims = JWTParser
                .parse(tokens.getAccessToken().getValue())
                .getJWTClaimsSet()
                .getClaims();
            assertThat(accessTokenClaims).satisfies(containsStandardAccessTokenClaimsAndUserData(SUCCESSFUL_USER_RESPONSE_IDENTITY));

            var idTokenClaims = tokens.getIDToken().getJWTClaimsSet().getClaims();
            assertThat(idTokenClaims).satisfies(containsStandardIdTokenClaimsAndUserData(SUCCESSFUL_USER_RESPONSE_IDENTITY));

            var refreshedAccessToken = authenticationPerformer.requestAccessTokenWithRefreshToken(tokens.getRefreshToken());
            var refreshedAccessTokenClaims = JWTParser.parse(refreshedAccessToken.getValue())
                .getJWTClaimsSet()
                .getClaims();
            assertThat(refreshedAccessTokenClaims).satisfies(containsStandardAccessTokenClaimsAndUserData(SUCCESSFUL_USER_RESPONSE_IDENTITY));

            var userinfoClaims = authenticationPerformer.requestUserinfoWithAccessToken(tokens.getAccessToken()).toJSONObject();
            assertThat(userinfoClaims).satisfies(containsStandardUserinfoClaimsAndUserData(SUCCESSFUL_USER_RESPONSE_IDENTITY));
        }
    }

    @Test
    void tolkevaravClaimsShouldBeEmpty_whenResponseFromTvApiIs404()
            throws IOException, UnsuccessfulResponseException, ParseException, URISyntaxException, java.text.ParseException {
        try (var mockTaraContainer = buildMockTaraContainer(NOT_FOUND_RESPONSE_IDENTITY)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var tokens = authenticationPerformer.performAuthorizationCodeFlow();

            var accessTokenClaims = JWTParser
                    .parse(tokens.getAccessToken().getValue())
                    .getJWTClaimsSet()
                    .getClaims();
            assertThat(accessTokenClaims).satisfies(containsStandardAccessTokenClaimsAndEmptyTolkevaravClaims());

            var idTokenClaims = tokens.getIDToken().getJWTClaimsSet().getClaims();
            assertThat(idTokenClaims).satisfies(containsStandardIdTokenClaimsAndEmptyTolkevaravClaims());

            var refreshedAccessToken = authenticationPerformer.requestAccessTokenWithRefreshToken(tokens.getRefreshToken());
            var refreshedAccessTokenClaims = JWTParser.parse(refreshedAccessToken.getValue())
                    .getJWTClaimsSet()
                    .getClaims();
            assertThat(refreshedAccessTokenClaims).satisfies(containsStandardAccessTokenClaimsAndEmptyTolkevaravClaims());

            var userinfoClaims = authenticationPerformer.requestUserinfoWithAccessToken(tokens.getAccessToken()).toJSONObject();
            assertThat(userinfoClaims).satisfies(containsStandardUserinfoClaimsAndEmptyTolkevaravClaims());
        }
    }

    @Test
    void mapperShouldFail_whenApiResponseIs403() throws IOException {
        try (var mockTaraContainer = buildMockTaraContainer(FORBIDDEN_RESPONSE_IDENTITY)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var initialApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();

            assertThatThrownBy(() -> authenticationPerformer.performAuthorizationCodeFlow(
                    new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, EXAMPLE_INSTITUTION_ID.getValue())
                ))
                .satisfies(ClaimsAssertions::isUnsuccessfulResponseExceptionWithStatus500);

            var finalApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();
            assertThat(finalApiRequestsCount).isGreaterThan(initialApiRequestsCount);
        }
    }

    @Test
    void mapperShouldFail_whenRequestToTvApiTimesout() throws IOException {
        try (var mockTaraContainer = buildMockTaraContainer(DELAYED_RESPONSE_IDENTITY)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);
            MOCK_TOLKEVARAV_CONTAINER.stop();

            assertThatThrownBy(() -> authenticationPerformer.performAuthorizationCodeFlow(
                    new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, EXAMPLE_INSTITUTION_ID.getValue())
                ))
                .satisfies(ClaimsAssertions::isUnsuccessfulResponseExceptionWithStatus500);
        } finally {
            MOCK_TOLKEVARAV_CONTAINER.start();
            updateMockTolkevaravUriInPerformer(MOCK_TOLKEVARAV_CONTAINER);
        }
    }

    @Test
    void mapperShouldFail_whenResponseFromTvApiIsEmpty() throws IOException {
        try (var mockTaraContainer = buildMockTaraContainer(EMPTY_RESPONSE_IDENTITY)) {
            mockTaraContainer.start();
            updateMockTaraUriInPerformer(mockTaraContainer);

            var initialApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();

            assertThatThrownBy(() -> authenticationPerformer.performAuthorizationCodeFlow(
                    new BasicNameValuePair(SELECTED_INSTITUTION_ID_HEADER_KEY, EXAMPLE_INSTITUTION_ID.getValue())
                ))
                .satisfies(ClaimsAssertions::isUnsuccessfulResponseExceptionWithStatus500);

            var finalApiRequestsCount = authenticationPerformer.retrieveMockTvApiRequestsCount();
            assertThat(finalApiRequestsCount).isGreaterThan(initialApiRequestsCount);
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
