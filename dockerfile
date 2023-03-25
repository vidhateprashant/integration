FROM openjdk:11
COPY target/integration-0.0.1-SNAPSHOT.jar integration.jar
ENTRYPOINT ["java","-jar","integration.jar"]