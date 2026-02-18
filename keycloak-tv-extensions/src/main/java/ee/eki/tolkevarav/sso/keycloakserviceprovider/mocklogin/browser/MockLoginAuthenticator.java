package ee.eki.tolkevarav.sso.keycloakserviceprovider.mocklogin.browser;

import static ee.eki.tolkevarav.sso.keycloakserviceprovider.mocklogin.MockAuthenticatorUtil.findOrCreateUser;
import static ee.eki.tolkevarav.sso.keycloakserviceprovider.mocklogin.MockAuthenticatorUtil.username;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class MockLoginAuthenticator implements Authenticator {

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    Response challenge = context.form()
        .createForm("mock-login.ftl");
    context.challenge(challenge);
  }

  @Override
  public void action(AuthenticationFlowContext context) {
    EventBuilder event = context.getEvent();
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

    String idCode = formData.getFirst("idCode");
    String username = username(idCode);

    UserModel user = findOrCreateUser(context, username);
    if (!user.isEnabled()) {
      event.clone()
           .detail(Details.USERNAME, username)
           .user(user).error(Errors.USER_DISABLED);
    } else {
      context.setUser(user);
    }

    context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

    context.success();

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
  public void close() {

  }
}
