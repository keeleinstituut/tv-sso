package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@SuppressWarnings("resource")
public class MockTaraContainer extends GenericContainer<MockTaraContainer> {
    public MockTaraContainer() {
        super("ghcr.io/navikt/mock-oauth2-server:0.5.8");
        withExposedPorts(8080);
        withEnv("LOG_LEVEL", "DEBUG");
        waitingFor(Wait.forLogMessage(".*started server.*", 1));
    }
}
