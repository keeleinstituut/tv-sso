package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.keycloak.utils.StringUtil;

public class TaraParser {
    private TaraParser() {}

    public static final String TARA_SUBJECT_CLAIM_ATTRIBUTE_KEY = "tara_subject_claim";

    public static String parsePicFromTaraSubClaim(String taraSubjectClaim) throws UnacceptableMapperException {
        if (StringUtil.isBlank(taraSubjectClaim) || !taraSubjectClaim.matches("^[eE]{2}\\d{11}")) {
            throw new UnacceptableMapperException("Attempted to parse personal identification code, " +
                "but it was either blank or not in an expected format: " + taraSubjectClaim);
        }

        return taraSubjectClaim.substring(2);
    }
}
