package com.e4net.rovio

import com.e4net.rovio.joystick.RovioJoystick
import com.e4net.rovio.comms.Comms

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
import javax.swing.JOptionPane

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences
import net.miginfocom.swing.MigLayout


/**
 * Console for controlling Rovio with a joystick
 * 
 * Gets and displays MJPEG video as it goes.
 * 
 * @author morris
 *
 */

class RovioConsole {
	private static final Logger log = LoggerFactory.getLogger(RovioConsole.class);
	private static Preferences prefs
	
	SwingBuilder swing
	Comms comms
	MJPEGParser mjpeg
	def resolution
	boolean running= false
	def joy
	
	@Bindable String fps= "??? fps"
	@Bindable String status= "Not Running"
	@Bindable String host= "???.???.???.???"
	@Bindable String battery= "??%"
	@Bindable int currentResolution= 0

	RovioConsole() {
		SwingBuilder.lookAndFeel('mac', 'nimbus', 'gtk', ['metal', [boldFonts: false]])
		swing= new SwingBuilder()
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
					checkBox(id: 'bluelight', text: 'Blue lights', selected: true, actionPerformed: {comms.setBlueLights(bluelight.selected)}, constraints: 'sg, align left')

					//button(text:'Test', constraints: 'gap push, sg, align right', actionPerformed: { comms.moveHead("up") })
					
					button(id: 'start', text:'Start Video', constraints: 'gap push, sg, align right', actionPerformed: { start() })
					button(id: 'stop', text:'Stop Video', constraints: 'sg, align right', actionPerformed: { stop() })
					button(text:'Logout', constraints: 'sg, align right', actionPerformed: { logout() })
					button(text:'Quit', constraints: 'sg, align right, wrap', actionPerformed: { System.exit(0) })

					panel(border:loweredBevelBorder(4),	layout: new MigLayout('fill, insets 1'), constraints: 'growx, dock south') {
						label(id: 'fps', text: bind(source: this, sourceProperty: 'fps'))
						label(text: "|")
						label(id: 'statusid', text: bind(source: this, sourceProperty: 'status'))
						label(text: "|")
						label(id: 'hostid', text: bind(source: this, sourceProperty: 'host'))
						label(text: "|")
						label(id: 'battery', text: bind(source: this, sourceProperty: 'battery'))
					}

				}
			}
		}
		
		swing.doLater{
			frame.size= [900, 700]
		}
	}
	
	def start() {
		swing.start.enabled= false
		swing.doOutside { 
			status= "Starting Video"
			mjpeg.start()
			
			swing.doLater {
				status= "Video running"
			}
			//comms.setFrameRate(15) // this does not seem to affect MJPEG	
		}
	}
	
	def stop() {
		status= "Stopping"
		swing.doOutside { 
			mjpeg.stop()
			running= false
			swing.doLater { 
				swing.start.enabled= true
				status= "Not Running"
			}
		}
	}
	
	def logout() {
		prefs.clear();
		System.exit(0);	
	}
	
	// gets called every second, use to check battery status
	def regularTasks() {
		swing.doOutside { 
			def s= comms.getStatus()
			def b= s.battery as Integer
			if(b < 106){
				battery= "critical"
			}else{
				b= ((b-106)*100)/(127-106) as Integer
				battery= "$b%"
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
		swing.doLater {
			fps= "$r fps"
		}
	}

	def getLogin() {
		def ds=	new SwingBuilder()
		def p= ds.panel(layout: new MigLayout("wrap 2", "[right]rel[]", "[]10[]")) {
			label('Hostname or IP')
			textField(id: 'host', columns: 20)
			label('Username')
			textField(id: 'username', columns: 20)
			label('Password')
			passwordField(id: 'password', columns: 20)
		}
		
		def ret= ds.optionPane().showOptionDialog(swing.frame, p, "Enter Rovio Admin Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null)
		if(ret == 0)
			[host: ds.host.text, username: ds.username.text, password: ds.password.text]
		else
			null
	}
	
	static main(args) {
		RovioConsole rovio= new RovioConsole()
		rovio.show()
		
		String host
		String username
		String password
		
		prefs = Preferences.userNodeForPackage(rovio.getClass())

		if(args.length >= 3){
			host= args[0]
			username= args[1]
			password= args[2]
			prefs.put "username", username
			prefs.put "password", password
			prefs.put "host", host
		}else{
			password= prefs.get("password", null)
			username= prefs.get("username", null)
			host= prefs.get("host", null)
		}
		
		if(!host || !username || !password) {
			def login= rovio.getLogin()
			if(login == null)
				System.exit(1)
				
			host= login.host
			username= login.username
			password= login.password
			prefs.put "host", host
			prefs.put "username", username
			prefs.put "password", password
		}
		
		rovio.host= host
		
		def comms= new Comms("http://$host", username, password)
		rovio.comms= comms

		try {
			rovio.joy= RovioJoystick.create(comms)
			
		}catch(Exception) {
			//log.error("No Joystick found")
			JOptionPane.showMessageDialog(rovio.swing.frame, "No Joystick found", "Joystick Error", JOptionPane.ERROR_MESSAGE)
		}
		
		rovio.mjpeg= new MyMJPEG(rovio, "http://$host/GetData.cgi", username, password)
		
		// get status from rovio
		try {
			def st= comms.getStatus()
			rovio.running= true
			rovio.status= "Connected"
			rovio.currentResolution= st.resolution as Integer

		}catch(Exception e){
			prefs.clear()
			def error= e.getMessage()
			log.error("Error getting status: {}", error)
			JOptionPane.showMessageDialog(rovio.swing.frame, error, "Failed to connect to Rovio", JOptionPane.ERROR_MESSAGE)
			System.exit(1)
		}
		
		// create a timer that can do regular updates of the UI
		Timer timer = new Timer(1000, { rovio.regularTasks() } as ActionListener)
		timer.repeats= true
		timer.start()
	}

}

class MyMJPEG extends MJPEGParser {
	long lasttime;
	int cnt= 0;
	RovioConsole rc;

	public MyMJPEG(RovioConsole rc, String mjpeg_url, String username, String password) {
		super(mjpeg_url, username, password);
		this.rc= rc;
		lasttime= System.currentTimeMillis()
	}

	@Override
	protected void handleJPEG(byte[] capture, int jpegSize) {
		long tm= System.currentTimeMillis();
		def delta= tm-lasttime
		if(delta >= 1000){
			rc.setFrameRate((cnt/(delta/1000)) as Integer);
			cnt= 0;
			lasttime= tm;
		}
	
		cnt++;
		rc.setImage(capture);
	}
}
