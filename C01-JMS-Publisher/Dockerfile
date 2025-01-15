FROM critoma/linux-u20-dev-security-ism

ENV PATH=$PATH:/opt/software/apache-tomcat-10.0.20/bin
ENV JAVA_HOME=/opt/software/jdk-21

RUN mkdir -p /opt/uploaded-images && \
    chmod 777 /opt/uploaded-images

# Copy the WAR file
COPY C01-JMS-Publisher.war /opt/software/apache-tomcat-10.0.20/webapps/

EXPOSE 8080

CMD ["catalina.sh", "run"]
