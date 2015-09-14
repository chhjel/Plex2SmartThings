/**
 *  Plex Integration
 *
 *  Copyright 2015 Christian Hjelseth
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
 *  v1.2 - Added ability to view api info with the App.  Also added ability to toggle debug logging on and off (@tonesto7)
 *			Added ability to set certain modes only to trigger switches
 *  -------------------------------------
 *  v1.1 - Added optional mode changes
 */
 
import groovy.json.JsonBuilder

definition(
    name: "${textAppName()}",
    namespace: "${textNamespace()}",
    author: "${textAuthor()}",
    description: "${textDesc()}",
    category: "My Apps",
    iconUrl: "http://i.imgur.com/38Y5PbU.png",
    iconX2Url: "http://i.imgur.com/38Y5PbU.png",
    iconX3Url: "http://i.imgur.com/38Y5PbU.png",
    oauth: [displayName: "PlexServer", displayLink: ""])

//--------Change App Variables Here!!!--------------
//Change this to rename the Default App Name
def appName() { "Plex Integration" }
//Pretty Self Explanatory
def appAuthor() { "ChristianH" }
//So is this...
def appNamespace() { "Christian Hjelseth" }
//Also this one...
def appDescription() { "Allows web requests to dim/turn off/on lights when plex is playing." }
//This one too...
def appVersion() { "1.2" }
//Definitely this one too!
def versionDate() { "9-13-2015" }
//--------------------------------------------------


preferences {
	page(name: "configPage") 
}

/*  Main Configuration Page  */
def configPage() {
	if (!state.accessToken) {
    	createAccessToken()
   	}
    if (state.debugLogging == null) { state.debugLogging = false }
    
	dynamicPage(name: "configPage", title: "", install: true, uninstall: true) {    
    	section() {
        	paragraph "Name: ${textAppName()}\nCreated by: ${textAuthor()}\n${textVersion()}\n${textModified()}\n\n${textDesc()}", image: "http://i.imgur.com/38Y5PbU.png"
        }
    	section("Control these bulbs...") {
			input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:false, multiple:true
		}
    	section ("..and these switches..") {
        	input "switches", "capability.switch", multiple: true, required: false
    	}
    	section("..and change modes") {
        	input "pauseMode", "mode", title: "Mode when playing?", required:false
        	input "playMode", "mode", title: "Mode when paused?", required:false
        	input "stopMode", "mode", title: "Mode when stopped?", required:false
    	}
    	section("Configuration") {
        	input(name: "bSwitchOffOnPause", type: "bool", title: "Turn switches off on pause")
        	input(name: "iLevelOnStop", type: "number", title: "Bulb levels on Stop", defaultValue:100)
        	input(name: "iLevelOnPause", type: "number", title: "Bulb levels on Pause", defaultValue:30)
        	input(name: "iLevelOnPlay", type: "number", title: "Bulb levels on Play", defaultValue:0)
    	}
        
        section(title: "More options", hidden: true, hideable: true) {
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            label title:"Assign a name", required:false
		}
        
        //logging debug only when enabled
        section(title: "Debug Logging", hidden: true, hideable: true) {
       		paragraph "Please enable debug logging to output logs to the IDE for troubleshoot issues"
            input "debugLogging", "bool", title: "Debug Logging...", required: false, defaultValue: false, refreshAfterSelection: true
            	
            if (debugLogging) { 
            	state.debugLogging = true 
                logWriter("Debug Logging has been ${state.debugLogging.toString().toUpperCase()}")
                paragraph "Debug Logging is Enabled: ${state.debugLogging}"
            }
            else { 
            	state.debugLogging = false 
            	logWriter("Debug Logging has been ${state.debugLogging.toString().toUpperCase()}")    
            }
    	}
    	section() { 
        	href url: "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/appinfo?access_token=${state.accessToken}", 
            		style:"embedded", required:false, title:"API Information", description: "Tap to view Info"
    	}
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	if (!state.accessToken) {
    	createAccessToken()
   	}
}

// This will generate a json output displaying app info for you to paste into config.config file on Plex Server...
def appInfoJson() {
	def configJson = new groovy.json.JsonOutput().toJson([
    	appId:        app.id,
    	accessToken:  state.accessToken,
    	appUrl: 	"https://graph.api.smartthings.com/api/smartapps/installations/${app.id}",
    	onPlay: 	"https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/statechanged/onplay",
		onPause:	"https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/statechanged/onpause",
		onStop:		"https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/statechanged/onstop"
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

mappings {
  path("/statechanged/:command") 	{ action: [ GET: "OnCommandRecieved" ] }
  path("/appinfo") 					{ action: [ GET: "appInfoJson" ] }
}

def OnCommandRecieved() {
	def command = params.command
	def userName = params.user
	def playerName = params.player
	def mediaType = params.type
	logWriter ("Plex.$command($userName, $playerName, $mediaType)")
    
    if (command == "onplay" && modes.contains(location.mode)) {
        changeMode(playMode)
		SetHuesLevel(iLevelOnPlay)
        SetSwitchesOff()
    }
    else if (command == "onpause"&& modes.contains(location.mode)) {
        changeMode(pauseMode)
    	SetHuesLevel(iLevelOnPause)
        if( bSwitchOffOnPause == "true") {
       		SetSwitchesOff()
        } else {
        	SetSwitchesOn()
        }
    }
    else if (command == "onstop"&& modes.contains(location.mode)) {
        changeMode(stopMode)
    	SetHuesLevel(iLevelOnStop)
        SetSwitchesOn()
    }
}

def changeMode(newMode) {
    if (newMode != null && newMode != "" && location.mode != newMode) {
        if (location.modes?.find{it.name == newMode}) {
            setLocationMode(newMode)
        }  else {
            log.warn "Tried to change to undefined mode '${newMode}'"
        }
    }
}

def SetSwitchesOn() {
	switches?.on()
}
def SetSwitchesOff() {
	switches?.off()
}
def SetHuesLevel(level) {
	logWriter ("SetHuesLevel: $level")
	hues*.setLevel(level)
}

//This should be used in place of log.debug if you want to use the toggle in prefs to determine debug output
private def logWriter(value) {
	if (state.debugLogging) {
        log.debug "${value}"
    }	
}


/**************************************************************  
*				  Application Info Variables				  *
***************************************************************/
private def textAppName() 	{ def text = "${appName()}" }	
private def textVersion() 	{ def text = "Version: ${appVersion()}" }
private def textModified() 	{ def text = "Updated: ${versionDate()}" }
private def textAuthor() 	{ def text = "${appAuthor()}" }
private def textNamespace() { def text = "${appNamespace()}" }
private def textVerInfo() 	{ def text = "${appVerInfo()}" }
private def textDesc() 		{ def text = "${appDescription()}" }