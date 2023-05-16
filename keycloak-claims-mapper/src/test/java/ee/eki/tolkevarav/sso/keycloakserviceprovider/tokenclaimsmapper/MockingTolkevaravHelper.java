package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.apache.hc.core5.net.URIAuthority;
import org.mockserver.client.MockServerClient;
import org.mockserver.configuration.ClientConfiguration;
import org.mockserver.model.*;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.*;
import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.TestIdentity.values;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.APPLICATION_JSON;
import static org.mockserver.model.NottableString.string;
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
                parameters(
                        buildInstitutionIdParameterMatcher(string(EMPTY_RESPONSE_INSTITUTION_ID.getValue()))
                ),
                response()
                        .withContentType(APPLICATION_JSON)
                        .withBody("")
                        .withDelay(SECONDS, 10)
        );

        for (ExpectedValues.TestIdentity identity : values()) {
            addRuleToMockTolkevaravApiServer(
                parameters(
                    createIdentityPicParameterMatcher(identity),
                    buildInstitutionIdParameterMatcher(string(SUCCESSFUL_RESPONSE_INSTITUTION_ID.getValue()))
                ),
                response()
                    .withContentType(APPLICATION_JSON)
                    .withBody(expectedTolkevaravApiResponseJson(identity))
            );
        }

        for (ExpectedValues.TestIdentity identity : values()) {
            addRuleToMockTolkevaravApiServer(
                    parameters(
                            createIdentityPicParameterMatcher(identity)
                    ),
                    response()
                            .withContentType(APPLICATION_JSON)
                            .withBody(expectedTolkevaravApiResponseJson(identity))
            );
        }

        addRuleToMockTolkevaravApiServer(parameters(), notFoundResponse());
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

    private Parameter buildInstitutionIdParameterMatcher(NottableString nottableString) {
        return param(string("institution_id"), nottableString);
    }

    private Parameter createIdentityPicParameterMatcher(TestIdentity identity) {
        return param("personal_identification_code", identity.getPersonalIdentificationCode());
    }
}
