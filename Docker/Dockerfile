FROM tomcat:9.0.56-jre11-openjdk-slim-buster

# Copy WAR from context
RUN rm -rf /usr/local/tomcat/webapps
COPY /target/*.war /usr/local/tomcat/webapps/ROOT.war

# Copy context.xml from context
COPY /Docker/context.xml /usr/local/tomcat/conf/context.xml

# Create data directory
RUN mkdir /home/doujinsoft

EXPOSE 8080
CMD ["catalina.sh", "run"]
