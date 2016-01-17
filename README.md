# PlexPlus (with Plex2SmartThings)

Note: This is a modified version of ChristianH’s Excellent Plex2SmartThings. This has been re-named to PlexPlus to allow trialling this app alongside the original, however please note that this is functionally based on the hard work of Christian and the updates made in this version are just additional functionality as described below.

Integration from Plex to SmartThings, notifying a smartapp when media starts playing, pauses or stops on Plex.

PlexPlus adds:
- Child apps for each room to allow as many rooms / scenes as required.
- This also allows matching multiple “rooms” from a single player for more complex lighting requirements.
- Additional functionality from v1.4.1, including a disable switch check and momentary switches.

## Requirements

- [SmartThings](http://www.smartthings.com/) home automation system.
- [Plex Media Server](https://plex.tv/)

## How to install it

### 1. Create the SmartApp

A. Go to the below URL and create a new SmartApp.

USA:  https://graph.api.smartthings.com/ide/apps 
Europe:  https://graph-eu01-euwest1.api.smartthings.com/ide/apps

B. Select “From Code” for each of the below:

#### Parent App:

       i. Paste App source code with the code from the file SmartApp_Source.groovy.txt.

       ii. Save and publish the app for yourself.

       iii. Enable OAuth by going in to App Settings > Enable OAuth.

       iv. Either configure the app online and note down the API Token and API Endpoint values, or install the app through the SmartThings app and click API Information to find the token and endpoints.

#### Child App:

       i. Paste App source code with the code from the file SmartApp_Child_Source.groovy.txt.

       ii. Save the App – DO NOT PUBLISH!


### 2. Configure the Plex2SmartThings application

Use version 2 of the Plex2SmartThings EXE if you also want to be able to identify player by IP.


A. Compile the Plex2SmartThings application or download pre-compiled

B. Open the config.config file.

C. In config/smartThingsEndpoints fill in your API token and endpoint urls. Be sure to keep the /statechanged/on* at the end of the urls.

This information can be obtained directly for “API Information” in the app, but ensure you copy the correct USA or Europe address depending on your location.  

4. If Plex and this application is not running on the same server then enter the URL to the session status page of your Plex server in the config/plexCheck/@plexStatusUrl attribute.

5. If you have Plex Pass users with PINs be sure to append your plex_token to the end of the url in the step above. (e.g. http://localhost:32400/status/sessions?X-Plex-Token='myPlexToken'). To find your plex token follow [this guide.](https://support.plex.tv/hc/en-us/articles/204059436-Finding-your-account-token-X-Plex-Token)

6. Configure the rest of the file to your likings.

### 3. Run Plex2SmartThings

You can now run Plex2SmartThings and the SmartApp should be notified whenever any media plays on plex.

If anything isn’t working you can try enabling some extra debug output by adding the d1 or d2 arguments. E.g. running Plex2SmartThingsV2.exe d1 from the command line.

