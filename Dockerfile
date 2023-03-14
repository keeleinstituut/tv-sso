# TODO: Once JWT-mapping provider has been created, add build steps as suggested by commented-out code

#FROM gradle:jdk17 as build
#WORKDIR /home/app
#COPY keycloak-tolkevarav-provider keycloak-tolkevarav-provider
#RUN gradle build

# TODO: Use an immutable image tag before moving to production
FROM bitnami/keycloak:20

# Trivial mutation to ensure the image is not exactly same as Bitnami’s.
# TODO: Remove once there are other modifications.
COPY .gitignore /opt/bitnami/keycloak/standalone

#COPY --from=build /home/app/keycloak-tolkevarav-provider/build/libs/keycloak-tolkevarav-provider.jar /opt/bitnami/keycloak/standalone/deployments/keycloak-tolkevarav-provider.jar
