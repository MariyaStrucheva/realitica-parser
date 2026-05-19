FROM eclipse-temurin:25-jre
COPY ./build/libs/mne-estate-parser-*.jar application.jar
ENTRYPOINT ["java", "-jar", "application.jar"]
