package ee.eki.tolkevarav.sso.keycloakserviceprovider.eventlistener;

import ee.eki.tolkevarav.sso.keycloakserviceprovider.serviceaccountfetcher.ServiceAccountFetcher;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class TvEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(TvEventListenerProvider.class);

    private final KeycloakSession session;
    private final AuditLogClient auditLogClient;

    public TvEventListenerProvider(KeycloakSession session, TvEventListenerConfiguration configuration) {
        this.session = session;
        ServiceAccountFetcher serviceAccountFetcher = new ServiceAccountFetcher(session, configuration.getTvEventListenerClientId());
        this.auditLogClient = new AuditLogClient(configuration, serviceAccountFetcher);
    }

    @Override
    public void onEvent(Event event) {
        logger.info("Received user event with type %s".formatted(event.getType()));
        List<AuditLogMessage> messages = this.constructAuditLogMessages(event);

        if (messages == null) {
            return;
        }

        messages.forEach(auditLogMessage -> {
            try {
                this.auditLogClient.send(auditLogMessage);
            } catch (IOException e) {
                logger.error("Encountered error sending message with AuditLogClient", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {
        try {
            this.auditLogClient.close();
        } catch (IOException | TimeoutException e) {
            logger.error("Encountered error closing AuditLogClient", e);
        }
    }

    private List<AuditLogMessage> constructAuditLogMessages(Event event) {
        if (event.getSessionId() == null) {
            return null;
        }

        RealmModel realm = session.realms().getRealm(event.getRealmId());
        UserSessionModel userSession = session.sessions().getUserSession(realm, event.getSessionId());

        if (userSession == null) {
            return null;
        }

        Map<String, String> notes = userSession.getNotes();
        List<AuditLogMessage> messages = new ArrayList<>();

        switch (event.getType()) {
            case LOGOUT -> messages.add(new AuditLogMessage()
                            .fillUserInfo(notes)
                            .fillInstitutionInfo(notes)
                            .eventType("LOG_OUT"));
            case REFRESH_TOKEN -> {
                String selectedInstitutionId = notes.get("TV_SELECTED_INSTITUTION_ID");
                String previousInstitutionId = notes.get("TV_PREVIOUS_SELECTED_INSTITUTION_ID");

                if (selectedInstitutionId != null) {
                    if (previousInstitutionId == null) {
                        messages.add(new AuditLogMessage()
                                        .fillUserInfo(notes)
                                        .fillInstitutionInfo(notes)
                                        .eventType("LOG_IN"));
                    } else if (!selectedInstitutionId.equals(previousInstitutionId)) {
                        messages.add(new AuditLogMessage()
                                        .fillUserInfo(notes)
                                        .fillPreviousInstitutionInfo(notes)
                                        .eventType("LOG_OUT"));
                        messages.add(new AuditLogMessage()
                                        .fillUserInfo(notes)
                                        .fillInstitutionInfo(notes)
                                        .eventType("LOG_IN"));
                   }
                }
            }
        }

        return messages;
    }
}
