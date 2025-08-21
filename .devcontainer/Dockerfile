# See here for image contents: https://github.com/microsoft/vscode-dev-containers/tree/v0.233.0/containers/codespaces-linux/.devcontainer/base.Dockerfile

# Make sure to always use the latest version of the devcontainer image, as that's the only one cached on the Codespaces VM.
# Trying to use a different one will likely result in you running out of space.
FROM mcr.microsoft.com/vscode/devcontainers/universal:2-focal

USER root

# ** [Optional] Uncomment this section to install additional packages. **
#
# RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
#     && apt-get -y install --no-install-recommends <your-package-list-here>
#

RUN cd /tmp && wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.73/bin/apache-tomcat-9.0.73.tar.gz && tar -xzf apache-tomcat-9.0.73.tar.gz \
    && mv apache-tomcat-9.0.73 /usr/local/tomcat && chmod -R 777 /usr/local/tomcat && rm -rf apache-tomcat-9.0.73.tar.gz

# Grab the DB backup from the live site and use it as a starting point. 
RUN cd /tmp && wget https://tvc-16.science/dsoft-backup.zip && unzip dsoft-backup.zip && mv Doujinsoft /home/doujinsoft && chmod -R 777 /home/doujinsoft

USER codespace

# Default admin password is test:test
ENV DSOFT_PASS="test:test"

