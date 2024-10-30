package ee.eki.tolkevarav.sso.keycloakserviceprovider.eventlistener;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import ee.eki.tolkevarav.sso.keycloakserviceprovider.serviceaccountfetcher.ServiceAccountFetcher;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class AuditLogClient {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    private static final Logger logger = Logger.getLogger(AuditLogClient.class);

    private final ServiceAccountFetcher serviceAccountFetcher;
    private final TvEventListenerConfiguration configuration;

    private Connection connection;
    private Channel channel;

    public AuditLogClient(TvEventListenerConfiguration configuration, ServiceAccountFetcher serviceAccountFetcher) {
        this.configuration = configuration;
        this.serviceAccountFetcher = serviceAccountFetcher;

        try {
            initConnection();
        } catch (IOException | TimeoutException e) {
            logger.error("Cannot initialize RabbitMQ connection for AuditLogClient", e);
            throw new RuntimeException(e);
        }
    }

    private void initConnection() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(this.configuration.getRabbitMqHost());
        connectionFactory.setPort(this.configuration.getRabbitMqPort());
        connectionFactory.setUsername(this.configuration.getRabbitMqUser());
        connectionFactory.setPassword(this.configuration.getRabbitMqPassword());

        this.connection = connectionFactory.newConnection();
        this.channel = connection.createChannel();
    }

    public void close() throws IOException, TimeoutException {
        this.channel.close();
        this.connection.close();
    }

    public void send(AuditLogMessage message) throws IOException {
        String json = objectMapper.writeValueAsString(message);

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .headers(Map.of(
                    "jwt", this.serviceAccountFetcher.getServiceAccountAccessToken()
                ))
                .build();

        this.channel.basicPublish("", "audit-log-events", properties, json.getBytes());
        logger.info("Sent AuditLog message with type %s".formatted(message.event_type));
    }
}
