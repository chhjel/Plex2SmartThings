/**
 *  Plex Plus Device
 *
 *  Copyright 2016 Jake Tebbett (jebbett)
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
 * 
 */

metadata {
	definition (name: "Plex Plus Device", namespace: "jebbett", author: "jebbett") {
	capability "musicPlayer"
    command "playbackType", ["string"]
}
	tiles(scale: 2) {
        multiAttributeTile(name:"status", type: "generic", width: 6, height: 4, canChangeIcon: false){
            tileAttribute ("device.status", key: "PRIMARY_CONTROL") {
            attributeState "playing", label:'Playing', icon:"st.Electronics.electronics16", backgroundColor:"#79b821"
            attributeState "stopped", label:'Stopped', icon:"st.Electronics.electronics16", backgroundColor:"#ffffff"
            attributeState "paused", label:'Paused', icon:"st.Electronics.electronics16", backgroundColor:"#FFA500"
        }        
            tileAttribute ("device.playbackType", key: "SECONDARY_CONTROL") {
                attributeState "playbackType", label:'${currentValue}'
            }
        }
    }
}

def playbackType(type) {
	sendEvent(name: "playbackType", value: type);
}

def play() {	        
    sendEvent(name: "status", value: "playing");
}

def pause() {
    sendEvent(name: "status", value: "paused");
}

def stop() {
    sendEvent(name: "status", value: "stopped");
    
}