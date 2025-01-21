# C01-JMS-Publisher/Dockerfile

FROM critoma/linux-u20-dev-security-ism

# Set JAVA_HOME to JDK 17
ENV JAVA_HOME=/opt/software/jdk-17.0.2
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Set Tomcat home
ENV CATALINA_HOME=/opt/software/apache-tomcat-10.0.20

# Remove default web applications
RUN rm -rf $CATALINA_HOME/webapps/*

# Copy the WAR file to Tomcat's webapps directory as ROOT.war
COPY C01-JMS-Publisher.war $CATALINA_HOME/webapps/ROOT.war

# Copy necessary libraries (if any)
COPY src/main/webapp/WEB-INF/lib/ /opt/software/apache-tomcat-10.0.20/lib/

# Ensure catalina.sh has execute permissions
RUN chmod +x $CATALINA_HOME/bin/catalina.sh

# Expose Tomcat's default port
EXPOSE 8080

# Start Tomcat using the absolute path to catalina.sh
CMD ["sh", "-c", "$CATALINA_HOME/bin/catalina.sh run"]
