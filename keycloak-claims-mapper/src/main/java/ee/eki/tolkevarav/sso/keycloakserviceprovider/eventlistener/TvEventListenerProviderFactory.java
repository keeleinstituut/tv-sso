package ee.eki.tolkevarav.sso.keycloakserviceprovider.eventlistener;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@AutoService(EventListenerProviderFactory.class)
public class TvEventListenerProviderFactory implements EventListenerProviderFactory {

    private TvEventListenerConfiguration configuration;

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new TvEventListenerProvider(keycloakSession, configuration);
    }

    @Override
    public void init(Config.Scope scope) {
        configuration = TvEventListenerConfiguration.fromSystemEnv();
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "tv-event-listener";
    }
}
