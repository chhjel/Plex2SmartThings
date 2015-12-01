# Plex2SmartThings
Integration from Plex to SmartThings, notifying a smartapp when media starts playing (or pauses/stops) on Plex.

## Requirements

- [SmartThings](http://www.smartthings.com/) home automation system.
- [Plex Media Server](https://plex.tv/)

## How to install it

### 1. Create the SmartApp

1. Go to https://graph.api.smartthings.com/ide/apps and create a new SmartApp.

2. Fill out the required fields.

3. Replace the generated app source code with the code from the file SmartApp_Source.groovy.txt.

4. Save and publish the app for yourself.

5. Either configure the app online and note down the API Token and API Endpoint values, or install the app through the SmartThings app and click Api Information to find the token and endpoints.
![](https://lh6.googleusercontent.com/IkYz19RC2T47L9kIaROifhE9-U1qY1dUKfvIpfSSZmph8kW-UAYnhDA_3TcYKXZ74PuCu8fqAAjusHkDFoxNSjSscsoFL2QPYJTGIh4UUNOLh6_vJzxY3kU9mCc8qid4VaVoXXk?raw=true)

### 2. Configure the Plex2SmartThings application

1. Compile the Plex2SmartThings application.

2. Open the config.config file.

3. In config/smartThingsEndpoints fill in your API token and endpoint urls. Be sure to keep the /statechanged/on* at the end of the urls.

4. If plex and this application is not running on the same server then enter the url to the session status page of your plex server in the config/plexCheck/@plexStatusUrl attribute.

5. If you have Plex Pass users with PINs be sure to append your plex_token to the end of the url in the step above. (e.g. http://localhost:32400/status/sessions?X-Plex-Token='myPlexToken'). To find your plex token follow [this guide.](https://support.plex.tv/hc/en-us/articles/204059436-Finding-your-account-token-X-Plex-Token)

6. Configure the rest of the file to your likings.

### 3. Run Plex2SmartThings

You can now run Plex2SmartThings and the SmartApp should be notified whenever any media plays on plex.

If anything isn’t working you can try enabling some extra debug output by adding the d1 or d2 arguments. E.g. running Plex2SmartThings.exe d1 from the command line.

