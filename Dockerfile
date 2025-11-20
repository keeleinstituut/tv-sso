FROM gradle:jdk17 as build
WORKDIR /home/app
COPY keycloak-tv-extensions .
RUN gradle jar
RUN curl -L -o /home/app/build/libs/amqp-client-5.22.0.jar https://repo1.maven.org/maven2/com/rabbitmq/amqp-client/5.22.0/amqp-client-5.22.0.jar

FROM quay.io/keycloak/keycloak:21.0.2 as keycloak-build

COPY --from=build /home/app/build/libs/*.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:21.0.2
COPY --from=keycloak-build /opt/keycloak/ /opt/keycloak/