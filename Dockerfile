FROM gradle:jdk17 as build

WORKDIR /home/app

COPY keycloak-tv-extensions/build.gradle keycloak-tv-extensions/settings.gradle ./
COPY keycloak-tv-extensions/gradle ./gradle
COPY keycloak-tv-extensions/src/main ./src/main

RUN gradle jar --no-daemon

RUN curl -L -o /tmp/amqp-client-5.22.0.jar https://repo1.maven.org/maven2/com/rabbitmq/amqp-client/5.22.0/amqp-client-5.22.0.jar

FROM quay.io/keycloak/keycloak:21.0.2

COPY --from=build /home/app/build/libs/*.jar /opt/keycloak/providers/
COPY --from=build /tmp/amqp-client-5.22.0.jar /opt/keycloak/providers/amqp-client-5.22.0.jar

RUN /opt/keycloak/bin/kc.sh build --db=postgres --health-enabled=true --metrics-enabled=true