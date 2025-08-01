# DoujinSoft

[![Docker Pulls](https://img.shields.io/docker/pulls/difegue/doujinsoft.svg)](https://hub.docker.com/r/difegue/doujinsoft/)

![screen1](https://cloud.githubusercontent.com/assets/8237712/26278790/4ffd633a-3da3-11e7-865f-015cab5fef5e.png)

Java Web Application for managing and distributing WWDIY content.  
Relies on [DIYEdit](https://github.com/xperia64/DIYEdit) and [Mio-Micro](https://github.com/yeahross0/Mio-Micro).  
Uses SQLite, materializeCSS and jQuery.  

## Features  

* .mio files dropped into the Webapp's data directory are parsed and added to the archive (on every app restart)
* Searchable lists with pages for every content (Games/Comics/Records)
* Comics can be directly read through the user's browser
* Records can be listened to through the browser (uses [mio-midi](https://www.npmjs.com/package/mio-midi))  
  - Midi files are played back using a custom soundfont based on [this one](https://musical-artifacts.com/artifacts/490).  
* All the content available on the archive can be automatically inserted into a game save provided by the user.
* Basic JSON API.
* `iframe` support so you can embed WarioWare DIY content on your own webpage.  

## Using the API

Adding `&format=json` to most search pages in the webapp will give you a JSON equivalent. Use at will! 🙋‍♂️  

## Yonderu! DoujinSoft API

The `/yonderu` endpoint is slightly more specialized for use with the companion comic apps for Playdate and the Pebble watch. The following capabilities are available:  

`GET /yonderu?id=xxxxx` - Return a Yonderu JSON for the given MIO hash, if it's in the server's storage.  
`GET /yonderu?random` - Return a Yonderu JSON for a random comic MIO in the database.  
`GET /yonderu?daily` - Return a Yonderu JSON for today's comic -- Daily comics are stored in a `yonderu.txt` file at the data directory root with 366 lines, one per day. (leap years included)  
`POST /yonderu?id=xxxxx&stars=(1-5)&comment=(1-8)` - Rate a given comic MIO.  

Yonderu JSONs follow this spec:  
```
{
    "id": "adf06f38c9d5399497a8d5314c83c40d", // mio hash
    "name": "My comix", // Comic name
    "date": "06/06/2009", // date of publication in DD/MM/YYYY format
    "creator": "dfug", // Comic creator
    "brand": "TVC-16", // WWDIY Brand for the creator
    "description": "Description of the comic",
    "logo": 2, // WWDIY logo for the comic
    "colorLogo": 0, // color of the logo
    "color": 0, // color of the comic icon itself
    "pages": [ 
        "RLE-encoded data for each page of the comic",
        "The encoding is a simple schema where each line of the image is represented by a line in the string",
        "with the number of pixels of a given black or white color in sequence. For example:",
        "63W2B25W2B66WB33W\n means the first line had 63 white pixels, then 2 black ones, then 25 white pixels, etc",
    ]
}
```

Any errors will output the following basic JSON:  

```
{
    "error" : "Something happened" // Error detail
}
```

## Environment variables

* WII_NUMBER: Wii Friend Code associated to the DoujinSoft WiiConnect24 account.
* WII_FALLBACK: Wii Friend Code that will receive Wii Mail forwarded from DoujinSoft.
* WC24_SERVER: WiiConnect24 server URL. Currently, there's only really `rc24.xyz`...
* WC24_PASSWORD: Password for the DoujinSoft WiiConnect24 account
* WC24_DEBUG: Add this variable to log additional Wii Mail data.  
* DSOFT_PASS: Password for the Admin console.
* WEBHOOK_URL: URL for a webhook. This hook is hit whenever new content is uploaded to DoujinSoft.

## Deploy/develop on Codespaces

You can use [GitHub Codespaces](https://github.com/Difegue/DoujinSoft/codespaces) to develop on the app using the provided configuration.  
Once in the codespace, you should be able to use `npm run dev-server` to immediately build/start a DoujinSoft instance.

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
