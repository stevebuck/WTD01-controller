/**
 *  Securifi Flood Sensor
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
 *  TODO:
 *   1. Battery status values
 *   2. Improved tile layout: battery status icon and refresh tiles
 */
metadata {
  definition (name: "Securifi Flood Sensor", namespace: "softwarily", author: "Steve Buck") {
    capability "Battery"
    capability "Tamper Alert"
    capability "Sensor"
    capability "Water Sensor"
    attribute "Flood Sensor", "string"
  }


  simulator {
    status "active": "zone report :: type: 19 value: 0031"
    status "inactive": "zone report :: type: 19 value: 0030"
  }

  tiles {
    tiles(scale: 2) {
      multiAttributeTile(name:"floodSensor", type: "device.floodSensor", width: 6, height: 4) {
        tileAttribute ("device.floodSensor", key: "PRIMARY_CONTROL") {
          attributeState "dry", label: '${name}', icon:"st.alarm.water.dry", backgroundColor:"#ffffff"
          attributeState "wet", label: '${name}', icon:"st.alarm.water.wet", backgroundColor:"#53a7c0"
        }
      }
    }

    standardTile("tamperSwitch", "device.tamperSwitch", width: 2, height: 2) {
      state("open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e")
      state("closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821")
    }

    main (["floodSensor"])
    details(["floodSensor","tamperSwitch"])
  }
}

def configure() {
  log.trace("** PIR02 ** configure called for device with network ID ${device.deviceNetworkId}")

  // Switch endian
  String zigbeeId = String.reverse(device.hub.zigbeeId)
  log.trace "Configuring Reporting, IAS CIE, and Bindings."
  def configCmds = [
    "zcl global write 0x500 0x10 0xf0 {${zigbeeId}}", "delay 200",
    "send 0x${device.deviceNetworkId} 1 1", "delay 1500",
    "zcl global send-me-a-report 1 0x20 0x20 0x3600 0x3600 {01}", "delay 200",
    "send 0x${device.deviceNetworkId} 1 1", "delay 1500",
    "zdo bind 0x${device.deviceNetworkId} 1 1 0x001 {${device.zigbeeId}} {}", "delay 1500",
    "raw 0x500 {01 23 00 00 00}", "delay 200",
    "send 0x${device.deviceNetworkId} 1 1", "delay 1500",
    ]

    return configCmds // send refresh cmds as part of config
}

def enrollResponse() {
  log.trace "Sending enroll response"
  [
    "raw 0x500 {01 23 00 00 00}", "delay 200",
    "send 0x${device.deviceNetworkId} 1 1"
    ]
}

// Parse incoming device messages to generate events
def parse(String description) {
  log.trace("** PIR02 parse received ** ${description}")
  def result = []
  Map map = [:]

  if (description?.startsWith('zone status')) {
    map = parseIasMessage(description)
  }

  log.trace "Parse returned $map"
  map.each { k, v ->
    log.debug "sending event ${v}"
    sendEvent v
  }

//	def result = map ? createEvent(map) : null

  if (description?.startsWith('enroll request')) {
    List cmds = enrollResponse()
    log.debug "enroll response: ${cmds}"
    result = cmds?.collect { new physicalgraph.device.HubAction(it) }
  }

  return result
}

private Map parseIasMessage(String description) {
  List parsedMsg = description.split(' ')
  String msgCode = parsedMsg[2]
  
  Map resultMap = [:]
  switch(msgCode) {
	case '0x0030': // Dry
      log.trace 'Detected dry'
      resultMap["moisture"] = getFloodSensorResult 'dry'
      resultMap["tamperSwitch"] = getContactResult 'closed'
      break

    case '0x0031': // Wet
      log.trace 'Detected moisture'
      resultMap["moisture"] = getFloodSensorResult 'wet'
      resultMap["tamperSwitch"] = getContactResult 'closed'
      break

    case '0x0034': // Supervision Report
      log.trace 'Detected tamper, no moisture'
      resultMap["moisture"] = getFloodSensorResult 'dry'
      resultMap["tamperSwitch"] = getContactResult 'open'
      break

    case '0x0035': // Restore Report
      log.trace 'Detected moisture and tamper'
      resultMap["moisture"] = getFloodSensorResult 'wet'
      resultMap["tamperSwitch"] = getContactResult 'open'
      break

	default:
      log.warn "Unknown signal: ${msgCode}"
      break
  }

  return resultMap
}

private Map getContactResult(value) {
  log.debug "Tamper Switch Status: ${value}"
  def linkText = getLinkText(device)
  def descriptionText = "${linkText} was ${value == 'open' ? 'opened' : 'closed'}"

  return [name: 'tamperSwitch', value: value, descriptionText: descriptionText]
}

private Map getFloodSensorResult(value) {
	log.debug "Flood Sensor Status: ${value}"
    def linkText = getLinkText(device)
    def descriptionText = "${linkText} was ${value == 'wet'? 'flooded': 'dry'}"
    
    return [name: 'floodSensor', value: value, descriptionText: descriptionText]
}

private hex(value) {
  new BigInteger(Math.round(value).toString()).toString(16)
}
