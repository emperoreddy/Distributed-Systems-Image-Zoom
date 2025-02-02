
FROM critoma/linux-u20-dev-security-ism


ENV JAVA_HOME=/opt/software/jdk-17.0.2
ENV PATH=${JAVA_HOME}/bin:${PATH}


WORKDIR /app


COPY C04-RMI-Server.jar /app/C04-RMI-Server.jar


COPY src/main/webapp/WEB-INF/lib/ /app/lib/


ENV CLASSPATH=/app/lib/*:/app/C04-RMI-Server.jar


EXPOSE 1099


CMD ["java", "eu.deic.rmi.RMIServer"]
