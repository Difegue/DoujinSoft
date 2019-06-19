FROM tomcat:9-jdk11-openjdk-slim

# Copy WAR from context and overwrite tomcat default app
COPY /target/*.war /usr/local/tomcat/webapps/ROOT.war

# Copy context.xml from context
COPY /Docker/context.xml /usr/local/tomcat/conf/context.xml

# Create data directory
RUN mkdir /home/doujinsoft

EXPOSE 8080
CMD ["catalina.sh", "run"]
