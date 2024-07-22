#!/bin/bash

mvn clean
mvn package -X
java -jar target/convert-0.0.1-SNAPSHOT.jar
