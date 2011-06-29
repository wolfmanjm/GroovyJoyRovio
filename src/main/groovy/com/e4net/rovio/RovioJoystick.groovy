package com.e4net.rovio

import com.centralnexus.input.Joystick
import groovy.util.logging.Slf4j

@Slf4j
public class RovioJoystick {
	private Joystick joy
	private Thread thread
	private Comms comms;
	private final int MAX_SPEED= 10;
	
	RovioJoystick(comms) {
		joy = Joystick.createInstance(0)
		println "Opened joystick: ${joy.toString()}"
		this.comms= comms
		
	}
	
	def start() {
		thread= Thread.start {
			while(!Thread.interrupted()) {
				joy.poll()
				handleJoy(joy)
				Thread.sleep(150)
			}
		}
	}
	
	void handleJoy(Joystick joy){
		// move takes priority over rotate
		if(!move(joy.getX(), joy.getY()))
			rotate(joy.getZ())
			
		// throttle -1.0 - +1.0
		def u= joy.getR();
		
		// hat rgt= 1.0 lft= -1.0
		def r= joy.getU();
		
		// hat up -1.0 down +1.0
		def v= joy.getV();
		
		// buttons
		def b= joy.getButtons();
		//def pov= joy.getPOV();
		
		//log.trace("handleJoy() - u= " + u + ", v= " + v + ", r= " + r + ", b= " + b);
	}
	
	boolean calcMovement(int x, int y) {
		log.trace "calcMovement x= {}, y= {}", x, y
		
		if(x == 0 && y == 0){
			setMovement('none', 0)
			return false
			
		}else{
			def move_id = 'none'
			double angler = Math.atan2(x, -y)
			
			if(angler < 0D)
				angler = (Math.PI*2D)+angler
			
			double angle = angler * (180D / Math.PI);
			
			// print "angle: $angle"
			
			switch(angle) {
				case {angle < 22.5D}:
						move_id = 'forward'; break
				case {angle < 67.5D}:
						move_id = 'forward_right'; break
				case {angle < 112.5D}:
						move_id = 'right'; break
				case {angle < 157.5D}:
						move_id = 'backward_right'; break
				case {angle < 202.5D}:
						move_id = 'backward'; break
				case {angle < 247.5D}:
						move_id = 'backward_left'; break
				case {angle < 292.5D}:
						move_id = 'left'; break
				case {angle < 337.5D}:
						move_id = 'forward_left'; break
				case {angle <= 360.0D}:
						move_id = 'forward'; break
			}
			
			// print "move_id: $move_id"
			def speed = ((1 - (Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)) / 100)) * (MAX_SPEED));
			//def speed= [x.abs(), y.abs()].max()
			
			setMovement(move_id, Math.round(speed) as Integer)
			return true
		}
	}
	
	boolean move(float x, float y) {
		int nx= Math.round(x * 100)
		int ny= Math.round(y * 100)
		calcMovement(nx, ny)
	}
	
	boolean rotate(float r){
		int nr= Math.round(r * 100)
		if(nr == 0){
			setRotation('none', 0)
			return false
		}
		
		if(nr > 0){
			setRotation('right', nr)
			return true
		}
		
		if(r < 0) {
			setRotation('left', nr.abs())
			return true
		}
		
		return false
	}
	
	// dir is 'left'|'right', speed is percentage
	def setRotation(String dir, int speed) {
		log.trace("setRotation: {} - {}", dir, speed)
		// set 1 to max, 10 to min
		speed= [100, speed].min()
		int s= 10 -  Math.round(speed/10)
		s= [s, 1].max()
		int d= 0
		
		switch(dir) {
			case 'none':
					d= 0; break
			case 'left':
					d= 5; break
			case 'right':
					d= 6; break
		}
		
		if(d != 0)
			command(d, s)
	}
	
	def setMovement(String dir, int speed) {
		log.trace "move: {} - {}", dir, speed
		
		// set 1 to max, 10 to min
		def s= speed
		if(speed > 10) s= 10
		else if(speed < 1) s= 1
		
//		speed= [100, speed].min()
//		int s= 10 - (speed/10)
//		s= [s, 1].max()
		int drive_cmd= 0
		
		switch(dir) {
			case 'none':
					drive_cmd= 0; break
			case 'forward':
					drive_cmd = 1; break
			case 'left':
					drive_cmd = 3; break
			case 'right':
					drive_cmd = 4; break
			case 'backward':
					drive_cmd = 2; break
			case 'forward_left':
					drive_cmd = 7; break
			case 'forward_right':
					drive_cmd = 8; break
			case 'backward_left':
					drive_cmd = 9; break
			case 'backward_right':
					drive_cmd = 10; break
		}
		
		if(drive_cmd != 0)
			command(drive_cmd, s)
	}
	
	def command(d, s) {
		String data = "Cmd=nav&action=18&drive=$d&speed=$s"
		comms.sendCommand('rev.cgi', [Cmd: 'nav', action: '18', drive: d, speed: s]);
	}
		
	//    video_thread= Thread.start {
	//      while(!Thread.interrupted()) {
	//        displayVideo()
	//        sleep(33)
	//      }
	//    }
	
	
	//  def displayVideo() {
	//    http.get(path: "/Jpeg/CamImg.jpg"){
	//      resp, reader ->
	//      // println "response status: ${resp.statusLine}"
	//      assert resp.statusCode == 200
	//      len= resp.headers.'Content-Length'
	//      buf << reader
	//      assert buf.size == len
	//      displayJPEG(buf)
	//    }
	//  }
	
	def stop() {
		thread.interrupt()
	}
	
	public static create() {
		def comms= new Comms("http://rovio", "morris", "qaz1xsw")
		RovioJoystick rovio = new RovioJoystick(comms)
		rovio.start()
		return rovio
	}
	
	public static void main(String[] args) {
		create()
	}
}
