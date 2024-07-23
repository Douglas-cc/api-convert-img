FROM teresaejunior/lubuntu

RUN apt update && apt install -y maven

WORKDIR /app
COPY src/main/java .
COPY src/main/webapp .

RUN mvn clean package

EXPOSE 8010

CMD [ "java", "-jar", "target/convert-0.0.1-SNAPSHOT.jar"] 
