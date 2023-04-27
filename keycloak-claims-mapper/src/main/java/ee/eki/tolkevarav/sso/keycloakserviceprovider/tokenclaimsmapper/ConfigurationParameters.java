package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.utils.StringUtil;

record ConfigurationParameters(String mapperId, String selfAuthenticationClientId, String claimsEndpointURL) {
    static final String CLAIMS_ENDPOINT_URL_KEY = "claims-endpoint-url";
    static final String SELF_AUTHENTICATION_CLIENT_ID_KEY = "client-id";
    static final String ABORT_IF_UNEXPECTED_ERROR_KEY = "abort-if-unexpected-error";

    static ConfigurationParameters fromModel(ProtocolMapperModel model) {
        return new ConfigurationParameters(
            model.getId(),
            model.getConfig().get(SELF_AUTHENTICATION_CLIENT_ID_KEY),
            model.getConfig().get(CLAIMS_ENDPOINT_URL_KEY)
        );
    }

    boolean isInvalid() {
        return StringUtil.isBlank(mapperId)
            || StringUtil.isBlank(selfAuthenticationClientId)
            || StringUtil.isBlank(claimsEndpointURL);
    }
}
