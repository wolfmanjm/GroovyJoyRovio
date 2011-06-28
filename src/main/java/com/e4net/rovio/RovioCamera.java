/**
 * RovioCamera class.
 **/

package com.e4net.rovio;

import java.net.*;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;
import java.io.*;

public final class RovioCamera {

	private static RovioCamera rob;
	private static Thread mainThread;
	private volatile boolean running = true;
	private volatile static boolean exiting = false;
	@SuppressWarnings("unused")
	private static int interval = 0;
	private String camhost;
	private String camproto;
	private int camport;
	private CameraWindow displayWin;
	private ArrayBlockingQueue<Object[]> eventQ;
	private static RovioJoystick joy;

	public RovioCamera() {
		eventQ= new ArrayBlockingQueue<Object[]>(10);
	}

	@SuppressWarnings("unused")
	private boolean move(int deltax, int deltay) {
		float scale= 200/80;
		int dx= (int) (deltax*scale);
		int dy= (int) (deltay*1.0);
		
		System.out.println("move: " + deltax + ", " + deltay + " ("	+ dx + "," + dy + ")");
        joy.rotate(dx);
        return true;
	}

	private byte[] getFrameFromWebcam(URL url) {
		byte[] ba = null;
		try {
			//System.out.println("Getting frame...");
			URLConnection urlc = url.openConnection();
			//System.out.println("connected...");
			
			urlc.setDoInput(true);
			BufferedInputStream in = new BufferedInputStream(urlc.getInputStream());
			int len = urlc.getContentLength();
			if (len <= 0) {
				System.err.println("No content Length");
				return null;
			}
			//System.out.println("got length...");
			ba = new byte[len];
			int cnt = 0;
			while (cnt < len) {
				int n = in.read(ba, cnt, len - cnt);
				if (n < 0)
					break;
				cnt += n;
			}
			in.close();
			//System.out.println("closed input...");
			if (cnt != len) {
				System.out.println("Error read: " + cnt + ", needed: " + len);
				return null;
			}
		} catch (Exception ex) {
			System.err.println("error: " + ex.getMessage());
			ba = null;
		}
		//System.out.println("Got frame length: " + ba.length);
		
		return ba;
	}

	// start a session to the specified RovioCamera server
	public boolean start(String camserverurl) {
		displayWin= new CameraWindow();
		displayWin.startVideoWindow("Video");
		displayWin.addObserver(new Observer(){
			public void update(Observable o, Object arg) {
				Object [] args= (Object[]) arg;
				if(args[0].equals("closed")){
					stopIt();
				}else if(args[0].equals("stop")){
					
				}else{
					eventQ.offer(args);
				}
			}});
		
		URL url;
		try {
			url = new URL(camserverurl);
			camproto = url.getProtocol();
			camhost = url.getHost();
			camport = url.getPort();
			System.out.println("Url: " + camproto + " " + camhost + " " + camport);
		} catch (Exception ex) {
			System.err.println("Bad URL: " + camserverurl);
			return false;
		}

		// connect to IP camera, and start grabbing frames
		while (running) {
//			try {
//				Object [] oa= eventQ.poll(interval, TimeUnit.MILLISECONDS);
//				if(oa != null){
//					if(oa[0].equals("move")) move((Integer)oa[1], (Integer)oa[2]);
//					else System.out.println("Unknown event: " + oa[0]);
//					continue;
//				}
//			} catch (InterruptedException e) {
//				break;
//			}
			byte[] img = getFrameFromWebcam(url);
			if (img != null) {
				displayJpg(img);
			} else {
				System.err.println("Failed to get URL: " + camserverurl);
				return false;
			}
		}
		
		return true;
	}

	private void displayJpg(byte[] img) {
		displayWin.display(img);
	}

	public void stop() {
		exiting = true;
		running = false;
		displayWin.stopVideoWindow();
	}

	static class BasicAuthenticator extends Authenticator {

		private final String name;
		private final char[] pw;

		public BasicAuthenticator(String name, String pw) {
			this.name = name;
			this.pw = pw.toCharArray();
		}

		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication (name, pw);
		}
	}
	
	static void runIt(final String url, final String name, final String pw) {
		mainThread = new Thread("comms") {
			public void run() {
				System.out.println("Started: " + url);
			    // Install Authenticator
			    Authenticator.setDefault (new BasicAuthenticator (name, pw));
				while (!exiting) {
					rob = new RovioCamera();
					if (!rob.start(url))
						exiting = true;
					if (exiting)
						break;
					rob = null;
				}
				System.out.println("Stopped: " + url);
			}
		};
		mainThread.start();
	}

	class MyMJPEG extends MJPEGParser {
		long lasttime= 0;
		int cnt= 0;
		public MyMJPEG(String mjpeg_url, String username, String password) {
			super(mjpeg_url, username, password);
		}

		protected void handleJPEG(byte[] capture, int jpegSize) {
			long tm= System.currentTimeMillis();
			if(lasttime == 0){
				lasttime= tm;
			}else if((tm-lasttime) >= 1000){
				System.out.println("frame rate= " + cnt + " frames/sec");
				cnt= 0;
				lasttime= tm;
			}
			cnt++;
			displayJpg(capture);
		}
	}
	
	MyMJPEG mjpeg;
	
	private void runMjpeg(String url, String username, String password) {
		displayWin= new CameraWindow();
		displayWin.startVideoWindow("Video");
		displayWin.addObserver(new Observer(){
			public void update(Observable o, Object arg) {
				Object [] args= (Object[]) arg;
				if(args[0].equals("closed")){
					mjpeg.stop();
				}else if(args[0].equals("stop")){
					mjpeg.stop();
					
				}else{
					eventQ.offer(args);
				}
			}});
		mjpeg= new MyMJPEG(url, username, password);
		mjpeg.start();
	}
	
	static void stopIt() {
		System.out.println("Stopping ICamera");
		rob.stop();
		mainThread.interrupt();
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			String t = args[0];
			interval = Integer.parseInt(t);
		}
		
		System.out.println("Rovio Camera: $Revision: 1.00 $ ");
		//runIt("http://rovio/Jpeg/CamImg.jpg", "morris", "qaz1xsw");
		RovioCamera rovioCamera = new RovioCamera();
		rovioCamera.runMjpeg("http://rovio/GetData.cgi", "morris", "qaz1xsw");
		
        //joy= (RovioJoystick) RovioJoystick.create();

//		try {
//			System.in.read();
//		} catch (Exception e) {
//		}
//		stopIt();
//		System.out.println("RovioCamera Stopped");
	}

}
