# Use the base image with Java installed
FROM critoma/linux-u20-dev-security-ism

# Set JAVA_HOME and update PATH
ENV JAVA_HOME=/opt/software/jdk-17.0.2
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY C03-MDB-RMI.jar /app/C03-MDB-RMI.jar

# Copy the lib directory
COPY src/main/webapp/WEB-INF/lib/ /app/lib/ 

# Set classpath to include all JARs in lib and the application JAR
ENV CLASSPATH=app/lib/*:/app/C03-MDB-RMI.jar

# Expose application port if applicable (e.g., 8083)
EXPOSE 8083

# Run the application
CMD ["java", "-jar", "C03-MDB-RMI.jar"]
