FROM ubuntu:latest AS build

RUN apt-get update
RUN apt-get install openjdk-17-jdk -y
COPY . .

RUN apt-get install maven -y
RUN mvn clean install

FROM openjdk:17-jdk-slim

EXPOSE 8080

COPY --from=build /target/chat-jms-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["/wait-for-it.sh", "activemq:61616", "--", "java", "-jar", "app.jar" ]