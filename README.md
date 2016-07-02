# PlexPlus (with Plex2SmartThings)

**Using either a Windows EXE or a Plex custom device type to translate status changes in Plex Media Server and passing these status changes to the Plex Plus SmartThings App to control lighting and switches. Different statuses include Play, Pause, Stop & Trailer**

**Note:** This is a modified version of @ChristianH’s Excellent Plex2SmartThings exe. 

**Code and Program Location:**
https://github.com/jebbett/Plex2SmartThings/tree/PlexPlus

**PlexPlus adds:**
- Child apps for each room to allow as many rooms / scenes as required.
- This also allows matching multiple “rooms” from a single player for more complex lighting requirements.
- Additional functionality from v1.4.1, including a disable switch check and momentary switches.
- Compatibility for US and EU, and updates to make setup easier.
- Setting up devices against IP address, rather than limited to name.
- Support for media player device type.
- All further changes are documented in the respective Smart Apps version history.

## Additional Requirements

Either:

**- Plex2SmartThings** - Windows Application - This is the most reliable method at the moment due to ST reliability issues and also should be able to handle any client and can select based on many variables. A computer will need to run a program to poll Plex, so will need to be switched on when you are using Plex for this to work (This does not need to be on the same computer as the Plex server)
**- Plex HT Manager** - Non Windows / NAS etc. - This is an app and custom device type, which uses ST to poll Plex every 10 seconds, as such is subject to ST reliability issues, but offers greater server support and easier to setup - https://community.smartthings.com/t/release-plex-home-theatre-manager-smartapp/37415

## How To Install:

### 1. Create the SmartApp

A. Go to your IDE location, the below link should re-direct you to the correct IDE location.

https://graph.api.smartthings.com/ide/apps 

B. Select “From Code” for each of the below:

#### Parent App:

i. Paste App source code with the code from the file SmartApp_Source.groovy.txt.

ii. Save and publish the app for yourself.

iii. Enable OAuth in IDE by going in to IDE > My Smart Apps > Plex Plus > App Settings > Enable OAuth.

iv. Get the Token and API Endpoint values via one of the below methods:

* Open the PlexPlus smart app and click API Information for your region, you can then send it to your computer via email from the app.
* Enable debugging, open live logging in IDE and then open the app again and press done and values will be returned in Live Logging.
* If the above does not work (usually as ST have introduced a new region) then you can just get the App ID and Token from the "Last Event / App ID / Token" screen, but you will manually need to write this down. 
* Configure the app online and note down the API Token and API Endpoint values.

#### Child App:

 i. Paste App source code with the code from the file SmartApp_Child_Source.groovy.txt.

 ii. Save the App – DO NOT PUBLISH!


### 2. Configure Your Chosen Polling Method (Choices detailled in "Additional Requirements section")

**If you have chosen Plex2SmartThings EXE continue below, otherwise follow the guide provided via the link in the "Addditional Requirements" section then resume at step 4**

A. Download Plex2SmartThingsV3.exe and config.config. (I have also put a ZIP file containing all of these in the same folder)

B. Open the config.config file.

C. In config/smartThingsEndpoints fill in your API token and add the APP ID to the endpoint urls from the previous section.

    <!ENTITY accessToken "123-123-123">
    <!ENTITY appId "abc-abc-abc">
    <!ENTITY ide "https://graph-eu01-euwest1.api.smartthings.com">
    <!ENTITY plexStatusUrl "http://localhost:32400/status/sessions">

D. Be sure to also check that your IDE URL matches the URL in config.config, if you have the URL from the app then this should be correct, if you were unable to get this from the app then you willl need to copy from IDE, it'll be somethign like "graph-na02-useast1.api.smartthings.com"
  
E. If Plex and this application is not running on the same server then enter the URL to the session status page of your Plex server in the plexStatusUrl attribute.

F. If you have Plex Pass users with PINs be sure to append your plex_token to the end of the url in the step above. (e.g. http://localhost:32400/status/sessions?X-Plex-Token='MYPLEXTOKEN'). To find your plex token follow [this guide.](https://support.plex.tv/hc/en-us/articles/204059436-Finding-your-account-token-X-Plex-Token)

G. The polling interval and debugging can also be configured, however the standard value should suffice.

### 3. Run Plex2SmartThings

You can now run Plex2SmartThings and the SmartApp should be notified whenever any media plays on plex.

If anything isn’t working you can try enabling some extra debug output by adding the d1 or d2 arguments. E.g. running Plex2SmartThingsV2.exe d1 from the command line or running the supplied launchers.


### 4. Configure the Smart App

Configuration should be self explanatory however come back here with any questions.

If using Plex2SmartThings EXE you will need to populate "Player name, User or IP" If you require device details from  then the App has a section called "Last Event" which will tell you the details of the last event recieved.


### 5. Errors / Debugging

**Plex2SmartThings.exe - SendGetRequest: the remote server returned and error: (403) Forbidden**
There is an issue with the URL in config.config re-check section 2.D

**No error in the EXE and no "Last Event" present in the App**
This is because SmartThings is not reciving an event, this is usually because the App ID or Token are incorrect, if you re-install the app these values will change and need to be setup again.

**"Last Event" in the app, but hasn't triggered the lights to change.**
This is likely to that the "Room" is not configured correctly, re-check the name or IP you have setup

**Live Logging - java.lang.NullPointerException: Cannot get property 'authorities' on null object @ line xx**
You have not enabled OAuth in the parent app, go to Section 1.B.iii 

**I've played a video via Plex but no "Last Event" is showing**
If the event is not appearing here then the config.config is not setup correctly and you will need to revisit step 2.