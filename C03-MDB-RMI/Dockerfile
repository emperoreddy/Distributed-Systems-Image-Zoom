FROM critoma/linux-u20-dev-security-ism

ENV PATH=$PATH:/opt/software/apache-tomcat-10.0.20/bin
ENV JAVA_HOME=/opt/software/jdk-21

# Create directory for uploaded images if needed
RUN mkdir -p /opt/uploaded-images && \
    chmod 777 /opt/uploaded-images

# Copy the WAR file
COPY C03-MDB-RMI.war /opt/software/apache-tomcat-10.0.20/webapps/

EXPOSE 8082

CMD ["catalina.sh", "run"]
