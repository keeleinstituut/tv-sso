# TODO: Once JWT-mapping provider has been created, add build steps as suggested by commented-out code

#FROM gradle:jdk17 as build
#WORKDIR /home/app
#COPY keycloak-tolkevarav-provider keycloak-tolkevarav-provider
#RUN gradle build

# TODO: Use an immutable image tag before moving to production
FROM bitnami/keycloak:20


#COPY --from=build /home/app/keycloak-tolkevarav-provider/build/libs/keycloak-tolkevarav-provider.jar /opt/bitnami/keycloak/standalone/deployments/keycloak-tolkevarav-provider.jar
