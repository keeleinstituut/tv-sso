package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import java.util.Map;
import java.util.function.Consumer;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper.ExpectedValues.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.*;

public class ClaimsAssertions {
    static Consumer<Map<String, Object>> containsStandardAccessTokenClaimsAndInstitutionUserData(TestIdentity identity) {
        return containsGivenClaimsWithInstitutionUserData(identity, EXPECTED_STANDARD_ACCESS_TOKEN_CLAIMS);
    }

    static Consumer<Map<String, Object>> containsStandardIdTokenClaimsAndInstitutionUserData(TestIdentity identity) {
        return containsGivenClaimsWithInstitutionUserData(identity, EXPECTED_STANDARD_ID_TOKEN_CLAIMS);
    }

    static Consumer<Map<String, Object>> containsStandardUserinfoClaimsAndInstitutionUserData(TestIdentity identity) {
        return containsGivenClaimsWithInstitutionUserData(identity, EXPECTED_STANDARD_USERINFO_CLAIMS);
    }

    static Consumer<Map<String, Object>> containsStandardAccessTokenClaimsAndUserData(TestIdentity identity) {
        return containsGivenClaimsWithUserData(EXPECTED_STANDARD_ACCESS_TOKEN_CLAIMS, identity);
    }

    static Consumer<Map<String, Object>> containsStandardIdTokenClaimsAndUserData(TestIdentity identity) {
        return containsGivenClaimsWithUserData(EXPECTED_STANDARD_ID_TOKEN_CLAIMS, identity);
    }

    static Consumer<Map<String, Object>> containsStandardUserinfoClaimsAndUserData(TestIdentity identity) {
        return containsGivenClaimsWithUserData(EXPECTED_STANDARD_USERINFO_CLAIMS, identity);
    }

    static Consumer<Map<String, Object>> containsStandardAccessTokenClaimsAndEmptyTolkevaravClaims() {
        return containsGivenClaimsWithEmptyTolkevaravClaims(EXPECTED_STANDARD_ACCESS_TOKEN_CLAIMS);
    }

    static Consumer<Map<String, Object>> containsStandardIdTokenClaimsAndEmptyTolkevaravClaims() {
        return containsGivenClaimsWithEmptyTolkevaravClaims(EXPECTED_STANDARD_ID_TOKEN_CLAIMS);
    }

    static Consumer<Map<String, Object>> containsStandardUserinfoClaimsAndEmptyTolkevaravClaims() {
        return containsGivenClaimsWithEmptyTolkevaravClaims(EXPECTED_STANDARD_USERINFO_CLAIMS);
    }

    private static Consumer<Map<String, Object>> containsGivenClaimsWithEmptyTolkevaravClaims(String[] expectedClaims) {
        return (map) -> assertThat(map)
                .containsKeys(expectedClaims)
                .extractingByKey(CUSTOM_CLAIMS_KEY)
                .isNotNull()
                .asInstanceOf(MAP)
                .isEmpty();
    }

    private static Consumer<Map<String, Object>> containsGivenClaimsWithUserData(String[] expectedClaims, TestIdentity identity) {
        return (map) -> assertThat(map)
            .containsKeys(expectedClaims)
            .extractingByKey(CUSTOM_CLAIMS_KEY)
            .isNotNull()
            .asInstanceOf(map(String.class, Object.class))
            .satisfies(
                hasExpectedPersonalIdentificationCode(identity),
                ClaimsAssertions::hasExpectedUserId,
                ClaimsAssertions::hasExpectedForename,
                ClaimsAssertions::hasExpectedSurname
            );
    }

    private static Consumer<Map<String, Object>> containsGivenClaimsWithInstitutionUserData(TestIdentity identity, String... expectedNonCustomClaims) {
        return map -> assertThat(map)
            .containsKeys(expectedNonCustomClaims)
            .extractingByKey(CUSTOM_CLAIMS_KEY)
            .isNotNull()
            .asInstanceOf(map(String.class, Object.class))
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
                (institution) -> assertThat(institution).extractingByKey(EXAMPLE_INSTITUTION_ID.getName()).isEqualTo(EXAMPLE_INSTITUTION_ID.getValue()),
                (institution) -> assertThat(institution).extractingByKey(INSTITUTION_NAME.getName()).isEqualTo(INSTITUTION_NAME.getValue())
            );
    }

    static void isUnsuccessfulResponseExceptionWithStatus500(Throwable object) {
        assertThat(object)
            .asInstanceOf(type(UnsuccessfulResponseException.class))
            .extracting(UnsuccessfulResponseException::getStatusCode)
            .isEqualTo(500);
    }
}
