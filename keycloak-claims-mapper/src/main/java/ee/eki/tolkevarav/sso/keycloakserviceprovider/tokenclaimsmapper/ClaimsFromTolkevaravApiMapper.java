package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import com.google.auto.service.AutoService;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.List;

import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;



@AutoService(ProtocolMapper.class)
public class ClaimsFromTolkevaravApiMapper extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Tõlkevärav claims mapper";
    }

    @Override
    public String getHelpText() {
        return "Queries user’s claims from Tõlkevärav’s API and includes them in ID tokens, access tokens and userinfo.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
            .property()
                .name(ConfigurationParameters.CLAIMS_ENDPOINT_URL_KEY)
                .type(STRING_TYPE)
                .label("Claims endpoint URL")
                .add()
            .property()
                .name(ConfigurationParameters.SELF_AUTHENTICATION_CLIENT_ID_KEY)
                .type(STRING_TYPE)
                .label("ID of Keycloak client used for authenticating self in requests to Tõlkevärav’s API.")
                .add()
            .build();
    }

    @Override
    public String getId() {
        return "claims-from-tolkevarav-api-mapper";
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token,
                                              ProtocolMapperModel mappingModel,
                                              KeycloakSession keycloakSession,
                                              UserSessionModel userSession,
                                              ClientSessionContext clientSessionContext) {
        return transformAccessToken(token, mappingModel, keycloakSession, userSession, clientSessionContext);
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token,
                                            ProtocolMapperModel mappingModel,
                                            KeycloakSession keycloakSession,
                                            UserSessionModel userSession,
                                            ClientSessionContext clientSessionContext) {
        new ClaimsFromTolkevaravApiTokenEnricher(
            mappingModel,
            keycloakSession,
            userSession,
            clientSessionContext
        ).enrichToken(token);

        return token;
    }

    @Override
    public IDToken transformIDToken(IDToken token,
                                    ProtocolMapperModel mappingModel,
                                    KeycloakSession keycloakSession,
                                    UserSessionModel userSession,
                                    ClientSessionContext clientSessionContext) {
        new ClaimsFromTolkevaravApiTokenEnricher(
            mappingModel,
            keycloakSession,
            userSession,
            clientSessionContext
        ).enrichToken(token);

        return token;
    }
}
