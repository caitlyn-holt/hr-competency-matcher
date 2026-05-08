FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test
ENTRYPOINT ["java", "-jar", "build/libs/HRmatch-0.0.1-SNAPSHOT.jar"]