# DoujinSoft

[![Docker Pulls](https://img.shields.io/docker/pulls/difegue/doujinsoft.svg)](https://hub.docker.com/r/difegue/doujinsoft/)

![screen1](https://cloud.githubusercontent.com/assets/8237712/26278790/4ffd633a-3da3-11e7-865f-015cab5fef5e.png)

Java Web Application for managing and distributing WWDIY content.  
Relies on the original [DIYEdit](https://gbatemp.net/threads/warioware-d-i-y-editor.346953/) by bobmcjr for everything reverse-engineering related.  
Uses SQLite, materializeCSS and jQuery.  

## Features  

* .mio files dropped into the Webapp's data directory are parsed and added to the archive (on every app restart)
* Searchable lists with pages for every content (Games/Comics/Records)
* Comics can be directly read through the user's browser
* Records can be listened to through the browser (uses [libTimidity](https://www.npmjs.com/package/timidity))
* All the content available on the archive can be automatically inserted into a game save provided by the user.
* Basic JSON API.

## Using the API

Adding `&format=json` to most search pages in the webapp will give you a JSON equivalent. Use at will! 🙋‍♂️  

## Environment variables

* WII_NUMBER: Wii Friend Code associated to the DoujinSoft WiiConnect24 account.
* WII_FALLBACK: Wii Friend Code that will receive Wii Mail forwarded from DoujinSoft.
* WC24_SERVER: WiiConnect24 server URL. Currently, there's only really `rc24.xyz`...
* WC24_PASSWORD: Password for the DoujinSoft WiiConnect24 account
* DSOFT_PASS: Password for the Admin console.
* WEBHOOK_URL: URL for a webhook. This hook is hit whenever new content is uploaded to DoujinSoft.

## Deploy/develop on Codespaces

You can use [GitHub Codespaces](https://github.com/Difegue/DoujinSoft/codespaces) to develop on the app using the provided configuration.  
Once in the codespace, you should be able to execute the `dev-server.sh` script to immediately build/start a DoujinSoft instance.

## Deploying through Docker

You can deploy the provided [Docker image](https://hub.docker.com/r/difegue/doujinsoft) by mapping a volume to /home/doujinsoft:
```
docker run --mount type=bind,src=/my/data/directory,dst=/home/doujinsoft -p 8080:8080 difegue/doujinsoft
```  

If you want to use WC24 interop, the environment variables WII_NUMBER, WII_FALLBACK, WC24_SERVER and WC24_PASSWORD must be defined.  

## Deploying manually

Get the [release WAR](https://github.com/Difegue/DoujinSoft/releases) (or just build it with maven from the sources)  
Edit your tomcat context to specify your data directory:  
```
nano /var/lib/tomcat8/conf/context.xml

<Parameter name="dataDirectory" value="/home/DoujinSoft-data" override="false"/>
```   
Create the folder you specified in the Parameter (here /home/DoujinSoft-data), and add .mio files to a "mio" subfolder in it.  
Don't forget to make it R/W by your tomcat server.  
```
cd /home
mkdir DoujinSoft-data
chown tomcat8 DoujinSoft-data/
chmod -R 755 DoujinSoft-data/
```
Drop the WAR into your tomcat webapps directory to start deployment. 

If you have a tomcat installed to `/usr/local/tomcat`, you can use the provided `dev-server.sh` script to start a DoujinSoft instance.  
Keep in mind said script will overwrite your ROOT.war, and requires the `/home/doujinsoft` directory to exist.  

## Adding .mio files

Add .mio files to the "mio" subfolder in your data directory.
The files will be consumed and added to the database.  
(This part might take a little time, benchmarks show a ~5 minutes deploy time for 2000 .mio files.)  
You can then access the Webapp and check everything's working.

## Adding Collections

Collections allow you to display a specific subset of Games to the user through a simple JSON file.  
Collection JSON files go in your data directory, subfolder "data/collections". An example file is included at the root of this project.  
Correctly written collections will be linked in the home page of the Webapp.

## More screenshots
![screen2](https://cloud.githubusercontent.com/assets/8237712/26278791/4fff292c-3da3-11e7-96be-575c8c96ab0b.png)
![screen3](https://cloud.githubusercontent.com/assets/8237712/26278792/50025d7c-3da3-11e7-947d-d87debba05c9.png)
