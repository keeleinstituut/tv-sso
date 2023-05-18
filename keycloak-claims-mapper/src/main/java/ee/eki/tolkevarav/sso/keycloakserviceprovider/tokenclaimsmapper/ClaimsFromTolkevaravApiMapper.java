package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import com.google.auto.service.AutoService;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.IDToken;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.keycloak.provider.ProviderConfigProperty.BOOLEAN_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;



@AutoService(ProtocolMapper.class)
public class ClaimsFromTolkevaravApiMapper extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final Logger logger = Logger.getLogger(ClaimsFromTolkevaravApiMapper.class);

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Query Tõlkevärav API";
    }

    @Override
    public String getHelpText() {
        return "Queries user’s claims from Tõlkevärav’s API and includes them in ID tokens, access tokens and userinfo.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        var configProperties = ProviderConfigurationBuilder.create()
            .property()
                .name(ConfigurationParameters.CLAIMS_ENDPOINT_URL_KEY)
                .type(STRING_TYPE)
                .label("Claims endpoint URL")
                .helpText("URL of endpoint in Tõlkevärav API that is queried for custom claims to be inserted under `tolkevarav` key")
                .add()
            .property()
                .name(ConfigurationParameters.SELF_AUTHENTICATION_CLIENT_ID_KEY)
                .type(STRING_TYPE)
                .label("Internal client ID")
                .helpText("ID of Keycloak client used for authenticating self in requests to Tõlkevärav’s API.")
                .add()
            .property()
                .name(ConfigurationParameters.ABORT_IF_UNEXPECTED_ERROR_KEY)
                .type(BOOLEAN_TYPE)
                .label("Abort request processing on unexpected errors?")
                .helpText("If enabled, aborts request processing when claims failed to be retrieved from Tõlkevärav’s API, " +
                    "given that the required header was present in the incoming request.")
                .defaultValue("true")
                .add()
            .build();

        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, ClaimsFromTolkevaravApiMapper.class);

        return configProperties;
    }

    @Override
    public String getId() {
        return "claims-from-tolkevarav-api-mapper";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mapperModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionContext) {
        try {
            new ClaimsFromTolkevaravApiTokenEnricher(
                mapperModel,
                keycloakSession,
                userSession
            ).enrichToken(token);
        } catch (TokenEnrichmentException | URISyntaxException | IOException exception) {
            logger.error("Cancelled enriching of token because of an unacceptable/unexpected scenario.", exception);

            if (isAbortRequiredOnUnexpectedErrors(mapperModel)) {
                logger.fatal("Aborting request processing since mapper is configured to abort on unexpected errors.", exception);
                throw new FatalException(exception);
            }
        } catch (InterruptedException interruptedException) {
            logger.fatal("Cancelled enriching of token because of interruption.", interruptedException);
            Thread.currentThread().interrupt();
        }
    }

    private boolean isAbortRequiredOnUnexpectedErrors(ProtocolMapperModel mapperModel) {
        return Boolean.parseBoolean(mapperModel.getConfig().get(ConfigurationParameters.ABORT_IF_UNEXPECTED_ERROR_KEY));
    }
}
