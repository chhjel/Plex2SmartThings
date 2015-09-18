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
 *  v1.1 - Added optional mode changes
 */
 
import groovy.json.JsonBuilder

definition(
    name: "Plex Integration",
    namespace: "ChristianH",
    author: "Christian Hjelseth",
    description: "Allows web requests to dim/turn off/on lights when plex is playing.",
    category: "My Apps",
    iconUrl: "http://1sd3vh2v9afo91q38219tlj1.wpengine.netdna-cdn.com/wp-content/uploads/2015/05/plex-icon-server-big-3b6e2330294017827d0354f0c768a3ab.png",
    iconX2Url: "http://1sd3vh2v9afo91q38219tlj1.wpengine.netdna-cdn.com/wp-content/uploads/2015/05/plex-icon-server-big-3b6e2330294017827d0354f0c768a3ab.png",
    iconX3Url: "http://1sd3vh2v9afo91q38219tlj1.wpengine.netdna-cdn.com/wp-content/uploads/2015/05/plex-icon-server-big-3b6e2330294017827d0354f0c768a3ab.png",
    oauth: [displayName: "PlexServer", displayLink: ""])

preferences {
	page(name: "configPage") 
}

//had to move to a dynamic page to handle the possible missing token
/*  Main Page  */
def configPage() {
	//Generates an accessToken if one has not been generated
	if (!state.accessToken) {
    	createAccessToken()
   	}
    if (state.debugLogging == null) 	{ state.debugLogging = false }
    
	dynamicPage(name: "configPage", title: "Main Page", install: true, uninstall: true) {    
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
        // Enables logging debug only when enabled
        section(title: "Debug Logging") {
       		paragraph "If you experiencing issues please enable logging to help troubleshoot"
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

// These Methods Generate Json for you Info Only
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
  path("/appinfo") 					{ action: [ GET: "appInfoJson"]   }
}

def OnCommandRecieved() {
	def command = params.command
	def userName = params.user
	def playerName = params.player
	def mediaType = params.type
	logWriter ("Plex.$command($userName, $playerName, $mediaType)")
    
    if (command == "onplay") {
        changeMode(playMode)
		SetHuesLevel(iLevelOnPlay)
        SetSwitchesOff()
    }
    else if (command == "onpause") {
        changeMode(pauseMode)
    	SetHuesLevel(iLevelOnPause)
        if( bSwitchOffOnPause == "true") {
       		SetSwitchesOff()
        } else {
        	SetSwitchesOn()
        }
    }
    else if (command == "onstop") {
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
	logWriter ("SetSwitchesOn")
	switches?.on()
}
def SetSwitchesOff() {
	logWriter ("SetSwitchesOff")
	switches?.off()
}
def SetHuesLevel(level) {
	if (level != null) {
		logWriter ("SetHuesLevel: $level")
		hues*.setLevel(level)
	}
}

private def logWriter(value) {
	if (state.debugLogging) {
        log.debug "${value}"
    }	
}