package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.keycloak.utils.StringUtil;

public class PersonalIdentificationCodeParser {
    private PersonalIdentificationCodeParser() {}

    public static String parseAssumingEePrefix(String username) throws TokenEnrichmentException {
        if (StringUtil.isBlank(username) || !username.matches("^[eE]{2}\\d{11}")) {
            throw new TokenEnrichmentException("Attempted to parse personal identification code, " +
                "but it was either blank or not in an expected format: " + username);
        }

        return username.substring(2);
    }
}
