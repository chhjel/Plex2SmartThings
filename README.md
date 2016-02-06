# PlexPlus (with Plex2SmartThings)

**Using a Windows EXE to translate status changes in Plex Media Server and passing these status changes to a SmartThings App to control lighting and switches. Different statuses include Play, Pause, Stop & Trailer**

**Note:** This is a modified version of @ChristianH’s Excellent Plex2SmartThings. This has been re-named to PlexPlus as a fork from his script and the name taken from the "Plex + SmartThings" thread that started it all, however please note that this is functionally based on the hard work of Christian and the updates made in this version are just additional functionality as described below.

**Code and Program Location:**
https://github.com/jebbett/Plex2SmartThings/tree/PlexPlus

**PlexPlus adds:**
- Child apps for each room to allow as many rooms / scenes as required.
- This also allows matching multiple “rooms” from a single player for more complex lighting requirements.
- Additional functionality from v1.4.1, including a disable switch check and momentary switches.
- Compatibility for US and EU, and updates to make setup easier.
- Setting up devices against IP address, rather than limited to name.
- All further changes are documented in the respective Smart Apps version history.

## Requirements

- Windows Computer - A computer will need to run a program to poll Plex, so will need to be switched on when you are using Plex for this to work (This does not need to be on the same computer as the Plex server)
- [SmartThings](http://www.smartthings.com/) home automation system.
- [Plex Media Server](https://plex.tv/)

## How To Install:

### 1. Create the SmartApp

A. Go to the below URL and create a new SmartApp.

USA:  https://graph.api.smartthings.com/ide/apps 
Europe:  https://graph-eu01-euwest1.api.smartthings.com/ide/apps

B. Select “From Code” for each of the below:

#### Parent App:

i. Paste App source code with the code from the file SmartApp_Source.groovy.txt.

ii. Save and publish the app for yourself.

iii. Enable OAuth in IDE by going in to IDE > My Smart Apps > Plex Plus > App Settings > Enable OAuth.

iv. Get the Token and API Endpoint values via one of the below methods:

* Enable debugging open the app and press done and values will be returned in Live Logging in IDE.
* Open the SmartThings app and click API Information.
* Configure the app online and note down the API Token and API Endpoint values.

#### Child App:

 i. Paste App source code with the code from the file SmartApp_Child_Source.groovy.txt.

 ii. Save the App – DO NOT PUBLISH!


### 2. Configure the Plex2SmartThings Windows application

A. Download Plex2SmartThingsV2.exe, config.config and if you like the debug launchers.

B. Open the config.config file.

C. In config/smartThingsEndpoints fill in your API token and endpoint urls from step B. Be sure to keep the /statechanged/on* at the end of the urls.
  
D. If Plex and this application is not running on the same server then enter the URL to the session status page of your Plex server in the config/plexCheck/@plexStatusUrl attribute.

E. If you have Plex Pass users with PINs be sure to append your plex_token to the end of the url in the step above. (e.g. http://localhost:32400/status/sessions?X-Plex-Token='MyPlexToken'). To find your plex token follow [this guide.](https://support.plex.tv/hc/en-us/articles/204059436-Finding-your-account-token-X-Plex-Token)

F. Configure the rest of the file to your likings.

### 3. Run Plex2SmartThings

You can now run Plex2SmartThings and the SmartApp should be notified whenever any media plays on plex.

If anything isn’t working you can try enabling some extra debug output by adding the d1 or d2 arguments. E.g. running Plex2SmartThingsV2.exe d1 from the command line or running the launchers.


### 4. Configure the Smart App

Configuration should be self explanatory, however if you require device details then the App has a section call "Last Event".

If you start a video on your device you want to setup the device details will be shown here to make configuration in to the App easier, if the event is not appearing here then the config.config is not setup correctly and you will need to revisit step 2.