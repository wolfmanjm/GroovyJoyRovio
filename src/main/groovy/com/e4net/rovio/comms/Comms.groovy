package com.e4net.rovio.comms

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovyx.net.http.HTTPBuilder;
import org.apache.http.params.HttpConnectionParams

class Comms {
	private static final Logger log = LoggerFactory.getLogger(Comms.class); 
	private HTTPBuilder http
	private final int timeout= 30000 // 30 second timeout

	Comms(url, user, password) {
		http= new HTTPBuilder(url)
		HttpConnectionParams.setConnectionTimeout( http.client.params, timeout );
		HttpConnectionParams.setSoTimeout( http.client.params, timeout );
		http.auth.basic user, password
	}

	def synchronized sendCommand(cmd, params) {
		log.trace("path: {}, params: {}", cmd, params)
		http.get(path: cmd, query: params)
	}

	def setLight(on) {
		// "Cmd=nav&action=19&LIGHT="+light+""
		sendCommand("rev.cgi", [Cmd: 'nav', action: '19', 'LIGHT': on?"1":"0"])
	}

	def setBlueLights(on) {
		if(on)
			sendCommand("mcu", [parameters: '114D4D00010053485254000100011AFF0000'])
		else
			sendCommand("mcu", [parameters: '114D4D00010053485254000100011A000000'])
	}

	def setResolution(n) {
		sendCommand('ChangeResolution.cgi', ['ResType': n])
	}
	
	def setNightMode(n) {
		sendCommand('debug.cgi', [action: 'write_i2c', address: '0x3b', value: '0x02'])
		def v= Integer.toString((0x80 | (n<<5) | 0x02), 16)
		def val= "0x$v"
		sendCommand('debug.cgi', [action: 'write_i2c', address: '0x3b', value: val])
		log.debug("Set night mode to {}", val)		
	}
	
	def setAGC(n) {
		def val= "0x${n}8"
		sendCommand('debug.cgi', [action: 'write_i2c', address: '0x14', value: val])
		log.debug("Set AGC to {}", val)		
	}
	
	def getStatus() {
		def status= [:]
		def r= sendCommand('rev.cgi', [Cmd: 'nav', action: '1'])
		/*
		System.out << r
		Cmd = nav
		responses = 0|x=3718|y=3495|theta=0.863|room=0|ss=60
		|beacon=51|beacon_x=9941|next_room=-1|next_room_ss=0
		|state=0|ui_status=0|resistance=0|sm=15|pp=0|flags=0001
		|brightness=6|resolution=3|video_compression=2|frame_rate=30
		|privilege=0|user_check=1|speaker_volume=14|mic_volume=17
		|wifi_ss=212|show_time=0|ddns_state=0|email_state=0
		|battery=123|charging=0|head_position=202|ac_freq=2
		*/
		r.splitEachLine(/\|/) { parts ->
			if(parts.size() > 0){
				parts.each {
					if(!it.trim().isEmpty()){
						def l= it.split("=")
						if(l.length > 1){
							status[l[0].trim()]= l[1].trim()
						}
					}
				}
			}
		}
		return status
	}
	
	def moveHead(def dir, def delta=1.0) {
		switch(dir){
			case "up":
				sendCommand('rev.cgi', [Cmd: 'nav', action: '18', drive: '11', speed: '10'])
				sendCommand('rev.cgi', [Cmd: 'nav', action: '18', drive: '0', speed: '10'])
				break
				
			case "down":
				sendCommand('rev.cgi', [Cmd: 'nav', action: '18', drive: '12', speed: '10'])
				sendCommand('rev.cgi', [Cmd: 'nav', action: '18', drive: '0', speed: '10'])
				break
		}		
	}
	
	def motor(drv, spd ) {
		sendCommand('rev.cgi', [Cmd: 'nav', action: '18', drive: drv, speed: spd]);
	}
	
	def setFrameRate(r) {
		sendCommand('ChangeFramerate.cgi', ['Framerate': r])
	}
}