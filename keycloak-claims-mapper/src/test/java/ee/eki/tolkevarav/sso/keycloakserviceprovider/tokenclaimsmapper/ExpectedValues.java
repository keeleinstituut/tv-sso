package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.List;
import java.util.Map;

public class ExpectedValues {
    final static String[] EXPECTED_STANDARD_ACCESS_TOKEN_CLAIMS = {"sub", "iat", "iss", "exp", "jti", "email_verified", "preferred_username", "sid"};
    final static String[] EXPECTED_STANDARD_USERINFO_CLAIMS = {"sub", "email_verified", "preferred_username"};

    final static String[] EXPECTED_STANDARD_ID_TOKEN_CLAIMS = {"sub", "email_verified", "preferred_username"};
    final static String CUSTOM_CLAIMS_KEY = "tolkevarav";
    final static String PERSONAL_IDENTIFICATION_CODE_KEY = "personalIdentificationCode";
    final static NameValuePair USER_ID = of("userId", "4c876e3a-cde0-4651-bbe6-0dd6ff93bc0c");
    final static NameValuePair INSTITUTION_USER_ID = of("institutionUserId", "ea1ccdbd-bd61-4a95-9527-a3bac503fc9a");
    final static NameValuePair FORENAME = of("forename", "John");
    final static NameValuePair SURNAME = of("surname", "Smith");
    final static String INSTITUTION_KEY = "selectedInstitution";
    final static NameValuePair EXAMPLE_INSTITUTION_ID = of("id", "5730fdc0-782f-45c9-90e6-2d5a177ddc06");
    final static NameValuePair INSTITUTION_NAME = of("name", "Smith Ltd.");
    final static String PRIVILEGES_KEY = "privileges";
    final static List<String> PRIVILEGES = List.of("ADD_USER", "EDIT_USER");
    static final String SELECTED_INSTITUTION_ID_HEADER_KEY = "X-Selected-Institution-ID";

    private static BasicNameValuePair of(String name, String value) {
        return new BasicNameValuePair(name, value);
    }

    static String expectedTolkevaravApiInstitutionUserResponseJson(TestIdentity identity) {
        try {
            return new ObjectMapper().writeValueAsString(Map.of(
                    PERSONAL_IDENTIFICATION_CODE_KEY, identity.getPersonalIdentificationCode(),
                    USER_ID.getName(), USER_ID.getValue(),
                    INSTITUTION_USER_ID.getName(), INSTITUTION_USER_ID.getValue(),
                    FORENAME.getName(), FORENAME.getValue(),
                    SURNAME.getName(), SURNAME.getValue(),
                    PRIVILEGES_KEY, PRIVILEGES,
                    INSTITUTION_KEY, Map.of(
                            EXAMPLE_INSTITUTION_ID.getName(), EXAMPLE_INSTITUTION_ID.getValue(),
                            INSTITUTION_NAME.getName(), INSTITUTION_NAME.getValue()
                    )
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static String expectedTolkevaravApiUserResponseJson(TestIdentity identity) {
        try {
            return new ObjectMapper().writeValueAsString(Map.of(
                    PERSONAL_IDENTIFICATION_CODE_KEY, identity.getPersonalIdentificationCode(),
                    USER_ID.getName(), USER_ID.getValue(),
                    FORENAME.getName(), FORENAME.getValue(),
                    SURNAME.getName(), SURNAME.getValue()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    enum TestIdentity {
        SUCCESSFUL_INSTITUTION_USER_RESPONSE_IDENTITY("EE49311255780"),
        SUCCESSFUL_USER_RESPONSE_IDENTITY("EE50301024958"),
        DELAYED_RESPONSE_IDENTITY("EE36010273790"),
        EMPTY_RESPONSE_IDENTITY("EE62207160033"),
        FORBIDDEN_RESPONSE_IDENTITY("EE43503304242"),
        NOT_FOUND_RESPONSE_IDENTITY("EE44208104267");

        private final String personalIdentificationCode;
        private final String taraSubjectClaim;

        TestIdentity(String taraSubjectClaim) {
            this.taraSubjectClaim = taraSubjectClaim;
            this.personalIdentificationCode = taraSubjectClaim.substring(2);
        }

        String getPersonalIdentificationCode() {
            return personalIdentificationCode;
        }

        String getTaraSubjectClaim() {
            return taraSubjectClaim;
        }
    }
}
