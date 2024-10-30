package ee.eki.tolkevarav.sso.keycloakserviceprovider.eventlistener;

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

    public AuditLogMessage fillUserInfo(Map<String, String> userSessionNotes) {
        this.acting_user_pic = userSessionNotes.get("TV_USER_PERSONAL_IDENTIFICATION_CODE");
        this.acting_user_forename = userSessionNotes.get("TV_USER_FORENAME");
        this.acting_user_surname = userSessionNotes.get("TV_USER_SURNAME");
        return this;
    }

    public AuditLogMessage fillInstitutionInfo(Map<String, String> userSessionNotes) {
        this.context_institution_id = userSessionNotes.get("TV_SELECTED_INSTITUTION_ID");
        this.acting_institution_user_id = userSessionNotes.get("TV_INSTITUTION_USER_ID");
        this.context_department_id = userSessionNotes.get("TV_DEPARTMENT_ID");
        return this;
    }

    public AuditLogMessage fillPreviousInstitutionInfo(Map<String, String> userSessionNotes) {
        this.context_institution_id = userSessionNotes.get("TV_PREVIOUS_SELECTED_INSTITUTION_ID");
        this.acting_institution_user_id = userSessionNotes.get("TV_PREVIOUS_INSTITUTION_USER_ID");
        this.context_department_id = userSessionNotes.get("TV_PREVIOUS_DEPARTMENT_ID");
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
