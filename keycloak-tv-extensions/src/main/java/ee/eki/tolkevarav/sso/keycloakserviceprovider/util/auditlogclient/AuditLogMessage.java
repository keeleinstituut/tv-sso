package ee.eki.tolkevarav.sso.keycloakserviceprovider.util.auditlogclient;

import org.keycloak.models.UserSessionModel;

import java.time.Instant;
import java.util.Map;

public class AuditLogMessage {
    String happened_at;
    String trace_id = null;
    String event_type = null;
    String failure_type = null;
    String context_institution_id = null;
    String acting_institution_user_id = null;
    String context_department_id = null;
    String acting_user_pic = null;
    String acting_user_forename = null;
    String acting_user_surname = null;

    Map<String, Object> event_parameters = null;

    public AuditLogMessage() {
        this.happened_at = Instant.now().toString();
    }

    public AuditLogMessage fillUserInfo(UserSessionModel userSession) {
        Map<String, String> notes = userSession.getNotes();
        this.acting_user_pic = notes.get("TV_USER_PERSONAL_IDENTIFICATION_CODE");
        this.acting_user_forename = notes.get("TV_USER_FORENAME");
        this.acting_user_surname = notes.get("TV_USER_SURNAME");
        return this;
    }

    public AuditLogMessage fillInstitutionInfo(UserSessionModel userSession) {
        Map<String, String> notes = userSession.getNotes();
        this.context_institution_id = notes.get("TV_SELECTED_INSTITUTION_ID");
        this.acting_institution_user_id = notes.get("TV_INSTITUTION_USER_ID");
        this.context_department_id = notes.get("TV_DEPARTMENT_ID");
        return this;
    }

    public AuditLogMessage eventParameters(Map<String, Object> eventParameters) {
        this.event_parameters = eventParameters;
        return this;
    }

    public AuditLogMessage eventType(String eventType) {
        this.event_type = eventType;
        return this;
    }

}
