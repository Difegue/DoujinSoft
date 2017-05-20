# DoujinSoft

![screen1](https://cloud.githubusercontent.com/assets/8237712/26278790/4ffd633a-3da3-11e7-865f-015cab5fef5e.png)

Java Web Application for managing and distributing WWDIY content.  
Based on the original DIYEdit by bobmcjr for everything reverse-engineering related.  
Uses SQLite, materializeCSS and jQuery. 

## Features  

* .mio files dropped into the Webapp's data directory are parsed and added to the archive (on every app restart)
* Searchable lists with pages for every content (Games/Comics/Records)
* Comics can be directly read through the user's browser
* Records can be listened to through the browser (uses MidiJS)
* All the content available on the archive can be automatically inserted into a game save provided by the user.


## Deploying  

Get the [release WAR](https://github.com/Difegue/DoujinSoft/releases) (or just build it with maven from the sources)  
Edit your tomcat 
Drop the WAR into your tomcat webapps directory to start deployment.  
The .mio files you put in the data directory's "mio" subfolder should be parsed and added to the database.  
You can then access the Webapp and check everything's working.

## More screenshots
![screen2](https://cloud.githubusercontent.com/assets/8237712/26278791/4fff292c-3da3-11e7-96be-575c8c96ab0b.png)
![screen3](https://cloud.githubusercontent.com/assets/8237712/26278792/50025d7c-3da3-11e7-947d-d87debba05c9.png)
