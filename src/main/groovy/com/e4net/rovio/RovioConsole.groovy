package com.e4net.rovio

import groovyx.net.http.HTTPBuilder

import groovy.beans.Bindable
import groovy.util.logging.Slf4j
import groovy.swing.SwingBuilder
import java.awt.event.ActionListener

import javax.swing.ImageIcon
import javax.swing.UIManager
import javax.swing.WindowConstants as WC
import javax.swing.SwingConstants as SC
import javax.swing.Timer

import net.miginfocom.swing.MigLayout


/**
 * Console for controlling Rovio with a joystick
 * 
 * Gets and displays MJPEG video as it goes.
 * 
 * @author morris
 *
 */

@Slf4j
class RovioConsole {
	SwingBuilder swing
	def model
	Comms comms
	MJPEGParser mjpeg
	def resolution
	boolean running= false
	
	@Bindable String fps= "?"
	@Bindable String status= "Not Running"
	@Bindable String battery= "?"
	@Bindable int currentResolution= 0

	RovioConsole() {
		SwingBuilder.lookAndFeel('mac', 'nimbus', 'gtk', ['metal', [boldFonts: false]])
		swing= new SwingBuilder()
		model= { status: 'this is the status' }
	}

	def show() {
		swing.edt {
			// fix disabled look of toolbar buttons
			def laf= UIManager.getLookAndFeelDefaults()
			laf.put("ToolBar:Button[Disabled].textForeground", UIManager.getColor("nimbusDisabledText"));
			laf.put("ToolBar:ToggleButton[Disabled].textForeground", UIManager.getColor("nimbusDisabledText"));

			frame(title:'Rovio Console', id: 'frame', resizable: true, pack: true, show: true, defaultCloseOperation: WC.EXIT_ON_CLOSE) {

				panel(layout: new MigLayout('nogrid, fill, insets 4')) { // add debug to see grids

					label(id: 'vidwin', border:loweredBevelBorder(2), horizontalAlignment: SC.CENTER, constraints: 'span, pushy, align center, wrap')

					panel(border: titledBorder(title: 'Resolutions'), layout: new MigLayout("nogrid, fillx"), id: 'resolutions', constraints: '') {
						buttonGroup().with { group ->
							[0: 'QCIF', 1: 'CIF', 2: 'QVGA', 3: 'VGA'].each { e ->
								radioButton(actionPerformed: { comms.setResolution(it.source.getClientProperty("Id")) },
								clientPropertyId: e.key, text: e.value, buttonGroup: group,
								selected: bind(source: this, sourceProperty: 'currentResolution', converter: { it == e.key } ),
								constraints: "hidemode 3")
							}
						}
					}

					panel(border: titledBorder(title: 'Night Mode'), layout: new MigLayout("nogrid, fillx"), id: 'night', constraints: '') {
						buttonGroup().with { group ->
							[0: 'Off', 1: '1/2', 2: '1/4', 3: '1/8'].each { e ->
								radioButton(actionPerformed: { comms.setNightMode(it.source.getClientProperty("Id")) },
								clientPropertyId: e.key, text: e.value, buttonGroup: group,
								constraints: "hidemode 3")
							}
						}
					}

					panel(border: titledBorder(title: 'AGC'), layout: new MigLayout("nogrid, fillx"), id: 'agc', constraints: 'wrap') {
						buttonGroup().with { group ->
							[0: '2x', 1: '4x', 2: '8x', 3: '16x', 4: '32x', 5: '64x', 6: '128x'].each { e ->
								radioButton(actionPerformed: { comms.setAGC(it.source.getClientProperty("Id")) },
								clientPropertyId: e.key, text: e.value, buttonGroup: group,
								constraints: "hidemode 3")
							}
						}
					}

					checkBox(id: 'light', text: 'Light', actionPerformed: {comms.setLight(light.selected)}, constraints: 'sg, align left')
					checkBox(id: 'bluelight', text: 'blue', selected: true, actionPerformed: {comms.setBlueLights(bluelight.selected)}, constraints: 'sg, align left')

					button(text:'Test', constraints: 'gap push, sg, align right', actionPerformed: { comms.moveHead("up") })
					button(id: 'start', text:'Start', constraints: 'gap push, sg, align right', actionPerformed: { start() })
					button(id: 'stop', text:'Stop', constraints: 'sg, align right', actionPerformed: { stop() })
					button(text:'Quit', constraints: 'sg, align right, wrap', actionPerformed: { System.exit(0) })

					panel(border:loweredBevelBorder(4),	layout: new MigLayout('fill, insets 1'), constraints: 'growx, dock south') {
						label(id: 'fps', text: bind(source: this, sourceProperty: 'fps'))
						label(text: "|")
						label(id: 'status', text: bind(source: this, sourceProperty: 'status'))
						label(text: "|")
						label(id: 'battery', text: bind(source: this, sourceProperty: 'battery'))
					}

				}
			}
		}
		
		// create a timer that can do regular updates of the UI
		Timer timer = new Timer(1000, { regularTasks() } as ActionListener)
		timer.repeats= true
		timer.start()

		swing.doLater{
			frame.size= [900, 700]
		}
	}
	
	def start() {
		mjpeg.start()
		running= true
		def s= comms.getStatus()
		swing.doLater { 
			swing.start.enabled= false
			status= "Running"
			currentResolution= s.resolution as Integer
		}
		log.debug "current resolution: {}", currentResolution
		comms.setFrameRate(15)
	}
	
	def stop() {
		mjpeg.stop()
		running= false
		swing.doLater { 
			swing.start.enabled= true
			status= "Not Running"
		}
	}
	
	// gets called every second, use to check battery status
	def regularTasks() {
		if(running) {
			def s= comms.getStatus()
			swing.doLater {
				def b= s.battery as Integer
				if(b < 106){
					battery= "critical"
				}else{
					b= ((b-106)*100)/(127-106) as Integer
					battery= "$b%"
				}	
			}
		}
	}

	def setImage(byte [] img){
		swing.doLater {
			ImageIcon ic= new ImageIcon(img)
			vidwin.setIcon(ic)
		}
	}

	def setFrameRate(int r) {
		//println "Frame rate: ${r} frames/sec"
		swing.doLater {
			fps= "$r fps"
		}
	}


	static main(args) {
		def rovio= new RovioConsole()
		rovio.show()

		def comms= new Comms("http://rovio", "morris", "qaz1xsw")
		rovio.comms= comms

		def joy= RovioJoystick.create(comms)

		rovio.mjpeg= new MyMJPEG(rovio, "http://rovio/GetData.cgi", "morris", "qaz1xsw")
	}

}

@Slf4j
class MyMJPEG extends MJPEGParser {
	long lasttime;
	int cnt= 0;
	RovioConsole frame;

	public MyMJPEG(RovioConsole frame, String mjpeg_url, String username, String password) {
		super(mjpeg_url, username, password);
		this.frame= frame;
		lasttime= System.currentTimeMillis()
	}

	@Override
	protected void handleJPEG(byte[] capture, int jpegSize) {
		long tm= System.currentTimeMillis();
		def delta= tm-lasttime
		if(delta >= 1000){
			frame.setFrameRate((cnt/(delta/1000)) as Integer);
			cnt= 0;
			lasttime= tm;
		}
	
		cnt++;
		frame.setImage(capture);
	}
}
