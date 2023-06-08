package ee.eki.tolkevarav.sso.keycloakserviceprovider.mocklogin;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class MockAuthenticatorUtil {

  public static UserModel findOrCreateUser(AuthenticationFlowContext context, String username) {
    UserModel user = findUser(context, username);

    if (user == null) {
      user = createUser(context, username);
    }
    setAmrIfMissing(user);

    return user;
  }

  public static String username(String idCode) {
    return idCode != null ? "ee" + idCode : null;
  }

  private static UserModel findUser(AuthenticationFlowContext context, String username) {
    return context.getSession().users().getUserByUsername(context.getRealm(), username);
  }

  private static UserModel createUser(AuthenticationFlowContext context, String username) {
    RealmModel realm = context.getRealm();
    UserModel user = context.getSession().users().addUser(realm, username);
    user.setFirstName(username + "-first-name");
    user.setLastName(username + "-last-name");
    user.setEnabled(true);
    return user;
  }

  private static void setAmrIfMissing(UserModel user) {
    String amrAttribute = user.getFirstAttribute("amr");
    if (amrAttribute == null || amrAttribute.isEmpty()) {
      user.setSingleAttribute("amr", "mID");
    }
  }
}
