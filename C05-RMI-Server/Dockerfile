# Use the base image with Java installed
FROM critoma/linux-u20-dev-security-ism

# Set JAVA_HOME to JDK 17.0.2
ENV JAVA_HOME=/opt/software/jdk-17.0.2
ENV PATH=${JAVA_HOME}/bin:${PATH}

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY C05-RMI-Server.jar /app/C05-RMI-Server.jar

# Copy the lib directory
COPY src/main/webapp/WEB-INF/lib/ /app/lib/

# Set classpath
ENV CLASSPATH=/app/lib/*:/app/C05-RMI-Server.jar

# Expose RMI port
EXPOSE 1100

# Run the RMI server
CMD ["java", "-jar", "C05-RMI-Server.jar"]
