package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import java.util.Map;
import java.util.function.Consumer;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.*;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.*;

public class ClaimsAssertions {
    static Consumer<Map<String, Object>> containsStandardAndCustomAccessTokenClaims(TestIdentity identity) {
        return containsExpectedClaimsGiven(identity, EXPECTED_STANDARD_ACCESS_TOKEN_CLAIMS);
    }

    static Consumer<Map<String, Object>> containsStandardAndCustomIdTokenClaims(TestIdentity identity) {
        return containsExpectedClaimsGiven(identity, EXPECTED_STANDARD_ID_TOKEN_CLAIMS);
    }

    static Consumer<Map<String, Object>> containsStandardAndCustomUserinfoClaims(TestIdentity identity) {
        return containsExpectedClaimsGiven(identity, EXPECTED_STANDARD_USERINFO_CLAIMS);
    }

    static void containsStandardAccessTokenClaimsNotCustom(Map<String, Object> map) {
        containsExpectedClaimsNotCustomGiven(map, EXPECTED_STANDARD_ACCESS_TOKEN_CLAIMS);
    }

    static void containsStandardIdTokenClaimsNotCustom(Map<String, Object> map) {
        containsExpectedClaimsNotCustomGiven(map, EXPECTED_STANDARD_ID_TOKEN_CLAIMS);
    }

    static void containsStandardUserinfoClaimsNotCustom(Map<String, Object> map) {
        containsExpectedClaimsNotCustomGiven(map, EXPECTED_STANDARD_USERINFO_CLAIMS);
    }

    private static void containsExpectedClaimsNotCustomGiven(Map<String, Object> map, String... expectedNonCustomClaims) {
        assertThat(map)
            .containsKeys(expectedNonCustomClaims)
            .doesNotContainKey(CUSTOM_CLAIMS_KEY);
    }

    private static Consumer<Map<String, Object>> containsExpectedClaimsGiven(TestIdentity identity, String... expectedNonCustomClaims) {
        return map -> assertThat(map)
            .containsKeys(expectedNonCustomClaims)
            .extractingByKey(CUSTOM_CLAIMS_KEY)
            .isNotNull()
            .isInstanceOf(String.class)
            .extracting(Util::convertToMap, as(map(String.class, Object.class)))
            .satisfies(
                hasExpectedPersonalIdentificationCode(identity),
                ClaimsAssertions::hasExpectedUserId,
                ClaimsAssertions::hasExpectedInstitutionUserId,
                ClaimsAssertions::hasExpectedForename,
                ClaimsAssertions::hasExpectedSurname,
                ClaimsAssertions::hasExpectedPrivileges,
                ClaimsAssertions::hasExpectedSelectedInstitution
            );
    }


    private static Consumer<Map<String, Object>> hasExpectedPersonalIdentificationCode(TestIdentity identity) {
        return map -> assertThat(map)
            .extractingByKey(PERSONAL_IDENTIFICATION_CODE_KEY)
            .isEqualTo(identity.getPersonalIdentificationCode());
    }

    private static void hasExpectedUserId(Map<String, Object> tvClaims) {
        assertThat(tvClaims)
            .extractingByKey(USER_ID.getName())
            .isEqualTo(USER_ID.getValue());
    }

    private static void hasExpectedInstitutionUserId(Map<String, Object> tvClaims) {
        assertThat(tvClaims)
            .extractingByKey(INSTITUTION_USER_ID.getName())
            .isEqualTo(INSTITUTION_USER_ID.getValue());
    }

    private static void hasExpectedForename(Map<String, Object> tvClaims) {
        assertThat(tvClaims)
            .extractingByKey(FORENAME.getName())
            .isEqualTo(FORENAME.getValue());
    }

    private static void hasExpectedSurname(Map<String, Object> map) {
        assertThat(map)
            .extractingByKey(SURNAME.getName())
            .isEqualTo(SURNAME.getValue());
    }

    private static void hasExpectedPrivileges(Map<String, Object> map) {
        assertThat(map)
            .extractingByKey(PRIVILEGES_KEY)
            .asInstanceOf(LIST)
            // .extracting(Util::convertToStringList, as(list(String.class)))
            .containsExactlyInAnyOrderElementsOf(PRIVILEGES);
    }

    private static void hasExpectedSelectedInstitution(Map<String, Object> map) {
        assertThat(map)
            .extractingByKey(INSTITUTION_KEY)
            .isNotNull()
            .asInstanceOf(MAP)
            // .extracting(Util::convertToMap, as(map(String.class, String.class)))
            .satisfies(
                (institution) -> assertThat(institution).extractingByKey(INSTITUTION_ID.getName()).isEqualTo(INSTITUTION_ID.getValue()),
                (institution) -> assertThat(institution).extractingByKey(INSTITUTION_NAME.getName()).isEqualTo(INSTITUTION_NAME.getValue())
            );
    }
}
