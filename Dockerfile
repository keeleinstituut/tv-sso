FROM gradle:jdk17 as build
WORKDIR /home/app

COPY keycloak-tv-extensions/build.gradle keycloak-tv-extensions/settings.gradle ./
COPY keycloak-tv-extensions/gradle ./gradle
COPY keycloak-tv-extensions/src/main ./src/main

RUN gradle jar --no-daemon

RUN curl -L -o /tmp/amqp-client-5.22.0.jar https://repo1.maven.org/maven2/com/rabbitmq/amqp-client/5.22.0/amqp-client-5.22.0.jar

# TODO: Use an immutable image tag before moving to production
FROM bitnamilegacy/keycloak:21.0.2

COPY --from=build /home/app/build/libs/*.jar /opt/bitnami/keycloak/providers/
COPY --from=build /tmp/amqp-client-5.22.0.jar /opt/bitnami/keycloak/providers/amqp-client-5.22.0.jar
