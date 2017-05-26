# DoujinSoft

![screen1](https://cloud.githubusercontent.com/assets/8237712/26278790/4ffd633a-3da3-11e7-865f-015cab5fef5e.png)

Java Web Application for managing and distributing WWDIY content.  
Relies on the original [DIYEdit](https://gbatemp.net/threads/warioware-d-i-y-editor.346953/) by bobmcjr for everything reverse-engineering related.  
Uses SQLite, materializeCSS and jQuery. 

## Features  

* .mio files dropped into the Webapp's data directory are parsed and added to the archive (on every app restart)
* Searchable lists with pages for every content (Games/Comics/Records)
* Comics can be directly read through the user's browser
* Records can be listened to through the browser (uses MidiJS)
* All the content available on the archive can be automatically inserted into a game save provided by the user.


## Deploying  

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
The .mio files you put in the data directory's "mio" subfolder should be consumed and added to the database.  
(This part might take a little time, benchmarks show a ~5 minutes deploy time for 2000 .mio files.)  
You can then access the Webapp and check everything's working.

## Adding Collections

Collections allow you to display a specific subset of Games to the user through a simple JSON file.  
Collection JSON files go in your data directory, subfolder "collections". An example file is included at the root of this project.  
Correctly written collections will be linked in the home page of the Webapp.

## More screenshots
![screen2](https://cloud.githubusercontent.com/assets/8237712/26278791/4fff292c-3da3-11e7-96be-575c8c96ab0b.png)
![screen3](https://cloud.githubusercontent.com/assets/8237712/26278792/50025d7c-3da3-11e7-947d-d87debba05c9.png)
