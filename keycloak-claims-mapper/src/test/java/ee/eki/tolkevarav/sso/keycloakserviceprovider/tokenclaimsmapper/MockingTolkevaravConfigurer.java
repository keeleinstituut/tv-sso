package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.apache.hc.core5.net.URIAuthority;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.*;
import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.TestIdentity.values;
import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.Util.convertCamelCaseToSnakeCase;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.APPLICATION_JSON;
import static org.mockserver.model.NottableString.not;
import static org.mockserver.model.NottableString.string;
import static org.mockserver.model.Parameter.param;

public class MockingTolkevaravConfigurer {
    public static void configure(URIAuthority mockTolevaravUriAuthority) {
        var configurationClient = new MockServerClient(mockTolevaravUriAuthority.getHostName(), mockTolevaravUriAuthority.getPort());

        for (ExpectedValues.TestIdentity identity : values()) {
            addRuleToMockTolkevaravApiServer(
                configurationClient,
                new Parameters(
                    createIdentityPicParameterMatcher(identity),
                    buildInstitutionIdParameterMatcher(string(INSTITUTION_ID.getValue()))
                ),
                response().withContentType(APPLICATION_JSON).withBody(expectedTolkevaravApiResponseJson(identity))
            );
        }

        addRuleToMockTolkevaravApiServer(
            configurationClient,
            new Parameters(
                createIdentityPicParameterMatcher(ExpectedValues.TestIdentity.values()),
                buildInstitutionIdParameterMatcher(not(INSTITUTION_ID.getValue()))
            ),
            notFoundResponse()
        );
    }

    private static void addRuleToMockTolkevaravApiServer(MockServerClient configurationClient, Parameters parameters, HttpResponse responseToReturn) {
        configurationClient.when(
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

    private static Parameter buildInstitutionIdParameterMatcher(NottableString nottableString) {
        return param(string(convertCamelCaseToSnakeCase(SELECTED_INSTITUTION_ID_KEY)), nottableString);
    }

    private static Parameter createIdentityPicParameterMatcher(TestIdentity... identities) {
        var picRegexMatchingAllIdentities = Stream.of(identities)
            .map(TestIdentity::getPersonalIdentificationCode)
            .collect(Collectors.joining("|"));

        return param(convertCamelCaseToSnakeCase(PERSONAL_IDENTIFICATION_CODE_KEY), picRegexMatchingAllIdentities);
    }

}
