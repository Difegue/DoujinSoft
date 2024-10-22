# Script to quickly bringup a Dsoft server 
# (meant for codespaces but should work on anything as long as you have the /home/doujinsoft directory created)

npm ci
npm run install-front
rm -rf target/*.war
mvn package -f "pom.xml"

# Copy WAR from context
rm -rf /usr/local/tomcat/webapps/*
cp target/*.war /usr/local/tomcat/webapps/ROOT.war

# Copy context.xml from context
cp Docker/context.xml /usr/local/tomcat/conf/context.xml

# Setup env variables
export DSOFT_PASS=admin:admin

/usr/local/tomcat/bin/catalina.sh run