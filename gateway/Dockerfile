# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY service/pom.xml service/pom.xml
COPY gateway/pom.xml gateway/pom.xml
RUN mvn -pl gateway -am -N -Dmaven.repo.local=/root/.m2 dependency:resolve || true
COPY gateway/src gateway/src
RUN mvn -pl gateway -am -DskipTests -Dmaven.repo.local=/root/.m2 package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/gateway/target/hw1-gateway.jar /app/app.jar
EXPOSE 9000
ENV JAVA_OPTS="-XX:+UseG1GC -Xms256m -Xmx384m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
