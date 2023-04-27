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

import java.util.List;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.TaraParser.TARA_SUBJECT_CLAIM_ATTRIBUTE_KEY;
import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.TaraParser.parsePicFromTaraSubClaim;
import static org.keycloak.provider.ProviderConfigProperty.BOOLEAN_TYPE;

@AutoService(ProtocolMapper.class)
public class TaraSubjectAttributeToPicClaimMapper extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final Logger logger = Logger.getLogger(TaraSubjectAttributeToPicClaimMapper.class);

    @Override
    public String getDisplayCategory() {
        return "User attribute";
    }

    @Override
    public String getDisplayType() {
        return "PIC from Tara attribute";
    }

    @Override
    public String getHelpText() {
        return "Parses a personal identification code form the current Keycloak user’s attribute `tara_subject_claim` and uses it to set the value of a token claim (defaults to `tolkevarav.personal_identification_code` as nested JSON).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        var configProperties = ProviderConfigurationBuilder.create()
            .property()
                .name(ConfigurationParameters.ABORT_IF_UNEXPECTED_ERROR_KEY)
                .type(BOOLEAN_TYPE)
                .label("Abort request processing on unexpected errors?")
                .helpText("If enabled, aborts request processing when claims failed to be retrieved from Tõlkevärav’s API, " +
                    "given that the required header was present in the incoming request.")
                .defaultValue("true")
                .add()
            .build();

        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, TaraSubjectAttributeToPicClaimMapper.class);

        configProperties.stream()
            .filter(property -> OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME.equals(property.getName()))
            .findFirst()
            .orElseThrow()
            .setDefaultValue("tolkevarav.personal_identification_code");

        return configProperties;
    }

    @Override
    public String getId() {
        return "tara-sub-attribute-to-pic-claim-mapper";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mapperModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionContext) {
        try {
            var taraSubjectClaim = userSession.getUser().getFirstAttribute(TARA_SUBJECT_CLAIM_ATTRIBUTE_KEY);
            logger.infof("Retrieved the 'tara_subject_claim' attribute from user session: %s", taraSubjectClaim);

            String personalIdentificationCode = parsePicFromTaraSubClaim(taraSubjectClaim);
            OIDCAttributeMapperHelper.mapClaim(token, mapperModel, personalIdentificationCode);

            logger.infof(
                "Triggered setting claim '%s' to value '%s' using OIDCAttributeMapperHelper::mapClaim",
                mapperModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME),
                personalIdentificationCode
            );
        } catch (UnacceptableMapperException exception) {
            logger.error("Cancelled enriching of token because of an unacceptable/unexpected scenario.", exception);

            if (isAbortRequiredOnUnexpectedErrors(mapperModel)) {
                logger.fatal("Aborting request processing since mapper is configured to abort on unexpected errors.", exception);
                throw new FatalException(exception);
            }
        }

    }

    private boolean isAbortRequiredOnUnexpectedErrors(ProtocolMapperModel mapperModel) {
        return Boolean.parseBoolean(mapperModel.getConfig().get(ConfigurationParameters.ABORT_IF_UNEXPECTED_ERROR_KEY));
    }
}
