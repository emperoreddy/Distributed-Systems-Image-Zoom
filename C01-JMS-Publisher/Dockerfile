
FROM critoma/linux-u20-dev-security-ism


ENV JAVA_HOME=/opt/software/jdk-17.0.2
ENV PATH="${JAVA_HOME}/bin:${PATH}"


ENV CATALINA_HOME=/opt/software/apache-tomcat-10.0.20


RUN rm -rf $CATALINA_HOME/webapps/*


COPY C01-JMS-Publisher.war $CATALINA_HOME/webapps/ROOT.war


COPY src/main/webapp/WEB-INF/lib/ /opt/software/apache-tomcat-10.0.20/lib/


RUN chmod +x $CATALINA_HOME/bin/catalina.sh


EXPOSE 8080


CMD ["sh", "-c", "$CATALINA_HOME/bin/catalina.sh run"]
