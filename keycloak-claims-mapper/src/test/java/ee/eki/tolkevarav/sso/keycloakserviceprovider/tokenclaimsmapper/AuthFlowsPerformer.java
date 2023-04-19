package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.Util.isSameHostName;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.familyOf;

public class AuthFlowsPerformer {
    private static final String KEYCLOAK_PATH_PREFIX = "/realms/tolkevarav-dev/protocol/openid-connect";
    private static final ClientID CLIENT_ID = new ClientID("tolkevarav-web-dev");
    private static final URI IGNORED_REDIRECT_URI = URI.create("https://ignore");
    private final URIAuthority keycloakUriAuthority;

    private URIAuthority mockTaraUriAuthority;

    AuthFlowsPerformer(URIAuthority keycloakUriAuthority, URIAuthority mockTolkevaravUriAuthority) {
        this.keycloakUriAuthority = keycloakUriAuthority;
        MockingTolkevaravConfigurer.configure(mockTolkevaravUriAuthority);
    }

    public void setMockTaraUriAuthority(URIAuthority mockTaraUriAuthority) {
        this.mockTaraUriAuthority = mockTaraUriAuthority;
    }

    OIDCTokens performAuthorizationCodeFlow(NameValuePair... headers)
        throws ParseException, IOException, URISyntaxException {
        var authorizationURL = followRedirectsUntilReachingIgnoredURL(
            createAuthorizationRequestURI()
        );

        var authorizationCode = AuthenticationResponseParser
            .parse(authorizationURL)
            .toSuccessResponse()
            .getAuthorizationCode();

        var grant = new AuthorizationCodeGrant(authorizationCode, IGNORED_REDIRECT_URI);
        return queryTokenEndpointWithGrant(grant, headers).toOIDCTokens();

    }

    AccessToken requestAccessTokenWithRefreshToken(RefreshToken refreshToken) throws ParseException, IOException, URISyntaxException {
        var grant = new RefreshTokenGrant(refreshToken);
        return queryTokenEndpointWithGrant(grant).getAccessToken();
    }

    UserInfo requestUserinfoWithAccessToken(AccessToken accessToken) throws ParseException, IOException, URISyntaxException {
        var httpResponse = new UserInfoRequest(createUserinfoURI(), accessToken)
            .toHTTPRequest()
            .send();

        return UserInfoResponse.parse(httpResponse)
            .toSuccessResponse()
            .getUserInfo();
    }

    private Tokens queryTokenEndpointWithGrant(AuthorizationGrant grant, NameValuePair... headers) throws IOException, ParseException, URISyntaxException {
        var request = new TokenRequest(createTokenURI(), CLIENT_ID, grant)
            .toHTTPRequest();

        Arrays.stream(headers).forEach(
            header -> request.setHeader(header.getName(), header.getValue())
        );

        return OIDCTokenResponseParser
            .parse(request.send())
            .toSuccessResponse()
            .getTokens();
    }

    private URI followResponseRedirect(ClassicHttpResponse response) throws ProtocolException {
        if (!REDIRECTION.equals(familyOf(response.getCode()))) {
            throw new IllegalStateException("Response was not a redirect while was still expecting redirects: " + response);
        }

        return URI.create(response.getHeader("Location").getValue());
    }

    private URI followRedirectsUntilReachingIgnoredURL(URI initialReauestAddress) throws IOException, URISyntaxException {
        try (var client = Util.buildHttpClient()) {
            var currentRedirectURI = client.execute(new HttpGet(initialReauestAddress), this::followResponseRedirect);

            while (!isSameHostName(currentRedirectURI, IGNORED_REDIRECT_URI)) {
                if (isSameHostName(currentRedirectURI, "mock-tara")) {
                    currentRedirectURI = new URIBuilder(currentRedirectURI).setAuthority(mockTaraUriAuthority).build();
                }

                currentRedirectURI = client.execute(new HttpGet(currentRedirectURI), this::followResponseRedirect);
            }

            return currentRedirectURI;
        }
    }

    private URI createAuthorizationRequestURI() throws URISyntaxException {
        var builder = new AuthenticationRequest.Builder(
            new ResponseType("code"),
            new Scope("openid"),
            CLIENT_ID,
            IGNORED_REDIRECT_URI
        );

        return builder
            .endpointURI(createAuthorizationURI())
            .state(new State())
            .nonce(new Nonce())
            .build()
            .toURI();
    }

    URI createAuthorizationURI() throws URISyntaxException {
        return buildKeycloakEndpointURI("auth");
    }

    private URI createTokenURI() throws URISyntaxException {
        return buildKeycloakEndpointURI("token");
    }

    private URI createUserinfoURI() throws URISyntaxException {
        return buildKeycloakEndpointURI("userinfo");
    }

    private URI buildKeycloakEndpointURI(String endpointSuffix) throws URISyntaxException {
        return new URIBuilder()
            .setScheme("http")
            .setAuthority(keycloakUriAuthority)
            .appendPath(KEYCLOAK_PATH_PREFIX)
            .appendPath(endpointSuffix)
            .build();
    }
}
