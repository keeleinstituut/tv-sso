package ee.eki.tolkevarav.sso.keycloakserviceprovider.mocklogin.directgrant;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.mocklogin.MockAuthenticatorUtil.username;

import com.google.auto.service.AutoService;
import ee.eki.tolkevarav.sso.keycloakserviceprovider.mocklogin.MockAuthenticatorUtil;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.directgrant.AbstractDirectGrantAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

@AutoService(AuthenticatorFactory.class)
public class MockIdCodeAuthenticator extends AbstractDirectGrantAuthenticator {
  private static final String PROVIDER_ID = "mock-direct-grant-id-code";

  @Override
  public void authenticate(AuthenticationFlowContext context) {

    String username = retrieveUsername(context);
    if (username == null) {
      context.getEvent().error(Errors.USER_NOT_FOUND);
      Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(),
                                                 "invalid_request", "Missing parameter: id_code");
      context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
      return;
    }
    context.getEvent().detail(Details.USERNAME, username);
    context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

    UserModel user = MockAuthenticatorUtil.findOrCreateUser(context, username);

    if (!user.isEnabled()) {
      context.getEvent().user(user);
      context.getEvent().error(Errors.USER_DISABLED);
      Response challengeResponse = errorResponse(Response.Status.BAD_REQUEST.getStatusCode(),
                                                 "invalid_grant", "Account disabled");
      context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
      return;
    }

    context.setUser(user);
    context.success();

  }

  private String retrieveUsername(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
    return username(inputData.getFirst("id_code"));

  }

  @Override
  public boolean requiresUser() {
    return false;
  }

  @Override
  public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
    return false;
  }

  @Override
  public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

  }

  @Override
  public String getDisplayType() {
    return "Mock user from id_code";
  }

  @Override
  public String getReferenceCategory() {
    return null;
  }

  @Override
  public boolean isConfigurable() {
    return false;
  }

  @Override
  public Requirement[] getRequirementChoices() {
    return new Requirement[] {
      Requirement.REQUIRED,
      Requirement.DISABLED
    };
  }

  @Override
  public boolean isUserSetupAllowed() {
    return false;
  }

  @Override
  public String getHelpText() {
    return "Grants access to username generated from 'id_code' parameter in direct grant request. FOR TESTING ONLY!";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return null;
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
