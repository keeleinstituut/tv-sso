package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;

import static java.time.Duration.ofSeconds;

public class LocalKeycloakContainer extends GenericContainer<LocalKeycloakContainer> {
    @SuppressWarnings("resource")
    public LocalKeycloakContainer() {
        super(new ImageFromDockerfile().withDockerfile(Path.of(".", "..", "Dockerfile")));
        withCopyFileToContainer(MountableFile.forClasspathResource("realm-tolkevarav-dev.json"), "/opt/bitnami/keycloak/data/import/realm-tolkevarav-dev.json");
        withEnv("KEYCLOAK_EXTRA_ARGS", "--import-realm");
        withEnv("KEYCLOAK_PRODUCTION", "FALSE");
        withExposedPorts(8080);
        setWaitStrategy(Wait.forHttp("/")
            .forPort(8080)
            .withStartupTimeout(ofSeconds(120))
        );

    }

    public LocalKeycloakContainer withPostgresHost(String postgresHost) {
        return withEnv("KEYCLOAK_DATABASE_HOST", postgresHost);
    }

    public LocalKeycloakContainer withDatabaseName(String databaseName) {
        return withEnv("KEYCLOAK_DATABASE_NAME", databaseName);
    }

    public LocalKeycloakContainer withDatabaseUser(String databaseUser) {
        return withEnv("KEYCLOAK_DATABASE_USER", databaseUser);
    }

    public LocalKeycloakContainer withDatabasePassword(String databasePassword) {
        return withEnv("KEYCLOAK_DATABASE_PASSWORD", databasePassword);
    }
}
