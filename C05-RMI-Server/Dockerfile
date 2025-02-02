
FROM critoma/linux-u20-dev-security-ism


ENV JAVA_HOME=/opt/software/jdk-17.0.2
ENV PATH=${JAVA_HOME}/bin:${PATH}


WORKDIR /app


COPY C05-RMI-Server.jar /app/C05-RMI-Server.jar


COPY src/main/webapp/WEB-INF/lib/ /app/lib/


ENV CLASSPATH=/app/lib/*:/app/C05-RMI-Server.jar


EXPOSE 1100


CMD ["java", "-jar", "C05-RMI-Server.jar"]
