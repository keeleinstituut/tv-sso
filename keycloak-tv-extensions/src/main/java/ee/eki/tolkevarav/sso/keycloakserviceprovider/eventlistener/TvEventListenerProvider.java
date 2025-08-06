package ee.eki.tolkevarav.sso.keycloakserviceprovider.eventlistener;

import ee.eki.tolkevarav.sso.keycloakserviceprovider.util.auditlogclient.AuditLogClient;
import ee.eki.tolkevarav.sso.keycloakserviceprovider.util.auditlogclient.AuditLogMessage;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class TvEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(TvEventListenerProvider.class);

    private final KeycloakSession session;
    // private final AuditLogClient auditLogClient;

    public TvEventListenerProvider(KeycloakSession session) {
        this.session = session;
        // this.auditLogClient = new AuditLogClient(session);
    }

    @Override
    public void onEvent(Event event) {
        // logger.info("Received user event with type %s".formatted(event.getType()));
        // List<AuditLogMessage> messages = this.constructAuditLogMessages(event);

        // if (messages == null) {
        //     return;
        // }

        // try {
        //     this.auditLogClient.send(messages);
        // } catch (IOException e) {
        //     logger.error("Encountered error sending messages with AuditLogClient", e);
        //     throw new RuntimeException(e);
        // }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {
        // try {
        //     this.auditLogClient.close();
        // } catch (IOException | TimeoutException e) {
        //     logger.error("Encountered error closing AuditLogClient", e);
        // }
    }

    // private List<AuditLogMessage> constructAuditLogMessages(Event event) {
    //     if (event.getSessionId() == null) {
    //         return null;
    //     }

    //     RealmModel realm = session.realms().getRealm(event.getRealmId());
    //     UserSessionModel userSession = session.sessions().getUserSession(realm, event.getSessionId());

    //     if (userSession == null) {
    //         return null;
    //     }

    //     List<AuditLogMessage> messages = new ArrayList<>();

    //     if (Objects.requireNonNull(event.getType()) == EventType.LOGOUT) {
    //         messages.add(new AuditLogMessage()
    //                 .fillUserInfo(userSession)
    //                 .fillInstitutionInfo(userSession)
    //                 .eventType("LOG_OUT"));
    //     }

    //     return messages;
    // }
}
