
FROM critoma/linux-u20-dev-security-ism


ENV JAVA_HOME=/opt/software/jdk-17.0.2
ENV PATH="${JAVA_HOME}/bin:${PATH}"


WORKDIR /app


COPY C03-MDB-RMI.jar /app/C03-MDB-RMI.jar


COPY src/main/webapp/WEB-INF/lib/ /app/lib/ 


ENV CLASSPATH=app/lib/*:/app/C03-MDB-RMI.jar


EXPOSE 8083


CMD ["java", "-jar", "C03-MDB-RMI.jar"]
