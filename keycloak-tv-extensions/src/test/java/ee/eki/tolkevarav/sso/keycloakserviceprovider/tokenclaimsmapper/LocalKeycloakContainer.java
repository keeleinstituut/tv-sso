package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;

import static java.time.Duration.ofSeconds;

public class LocalKeycloakContainer extends GenericContainer<LocalKeycloakContainer> {
    private static final int CONTAINER_HTTP_PORT = 8080;

    @SuppressWarnings("resource")
    public LocalKeycloakContainer() {
        super(new ImageFromDockerfile().withDockerfile(Path.of(".", "..", "Dockerfile")));
        withCopyFileToContainer(MountableFile.forClasspathResource("realm-tolkevarav-dev.json"), "/opt/keycloak/data/import/realm-tolkevarav-dev.json");
        withEnv("KC_HOSTNAME_PORT", "8080");
        withCommand("start-dev", "--import-realm");
        withExposedPorts(CONTAINER_HTTP_PORT);
        setWaitStrategy(Wait.forHttp("/")
            .forPort(CONTAINER_HTTP_PORT)
            .withStartupTimeout(ofSeconds(120))
        );
    }

    public LocalKeycloakContainer withPostgresHost(String postgresHost) {
        return withEnv("KC_DB_URL_HOST", postgresHost);
    }

    public LocalKeycloakContainer withDatabaseName(String databaseName) {
        return withEnv("KC_DB_URL_DATABASE", databaseName);
    }

    public LocalKeycloakContainer withDatabaseUser(String databaseUser) {
        return withEnv("KC_DB_USERNAME", databaseUser);
    }

    public LocalKeycloakContainer withDatabasePassword(String databasePassword) {
        return withEnv("KC_DB_PASSWORD", databasePassword);
    }
}
