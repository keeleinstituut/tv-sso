package ee.eki.tolkevarav.sso.keycloakserviceprovider.util.auditlogclient;

class AuditLogClientConfiguration {

    private String rabbitMqHost;
    private int rabbitMqPort;
    private String rabbitMqUser;
    private String rabbitMqPassword;
    private String tvEventListenerClientId;

    public static AuditLogClientConfiguration fromSystemEnv() {
        AuditLogClientConfiguration configuration = new AuditLogClientConfiguration();
        configuration.rabbitMqHost = System.getenv("RABBITMQ_HOST");
        configuration.rabbitMqPort = Integer.parseInt(System.getenv("RABBITMQ_PORT"));
        configuration.rabbitMqUser = System.getenv("RABBITMQ_USER");
        configuration.rabbitMqPassword = System.getenv("RABBITMQ_PASSWORD");
        configuration.tvEventListenerClientId = System.getenv("TV_EVENT_LISTENER_CLIENT_ID");
        return configuration;
    }

    public String getRabbitMqHost() {
        return rabbitMqHost;
    }

    public int getRabbitMqPort() {
        return rabbitMqPort;
    }

    public String getRabbitMqUser() {
        return rabbitMqUser;
    }

    public String getRabbitMqPassword() {
        return rabbitMqPassword;
    }

    public String getTvEventListenerClientId() {
        return tvEventListenerClientId;
    }
}
