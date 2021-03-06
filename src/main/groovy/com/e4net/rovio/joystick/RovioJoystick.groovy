package com.e4net.rovio.joystick

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Level

import com.centralnexus.input.Joystick
import com.e4net.rovio.comms.Comms

/**
 * Wrapper for the handling the joystick commands as they relate to the Rovio
 * 
 * The Java Joystick library this code uses is Copyright 2000-2001 George Rhoten and others.
 * Source code is available from http://sourceforge.net/projects/javajoystick/
 * 
 * @author morris
 *
 */

public class RovioJoystick {
	private static final Logger log = LoggerFactory.getLogger(RovioJoystick.class); 
		
	private Joystick joy
	private Thread thread
	private Comms comms
	private final int MAX_SPEED= 10
	private long lastUpdate= 0
	private final int MOTORDELAY= 200
	
	RovioJoystick(comms, n) {
		log.debug("Attempting to open joystick {}", n)
		joy = Joystick.createInstance(0)
		log.info "Opened joystick: ${joy.toString()}"
		this.comms= comms	
	}
	
	def start() {
		thread= Thread.start {
			log.trace "Starting joystick thread"
			while(!Thread.interrupted()) {
				joy.poll()
				handleJoy(joy)
				
				long now= System.currentTimeMillis()
				if(lastUpdate != 0) {
					long delay= MOTORDELAY - (now - lastUpdate)
					if(delay > 0){
						//log.debug "sleep for {}", delay
						Thread.sleep(delay)
					}else{
						//log.debug "delay= {}", delay
					}
					lastUpdate= System.currentTimeMillis()
					
				}else
					lastUpdate= now;
			}
			log.trace "Leaving joystick thread"
		}
	}
	
	void handleJoy(Joystick joy){
		// move takes priority over rotate
		float x= joy.getX()
		float y= joy.getY()
		float z= joy.getZ()
		int b= joy.getButtons();
		log.trace("handleJoy() - x= {}, y= {}, z= {}, b= {}", [x, y, z, b] as Object[]);
		
		if(!move(x, y))
			rotate(z)
			
		// throttle -1.0 - +1.0
//		float u= joy.getR();
		
		// hat rgt= 1.0 lft= -1.0
//		float r= joy.getU();
		
		// hat up -1.0 down +1.0
//		float v= joy.getV();
//      def pov= joy.getPOV();
		
		
		if(comms) {
			if(b & 4) {
				comms.moveHead("up")
			}else if(b & 8) {
				comms.moveHead("down")
			}
		}
		
		//log.trace("handleJoy() - x= $x, y= $y, z= $z, u= $u, v= $v, r= $r, b= $b");
	}
	
	boolean calcMovement(int x, int y) {
		//log.debug "calcMovement x= {}, y= {}", x, y
		
		if(x == 0 && y == 0){
			setMovement('none', 0)
			return false
			
		}else{
			String move_id = 'none'
			double angler = Math.atan2(x, -y)
			
			if(angler < 0D)
				angler = (Math.PI*2D)+angler
			
			double angle = angler * (180D / Math.PI);
			
			// print "angle: $angle"
			// move_id= Math.round(angle/45.0) as Integer
			// 0-8 
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
			float speed = ((1 - (Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)) / 100)) * (MAX_SPEED));
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
		int nr= Math.round(r * 10)
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
	
	// dir is 'left'|'right', speed is 0-10
	def setRotation(String dir, int speed) {
		//log.trace("setRotation: {} - {}", dir, speed)
		if(speed < 1) // 0 - 1 is dead spot
			return;
		
		if(speed > 10)
			speed= 10;
			
		// speed is 1-10 where 10 is fastest, need to invert to 10-1 where 1 is fastest
		int s= 11 - speed // invert		
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
		//log.debug "move: {} - {}", dir, speed
		
		// set 1 to max, 10 to min
		int s= speed
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
	
	void command(d, s) {
		if(comms)
			comms.motor(d, s)
		else
			log.debug("drive: {}, speed: {}", d, s)	
	}
		
	void stop() {
		thread.interrupt()
	}
	
	public static create(comms, n) {
		// override default logging level if specified
		if("true" == System.getProperty("rovio.joystick.debug")){
			log.setLevel(Level.TRACE);
		}
		
		RovioJoystick rovio = new RovioJoystick(comms, n)
		rovio.start()
		return rovio
	}
	
	public static void main(String[] args) {
		RovioJoystick rovio = new RovioJoystick(null, 0)
		rovio.start()
	}
}
