FROM eclipse-temurin:17-jdk-alpine

WORKDIR /convert

COPY ./target/convert-0.0.1-SNAPSHOT.jar /convert/convert.jar

COPY ./src/main/webapp/WEB-INF/views/  /convert/src/main/webapp/WEB-INF/views/

COPY ./.env /convert/.env


EXPOSE 8010

RUN mkdir -p /convert/uploads

CMD [ "java", "-jar", "convert.jar"]
