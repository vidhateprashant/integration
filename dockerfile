FROM openjdk:11
COPY logs/suitetalk-axis-proxy-v2022_1-1.0.0.jar suitetalk-axis-proxy-v2022_1-1.0.0.jar
COPY target/integration-0.0.1-SNAPSHOT.jar integration.jar
ENTRYPOINT ["java","-jar","integration.jar"]