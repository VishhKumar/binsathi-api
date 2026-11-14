# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
# Windows line endings would break the wrapper script on Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B -q -DskipTests package

# ---------- Run stage ----------
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]