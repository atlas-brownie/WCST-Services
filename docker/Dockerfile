FROM maven:3.6.3-jdk-11-slim AS builder
RUN apt-get update

COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:11-jre-slim

COPY --from=builder usr/src/app/target/*.jar app.jar

# ARG JAR_FILE
# ADD target/${JAR_FILE} app.jar
EXPOSE 8080

ENTRYPOINT exec java $JAVA_OPTS -Dspring.profiles.active=dev -jar /app.jar