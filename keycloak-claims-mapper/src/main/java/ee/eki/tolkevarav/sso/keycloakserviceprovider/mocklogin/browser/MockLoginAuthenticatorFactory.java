package ee.eki.tolkevarav.sso.keycloakserviceprovider.mocklogin.browser;

import java.util.List;

import com.google.auto.service.AutoService;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

@AutoService(AuthenticatorFactory.class)
public class MockLoginAuthenticatorFactory implements AuthenticatorFactory {
  private static final String PROVIDER_ID = "mock-login";

  private static final MockLoginAuthenticator AUTHENTICATOR_INSTANCE = new MockLoginAuthenticator();

  @Override
  public String getDisplayType() {
    return "Mock login";
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
    return "Displays text box for id code, finds or creates mock user by id code. FOR TESTING ONLY!";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return null;
  }

  @Override
  public Authenticator create(KeycloakSession session) {
    return AUTHENTICATOR_INSTANCE;
  }

  @Override
  public void init(Scope config) {

  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {

  }

  @Override
  public void close() {

  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
