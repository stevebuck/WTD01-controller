/**
 *  Securifi Flood Sensor SZ-WTD01
 *
 *  Copyright 2016 Steve Buck
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
 */
definition(
    name: "Securifi Flood Sensor SZ-WTD01",
    namespace: "softwarily",
    author: "Steve Buck",
    description: "Securifi Flood Sensor application used to bring the flood sensor functionality into ST.",
    category: "Safety & Security",
    iconUrl: "http://floodsolutionssd.com/wp-content/themes/infowaytheme/images/icon_flood.png",
    iconX2Url: "http://floodsolutionssd.com/wp-content/themes/infowaytheme/images/icon_flood.png",
    iconX3Url: "http://floodsolutionssd.com/wp-content/themes/infowaytheme/images/icon_flood.png")


preferences {
	section("Title") {
		// TODO: put inputs here
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
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers