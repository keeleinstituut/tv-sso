package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.apache.hc.core5.net.URIAuthority;
import org.mockserver.client.MockServerClient;
import org.mockserver.configuration.ClientConfiguration;
import org.mockserver.model.*;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.*;
import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.TestIdentity.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.FORBIDDEN_403;
import static org.mockserver.model.MediaType.APPLICATION_JSON;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.Parameters.parameters;

public class MockingTolkevaravHelper {
    private final MockServerClient client;

    public MockingTolkevaravHelper(URIAuthority mockTolevaravUriAuthority) {
        this.client = new MockServerClient(
            ClientConfiguration.clientConfiguration().maxSocketTimeoutInMillis((long) (60 * 1000)),
            mockTolevaravUriAuthority.getHostName(),
            mockTolevaravUriAuthority.getPort()
        );

        configure();
    }

    public void configure() {
        addRuleToMockTolkevaravApiServer(
            parameters(createIdentityPicParameterMatcher(SUCCESSFUL_INSTITUTION_USER_RESPONSE_IDENTITY)),
            response(expectedTolkevaravApiInstitutionUserResponseJson(SUCCESSFUL_INSTITUTION_USER_RESPONSE_IDENTITY))
                    .withContentType(APPLICATION_JSON)
        );

        addRuleToMockTolkevaravApiServer(
                parameters(createIdentityPicParameterMatcher(SUCCESSFUL_USER_RESPONSE_IDENTITY)),
                response(expectedTolkevaravApiUserResponseJson(SUCCESSFUL_USER_RESPONSE_IDENTITY))
                        .withContentType(APPLICATION_JSON)
        );

        addRuleToMockTolkevaravApiServer(
                parameters(createIdentityPicParameterMatcher(DELAYED_RESPONSE_IDENTITY)),
                response(expectedTolkevaravApiInstitutionUserResponseJson(SUCCESSFUL_INSTITUTION_USER_RESPONSE_IDENTITY))
                        .withContentType(APPLICATION_JSON)
                        .withDelay(SECONDS, 10)
        );

        addRuleToMockTolkevaravApiServer(
                parameters(createIdentityPicParameterMatcher(FORBIDDEN_RESPONSE_IDENTITY)),
                response().withStatusCode(FORBIDDEN_403.code())
        );

        addRuleToMockTolkevaravApiServer(
                parameters(createIdentityPicParameterMatcher(EMPTY_RESPONSE_IDENTITY)),
                response("").withContentType(APPLICATION_JSON)
        );

        addRuleToMockTolkevaravApiServer(
                parameters(createIdentityPicParameterMatcher(NOT_FOUND_RESPONSE_IDENTITY)),
                notFoundResponse()
        );
    }

    public int retrieveReceivedRequestsCount() {
        return client.retrieveRecordedRequests(null).length;
    }

    private void addRuleToMockTolkevaravApiServer(Parameters parameters, HttpResponse responseToReturn) {
        client.when(
                request()
                    .withMethod("GET")
                    .withPath("/api/jwt-claims")
                    .withHeaders(
                        header("Authorization", "Bearer\\s[^\\s]+"),
                        header("Accept", "application/json")
                    )
                    .withQueryStringParameters(parameters.withKeyMatchStyle(KeyMatchStyle.MATCHING_KEY))
            )
            .respond(responseToReturn);
    }

    private Parameter createIdentityPicParameterMatcher(TestIdentity identity) {
        return param("personal_identification_code", identity.getPersonalIdentificationCode());
    }
}
