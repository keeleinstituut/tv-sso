FROM gradle:jdk17 as build
WORKDIR /home/app
COPY keycloak-claims-mapper .
RUN gradle jar

# TODO: Use an immutable image tag before moving to production
FROM bitnami/keycloak:21.0.2

COPY --from=build /home/app/build/libs/*.jar /opt/bitnami/keycloak/providers/
RUN cd /opt/bitnami/keycloak/providers && curl -O https://repo1.maven.org/maven2/com/rabbitmq/amqp-client/5.22.0/amqp-client-5.22.0.jar
