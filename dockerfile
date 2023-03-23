FROM openjdk:11
COPY target/integration-0.0.1-SNAPSHOT.jar integration.jar
COPY suitetalk-axis-proxy-v2022_1-1.0.0.jar .
ENTRYPOINT ["java","-jar","integration.jar"]