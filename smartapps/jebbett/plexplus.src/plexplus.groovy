import groovy.json.JsonBuilder

/**
 *  Plex Plus
 *
 *  Copyright 2016 Christian Hjelseth / Jake Tebbett
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * 
 * VERSION CONTROL - Plex Plus
 * ###############
 *
 *  v3.0 - Combined everything in to a single app, old app will need to be removed and re-installed.
 *	v3.1 - Added support for Plex Webhook (Plex Pass users only)
 *	v3.2 - Updated Live Logging, Cosmetics for WebHook and addition of matching 2 criteria.
 *	v3.3 - Added bulb temperature for color bulbs
 *	v3.4 - Added control for switches to only react to 'Play' and added Routine triggers
 *	v3.5 - Added scrobble as onplay
 *	v3.6 - Added error handling for token creation
 *	v3.7 - Fixed the use of wildcards
 *
 */

definition(
    name: "PlexPlus",
    namespace: "jebbett",
    author: "Christian Hjelseth & Jake Tebbett",
    description: "Allows web requests to dim/turn off/on lights when plex is playing",
    category: "My Apps",
    parent: parent ? "jebbett:PlexPlus" : null,
    iconUrl: "https://github.com/jebbett/Plex2SmartThings/raw/PlexPlus/icon.png",
    iconX2Url: "https://github.com/jebbett/Plex2SmartThings/raw/PlexPlus/icon.png",
    iconX3Url: "https://github.com/jebbett/Plex2SmartThings/raw/PlexPlus/icon.png",
    oauth: [displayName: "PlexServer", displayLink: ""])


def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
	if(parent){
       	state.catcherRunning = false
    	subscribe(playerDT, "status", PlayerDTCommandRecieved)
        logWriter("App settings: ${settings}\nLast Event:\n${parent.state.lastEvent}")
    }else{
        if (!state.accessToken) {
            createAccessToken()
        }
        
        logWriter("URL FOR USE IN PLEX2SMARTTHINGS EXE:\n"+
        		"<!ENTITY accessToken '${state.accessToken}'>\n"+
				"<!ENTITY appId '${app.id}'>\n"+
				"<!ENTITY ide '${getApiServerUrl()}'>\n"+
				"<!ENTITY plexStatusUrl 'http://localhost:32400/status/sessions?X-Plex-Token=INSERTTOKENHERE'>")
        
        logWriter("URL FOR USE IN PLEX WEBHOOK:\n${getApiServerUrl()}/api/smartapps/installations/${app.id}/pwh?access_token=${state.accessToken}")
        if(state.lastEvent == null){state.lastEvent = "No event recieved, please ensure that config.config is setup correctly"}
	}
}

preferences {
	page name: "mainMenu"
    page name: "parentPage"
    page name: "childPage"
    //Parent Pages
    page name: "lastEvt"
    page name: "P2ST"
    page name: "instructions"
    page name: "pageDevice"
    page name: "pageDevDetails"
    page name: "pageDevDelete"
    page name: "pageDevAdd"
    //Child Pages
    page name: "pageDoThis"
    page name: "pageWhenThis"
    page name: "pageMediaSettings"
}

mappings {
  path("/statechanged/:command") 	{ action: [ GET: "OnCommandRecieved" ] }
  path("/appinfo") 					{ action: [ GET: "appInfoJson"]   }
  path("/appinfo2") 				{ action: [ GET: "appInfoJson2"]   }
  path("/appinfo3") 				{ action: [ GET: "appInfoJson3"]   }
  path("/pwh") 						{ action: [ POST: "plexWebHookHandler"] }
}

def mainMenu() {
	parent ? childPage() : parentPage()
}

def parentPage() {

    dynamicPage(name: "parentPage", title: "Rooms", install: true, uninstall: true, submitOnChange: true) {              
       
        section {
            app(name: "childapp", appName: "PlexPlus", namespace: "jebbett", title: "Create New Room", multiple: true)
            }	
        
        section() { 
        	href(name: "instructions", title: "Instructions", required: false, page: "instructions", description: "how to link this app to Plex!")
    	}
        section() { 
        	href(name: "pageDevice", title: "Create Virtual Device", required: false, page: "pageDevice", description: "create a virtual device here")
    	}
		
        try { if (!state.accessToken) {createAccessToken()} }
		catch (Exception e) {log.info "Unable to create access token, OAuth has probably not been enabled: $e"}
	    

        // Enables logging debug only when enabled
        section(title: "ADVANCED") {
       		paragraph "If you experiencing issues please enable logging to help troubleshoot"
            input "debugLogging", "bool", title: "Debug Logging...", required: false, defaultValue: false, submitOnChange: true
            href(name: "usersBal", title: "Last Event", required: false, page: "lastEvt", description: "view last event recieved by app")
            href(name: "P2STd", title: "Plex2SmartThings Setup Details", required: false, page: "P2ST", description: "information for setting up the Plex2SmartThings program if used")
    	}
    }
}

def childPage() {
    dynamicPage(name: "childPage", uninstall: true, install: true) {
        section() {
                label title: "Enter Room Name", defaultValue: app.label, required: false
        }
        section ("When this happens"){
        	href(name: "pageWhenThis", title:"When Event Comes From", description: "", page: "pageWhenThis", required: false, image: "https://cdn0.iconfinder.com/data/icons/round-ui-icons/128/tick_blue.png")      

        	href(name: "pageDoThis", title:"Trigger these actions", description: "", page: "pageDoThis", required: false, image: "https://cdn0.iconfinder.com/data/icons/round-ui-icons/128/favourite_blue.png")      

        	href(name: "pageMediaSettings", title:"With these settings", description: "", page: "pageMediaSettings", required: false, image: "https://cdn0.iconfinder.com/data/icons/round-ui-icons/128/setting_blue.png")      
      	}
    }
}

def instructions() {
    dynamicPage(name: "instructions", title: "Instructions", install: false, uninstall: false) {        
        section(title: "Choose a method of linking to Plex") {
        	paragraph "There are currently three methods of linking Plex to SmartThings"
        }
        
        section(title: "1. Plex2SmartThings program on your Plex server")
        section(title: "This is the preferred method if your Plex server runs on a windows computer, as most of the work is done by a program that runs on the server and avoids reliance of ST to constantly monitor your plex server, however does require some minor technical ability. This is using the windows application created by ChristianH and modified by EntityXenon")
        section(title: "2. Plex WebHook (Plex Pass Only)")
        section(title: "If you have plex pass then this is the recommended method, you will need to get the webhook URL from live logging, and set this up in your plex server.")
        section(title: "3. Custom Device Type for Plex")
        section(title: "This is the only method if your Plex server is not running on a windows computer as works with all variants of Plex Server, but does rely on SmartThings checking the Plex status every 10 seconds. This is using the Device Handler and smart app created by iBeech.")
   }
}

private getSortedDevices() {
	return getChildDevices().sort{it.displayName}
}

def pageDevice() {
    dynamicPage(name: "pageDevice", title: "Create Device", install: false, uninstall: false) {        
        section() {
          def childDevs = []
          def i = 1 as int
            getSortedDevices().each { dev ->
                href(name: "pageDevDetails$i", title:"$dev.label", description: "$dev.deviceNetworkId", params: [devi: dev.deviceNetworkId, devstate: dev.statusState?.value], page: "pageDevDetails", state: "complete", required: false, image: "https://cdn0.iconfinder.com/data/icons/round-ui-icons/128/setting_blue.png")
                i++
        	}
        }
        section(){
            href(name: "pageDevDetails", title:"Create New Device", description: "Please ensure the custom device type is also installed", params: [devi: false], page: "pageDevDetails", image: "https://cdn0.iconfinder.com/data/icons/round-ui-icons/128/settings_red.png")
        }
    }
}

def pageDevDetails(params) {
    dynamicPage(name: "pageDevDetails", title: "Device Details", install: false, uninstall: false) {
		if(params.devi){
			section("Name & Status") {
        		paragraph("${params.devi}")
                paragraph("${params.devstate}")
            }
            section("DELETE") {
            	href(name: "pageDevDelete", title:"DELETE DEVICE", description: "ONLY PRESS IF YOU ARE SURE!", params: [devi: "$params.devi"], page: "pageDevDelete", required: false, image: "https://cdn0.iconfinder.com/data/icons/round-ui-icons/128/close_red.png")
        	}
   		}else{
       		section() {
        		paragraph("Create a new Plex+ Reporting Device")
                input "devName", type: "text", title: "Name:", required:false, submitOnChange: true, defaultValue: "New Plex Plus Device"
            	href(name: "pageDevAdd", title:"Create Device", description: "", params: [devi: "$params.devi"], page: "pageDevAdd", required: false, image: "https://cdn0.iconfinder.com/data/icons/round-ui-icons/128/add_green.png")
        	}
		}        
   }
}

def pageDevAdd(params) {
	if(settings.devName){
    	def DeviceID = "PlexPlusDev:"+settings.devName
		def existingDevice = getChildDevice(DeviceID)
		if(!existingDevice) {
        	def newTrigger = addChildDevice("jebbett", "Plex Plus Device", DeviceID, null, [name: settings.devName, label: settings.devName])
		}
        pageDevice()
	}else{
    	dynamicPage(name: "pageDevAdd", title: "Device Details", install: false, uninstall: false) {        
			section() {
            	paragraph("Name not set")
        	}
		}
	}
}

def pageDevDelete(params) {
    deleteChildDevice(params.devi)
	pageDevice()
}

def lastEvt() {
    dynamicPage(name: "lastEvt", title: "Last Event", install: false, uninstall: false) {        
        section(title: "Details of Last Event Recieved") {
        	paragraph "$state.lastEvent"
        }
    }
}

def P2ST() {
    dynamicPage(name: "P2ST", title: "Plex2SmartThings Information", install: false, uninstall: false) {
    	section(title: "App ID") {
        	paragraph "$app.id"
        }
        
        section(title: "Access Token") {
        	paragraph "$state.accessToken"
        }
        
        section(title: "API Information") { 
        	href url: "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/appinfo?access_token=${state.accessToken}",
            		style:"embedded", required:false, title:"API Info US Standard", description: "Tap to view Info"
            
            href url: "https://graph-na02-useast1.api.smartthings.com/api/smartapps/installations/${app.id}/appinfo3?access_token=${state.accessToken}",
            		style:"embedded", required:false, title:"API Info US East", description: "Tap to view Info"
    	
        	href url: "https://graph-eu01-euwest1.api.smartthings.com/api/smartapps/installations/${app.id}/appinfo2?access_token=${state.accessToken}", 
            		style:"embedded", required:false, title:"API Info Europe", description: "Tap to view Info"
    	}
    }
}


// These Methods Generate Json for you Info Only
def appInfoJson() {
	def configJson = new groovy.json.JsonOutput().toJson([
    	accessToken:  state.accessToken,
        appId:        app.id,
		ide:		"https://graph.api.smartthings.com"
        
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

def appInfoJson2() {
	def configJson = new groovy.json.JsonOutput().toJson([
    	accessToken:  state.accessToken,
        appId:        app.id,
    	ide: 	"https://graph-eu01-euwest1.api.smartthings.com",
  
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

def appInfoJson3() {
	def configJson = new groovy.json.JsonOutput().toJson([
    	accessToken:  state.accessToken,
        appId:        app.id,
		ide:		"https://graph-na02-useast1.api.smartthings.com"        
        
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}



def OnCommandRecieved() {
	def command = params.command
	def userName = params.user
	def playerName = params.player
    def playerIP = params.ipadd
	def mediaType = params.type
	logWriter("Command Recieved")
	// If Plex2SmartThings.exe has not been modified to return IP Address (ipadd) then return No IP to stop false triggering.
    if (playerIP == null) {
        playerIP = "No IP Returned"
    }
    
	childApps.each { child ->
    	child.AppCommandRecieved(command, userName, playerName, playerIP, mediaType)
    }
	return
}

def StoreLastEvent(command, userName, playerName, playerIP, mediaType) {

	state.lastEvent = " User Name: $userName \n Player Name: $playerName \n IP Address: $playerIP \n Command: $command \n Media Type: $mediaType"
	return
}

//// CHILD CODE

def pageWhenThis(){
	dynamicPage(name: "pageWhenThis", uninstall: false) {
    	section("When Plex2SmartThings or Plex WebHook sends and event matching:") {
            input(name: "playerA1", type: "text", title: "Player name, User or IP", required:false)
            input(name: "playerB1", type: "text", title: "Player name, User or IP 2", required:false)
            input(name: "matchBoth", type: "bool", title: "Trigger only if both the above are found", required: false)
            paragraph "The above are case sensitive, and IP can't be used for Plex WebHook"
        }
        section("Or when a media player device changes state:") {
            input(name: "playerDT", type: "capability.musicPlayer", title: "ST Media Player Device", multiple: false, required:false)
        }
        section("Notes"){
        	paragraph "To identify player, you can use either Player Device Name, Username, IP address or * in order to match any player where using the Plex2SmartThings program on your computer. \n\nOr you can use a supported media player device type."
       }
	}
}

def pageDoThis(){
	dynamicPage(name: "pageDoThis", uninstall: false) {
        section("Lights") {
			input "dimmers1", "capability.switchLevel", title: "Adjust level of these bulbs", multiple: true, required: false, submitOnChange: true
            input "hues1", "capability.colorControl", title: "Adjust level and color of these bulbs", multiple:true, required:false, submitOnChange: true
            if(hues1||dimmers1) {
            input(name: "iLevelOnPlay1", type: "number", title: "Level on Play", defaultValue:0)
            input(name: "iLevelOnPause1", type: "number", title: "Level on Pause", defaultValue:30)
            input(name: "iLevelOnStop1", type: "number", title: "Level on Stop", defaultValue:100)
            }
            if(hues1) {
				input "colorOnPlay", "enum", title: "Hue Bulbs > Color On Play", required: false, multiple: false, submitOnChange: true,
					options: ["Soft White", "White", "Daylight", "Warm White", "Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"]
                input "colorOnPause", "enum", title: "Hue Bulbs > Color On Pause", required: false, multiple: false, submitOnChange: true,
					options: ["Soft White", "White", "Daylight", "Warm White", "Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"]
                input "colorOnStop", "enum", title: "Hue Bulbs > Color On Stop", required: false, multiple: false, submitOnChange: true,
					options: ["Soft White", "White", "Daylight", "Warm White", "Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"]
                input(name: "tempOnPlay", description: "1000..9999", type: "number", range: "1000..9999", title: "Color Temperature on Play (°K)", required: false)
                input(name: "tempOnPause", description: "1000..9999", type: "number", range: "1000..9999", title: "Color Temperature on Pause (°K)", required: false)
                input(name: "tempOnStop", description: "1000..9999", type: "number", range: "1000..9999", title: "Color Temperature on Stop (°K)", required: false)
            }
            input(name: "bDimOnlyIfOn1", type: "bool", title: "Dim bulbs only if they're already on", required: false)
        }
		section("Switches") {
        	input "switches2", "capability.switch", title:"Switches On when Playing", multiple: true, required: false
            input "switches1", "capability.switch", title:"Switches Off when Playing", multiple: true, required: false
            input(name: "bReturnState1", type: "bool", title: "Switches return to original state when Stopped", required: false)
            input(name: "bSwitchOffOnPause1", type: "bool", title: "Switches use Play config when Paused", required: false)
            input(name: "switchOnPlay", type: "bool", title: "Switches only change on 'Play'", required: false)
            paragraph "The below switches do not toggle off when state becomes inactive, ideal for tiggering external App scenes"
            input "mSwitchPlay", "capability.switch", title:"Momentary switch on Play", multiple: true, required: false
            input "mSwitchPause", "capability.switch", title:"Momentary switch on Pause", multiple: true, required: false
            input "mSwitchStop", "capability.switch", title:"Momentary switch on Stop", multiple: true, required: false
            
        }
		section("Modes") {
			input "playMode1", "mode", title: "Mode when playing", required:false
			input "pauseMode1", "mode", title: "Mode when paused", required:false
			input "stopMode1", "mode", title: "Mode when stopped", required:false
		}
        section("Routines") {
        	def actions = location.helloHome?.getPhrases()*.label
        	input "playRoutine", "enum", title: "Routine when playing", required: false, options: actions
            input "pauseRoutine", "enum", title: "Routine when paused", required: false, options: actions
            input "stopRoutine", "enum", title: "Routine when stopped", required: false, options: actions
        }
        section("Update This Device"){
        	input(name: "PlexPlusDT", type: "capability.musicPlayer", title: "Update This Device", description: "Use a Plex Plus or HT Custom Device", multiple: false, state: null, required:false)
        }
	}
}

def pageMediaSettings(){
	dynamicPage(name: "pageMediaSettings", uninstall: false) {
    	section("Media Settings") {	
			input(name: "bTreatTrailersAsPause1", type: "bool", title: "Use pause config for movie trailers", required: false)
            input(name: "stopDelay", type: "number", title: "Delay stop action", required:false, defaultValue:0)
            input(name: "pauseDelay", type: "number", title: "Delay pause action", required:false, defaultValue:0)
		}
        section("Restrictions") {
			input "mediaTypeOk", "enum", title: "Only for media types:", multiple: true, submitOnChange: true, required: false,
			options: ['movie', 'episode', 'clip', 'track']
        	input "disabled", "capability.switch", title: "Switch to disable when On", required: false, multiple: false
            input "activeMode", "mode", title: "Only run in selected modes", multiple: true, required:false
        }
	}
}

// Recieve command from MusicPlayer device type
def PlayerDTCommandRecieved(evt){
	//If no device type configured do not run the below code
	if(!playerDT){return}
    
	if(evt.value=="playing"){AppCommandRecieved("onplay", "Unknown", playerDT,"ST Media Player Device", playerDT.currentplaybackType)}
	else if(evt.value=="stopped"){AppCommandRecieved("onstop", "Unknown", playerDT,"ST Media Player Device", playerDT.currentplaybackType)}
    else if(evt.value=="paused"){AppCommandRecieved("onpause", "Unknown", playerDT,"ST Media Player Device", playerDT.currentplaybackType)}
}

def plexWebHookHandler(){    
    def jsonSlurper = new groovy.json.JsonSlurper()
	def plexJSON = jsonSlurper.parseText(params.payload)
    
    //logWriter "Player JSON: ${plexJSON.Metadata}"
    logWriter "Player JSON: ${plexJSON.Player}"
    logWriter "Account JSON: ${plexJSON.Account}"
    logWriter "Account JSON: ${plexJSON.event}"
    
    def command = ""
	def userName = plexJSON.Account.title
	def playerName = plexJSON.Player.title
    def playerIP = plexJSON.Player.publicAddress
	def mediaType = plexJSON.Metadata.type
    // change command to right format
    switch(plexJSON.event) {
		case ["media.play","media.resume","media.scrobble"]:		command = "onplay"; 	break;
        case "media.pause":						command = "onpause"; 	break;
        case "media.stop":						command = "onstop"; 	break;
        return
    }
    // send to child apps
    childApps.each { child ->
    	child.AppCommandRecieved(command, userName, playerName, playerIP, mediaType)
    }

}

def AppCommandRecieved(command, userName, playerName, playerIP, mediaType) {
log.warn "THIS IS THE MEDIA TYPE: $mediaType COMMAND: $command"
//Log last event
	parent.StoreLastEvent(command, userName, playerName, playerIP, mediaType)
	
//Check if room found
	def allowedDevs = ["$playerIP", "$playerName", "$userName"]
    
    log.debug "$settings.playerA1 is not in $allowedDevs"
    
    if (settings?.playerA1 == "*"){logWriter ("Player 1 Wildcard Match")}
    else if (allowedDevs.contains("${playerA1}") && !matchBoth){logWriter ("Player 1 Match")}
    else if (allowedDevs.contains("${playerB1}") && !matchBoth){logWriter ("Player 2 Match")}
    else if (matchBoth && allowedDevs.contains("${playerA1}") && allowedDevs.contains("${playerB1}")){logWriter ("Player Combination Match")}
    else if ("$playerIP" == "ST Media Player Device"){logWriter ("ST Device Type Match")}
    else{logWriter ("No match found for room"); return}
    
// Stop running if disable switch is activated    
    if (disabled != null) {if(disabled.currentSwitch == "on") {logWriter ("Disabled via switch"); return}}
    if (activeMode != null && !activeMode.contains(location.mode)) {logWriter ("Disabled via invalid mode"); return}

// Check if Media Type is correct
	if(mediaTypeOk){
		def mediaTypeFound = mediaTypeOk.find { item -> item == mediaType}
    	if(mediaTypeFound == null) {logWriter ("Match NOT found for media type: ${mediaType}"); return}
	}
    
//Translate play to pause if bTreatTrailersAsPause is enabled for this room
    if(settings?.bTreatTrailersAsPause1 && mediaType == "clip" && command == "onplay") {command = "onpause"}

// Unschedule delays
	unschedule(StopCommand)
    unschedule(PauseCommand)

// Send media type to Plex Plus Device Type if configured.
	try { settings.PlexPlusDT?.playbackType("${mediaType}") }
	catch (Exception e) {log.info "Playback Type Not Supported: $e"}
// Play, Pause or Stop
    if (command == "onplay") {
    	logWriter ("Playing")
        PlayCommand()
    }
    else if (command == "onpause") {        
        logWriter ("Paused")
        if(!settings?.pauseDelay || pauseDelay == "0"){
        	PauseCommand()
        }else{
            logWriter ("Pause Action Delay")
        	runIn(settings?.pauseDelay.value, PauseCommand)
    	}
    }
    else if (command == "onstop") {
        logWriter ("Stopped")
        if(!settings?.stopDelay || stopDelay == "0"){
        	StopCommand()
        }else{
           	logWriter ("Stop Action Delay")
        	runIn(settings?.stopDelay.value, StopCommand)
        }
    }
}

def PlayCommand(){
	if(!state.catcherRunning){
        catchState("switches1")
    	catchState("switches2")
        state.catcherRunning = true
    }
    if(settings?.playMode1){setLocationMode(playMode1)}
	SetLevels(iLevelOnPlay1, colorOnPlay, tempOnPlay)
    SetSwitchesOff()
    mSwitchPlay?.on()
    PlexPlusDT?.play()
    if(playRoutine) { location.helloHome?.execute(playRoutine) }
}

def PauseCommand(){
    if(settings?.pauseMode1){setLocationMode(pauseMode1)}
   	SetLevels(iLevelOnPause1, colorOnPause, tempOnPause)
    mSwitchPause?.on()
    PlexPlusDT?.pause()
    if(pauseRoutine) { location.helloHome?.execute(pauseRoutine) }
    if(settings?.bSwitchOffOnPause1) {
   		SetSwitchesOff()
    } else {
       	if(state.catcherRunning && settings?.bReturnState1){
       		returnToState("switches1")
   			returnToState("switches2")
           	state.catcherRunning = false
       	}else{
       		SetSwitchesOn()
           	state.catcherRunning = false
       	}
    }
}

//Stop command
def StopCommand(){

	if(settings?.stopMode1){setLocationMode(settings?.stopMode1)}
    SetLevels(iLevelOnStop1, colorOnStop, tempOnStop)
    mSwitchStop?.on()
    PlexPlusDT?.stop()
    if(stopRoutine) { location.helloHome?.execute(stopRoutine) }
    if(state.catcherRunning && settings?.bReturnState1){
       	returnToState("switches1")
    	returnToState("switches2")
        state.catcherRunning = false
    }else{
       	SetSwitchesOn()
        state.catcherRunning = false
    }
}

// Actions
def SetSwitchesOn() {
	if(!switchOnPlay){
		switches1?.on()
    	switches2?.off()
    }
}
def SetSwitchesOff() {
	switches1?.off()
    switches2?.on()
}

def SetLevels(level, acolor, temp) {
	// If color specified set hues
    if (level != null) {
    	def hueColor = 23
		def saturation = 56
		switch(acolor) {
			case "White":
				hueColor = 52
				saturation = 19
				break;
			case "Daylight":
				hueColor = 53
				saturation = 91
				break;
			case "Soft White":
				hueColor = 23
				saturation = 56
				break;
			case "Warm White":
				hueColor = 20
				saturation = 80 //83
				break;
			case "Blue":
				hueColor = 70
				break;
			case "Green":
				hueColor = 35
				break;
			case "Yellow":
				hueColor = 25
				break;
			case "Orange":
				hueColor = 10
				break;
			case "Purple":
				hueColor = 75
				break;
			case "Pink":
				hueColor = 83
				break;
			case "Red":
				hueColor = 100
				break;
		}
        
        if (settings?.bDimOnlyIfOn1){
        	if(acolor != null){ 	hues1?.each 	{ hue -> if ("on" == hue.currentSwitch) 	{ hue.setColor([hue: hueColor, saturation: saturation, level: level]) } } }
            else if(temp != null){ 	hues1?.each 	{ hue -> if ("on" == hue.currentSwitch) 	{ hue.setColorTemperature(temp) } } }
            else {					hues1?.each 	{ hue -> if ("on" == hue.currentSwitch) 	{ hue.setLevel(level) } } }
            
        							dimmers1?.each 	{ bulb -> if ("on" == bulb.currentSwitch) 	{ bulb.setLevel(level) } }
            
        }else{
        	// color takes priority over temperature, dimmers will still set temperature if available
        	if(acolor != null){		hues1?.setColor([hue: hueColor, saturation: saturation, level: level]) }
            else if(temp != null){	hues1?.setColorTemperature(temp) }

            						dimmers1?.setLevel(level)
        }
	}
}

//Save state
private catchState(switches) {
        settings."${switches}"?.each { switcher -> state."${switcher.id}State" = switcher.currentValue("switch")
        	logWriter (switcher.currentValue("switch"))
        }
}
//Return to state
private returnToState(switches) {
	settings."${switches}"?.each {switcher -> 
    	if(state."${switcher.id}State" == "on") {switcher.on()}
        if(state."${switcher.id}State" == "off") {switcher.off()}
    }
}


//// GENERIC CODE

private def logWriter(value) {
    if(parent){
    	if(parent.debugLogging) {log.debug "PlexPlus [${app.label}] >> ${value}"}
    }else{
		if(debugLogging) {log.debug "PlexPlus [PARENT] >> ${value}"}
    }
}