package com.e4net.rovio;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MJPEGParser {
	private static final Logger jlog = LoggerFactory.getLogger(MJPEGParser.class);
	
	private final int MAXFRAME= 8192;
	private String boundary=  "--WINBONDBOUDARY";
	private int[] failure;
	private String mjpeg_url;
	private String username;
	private String password;
	private BufferedInputStream bis;
	private enum State {FIRST, SKIP, SECOND};
	private long lasttime;
	private int frameCnt;
	
	
	/**
	 * @param args
	 * 
	 * For testing only
	 */
	public static void main(String[] args) {
		String host;
		String username;
		String password;
		
		if(args.length >= 3){
			host= args[0];
			username= args[1];
			password= args[2];
		}else{
			jlog.error("Usage: host username password are required");
			return;
		}
		MJPEGParser m= new MJPEGParser("http://" + host + "/GetData.cgi", username, password);
		m.start();
	}

	public MJPEGParser(String mjpeg_url)
	{
		this(mjpeg_url,null,null);
	}

	public MJPEGParser(String mjpeg_url, String username, String password)
	{
		this.failure = computeFailure(boundary.getBytes());
		this.mjpeg_url= mjpeg_url;
		this.username= username;
		this.password= password;
	}
	
	public void start() {
		jlog.debug("process() - Starting to stream from {}", mjpeg_url);
		jlog.trace("TRACE enabled");
		new Thread(new Runnable() {
			public void run() {
				try {
					if (username != null && password != null) {
						Authenticator.setDefault(new HTTPAuthenticator(username, password));
					}

					URL url = new URL(mjpeg_url);
					URLConnection urlc = url.openConnection();
					bis = new BufferedInputStream(urlc.getInputStream());
					ByteArrayOutputStream baos = new ByteArrayOutputStream(MAXFRAME);
					int cnt = 0;
					int pos = 0;
					int off= 0;
					int frameStart= 0;
					byte[] buf = new byte[MAXFRAME];
					State state= State.FIRST;
					
					while ((cnt = bis.read(buf, 0, buf.length)) > 0) {
						// accumulate enough to guarantee we have an entire frame
						baos.write(buf, 0, cnt);

						byte[] b = baos.toByteArray();

						switch (state) {
							case FIRST:
								if ((pos = findString(b, off, b.length, boundary)) == -1){
									jlog.trace("first boundary not found, off: {}", off);
									off= b.length - boundary.length();
									continue;
								}
								jlog.trace("first boundary found");
								state= State.SKIP;
								off= 0;
								
							case SKIP:
								// boundary found at pos, now skip rest of header lines and get to data
								frameStart= skipLines(b, pos, 3); // read ahead number of lines
								if(frameStart == -1){
									jlog.trace("first boundary header not found, pos: {}", pos);
									continue;
								}
								jlog.trace("first boundary headers found");
								state= State.SECOND;
								off= frameStart;
								
							case SECOND:
								// frameStart now points to start of JPEG frame
								if ((pos = findString(b, off, b.length, boundary)) == -1){
									jlog.trace("second boundary not found, off: {}, len: {}", off, b.length);
									off= b.length - boundary.length();
									continue;
								}
								jlog.trace("second boundary found");
								break;
								
							default:
								throw new IllegalStateException("Unknown state: " + state);
						}

						state= State.FIRST;
						off= 0;
						
						// pos is the end of the frame
						int jpegSize = pos - frameStart;
						byte[] capture = new byte[jpegSize];
						System.arraycopy(b, frameStart, capture, 0, jpegSize);
						handleJPEG(capture, jpegSize);

						// put excess at start of new buffer
						baos.reset();
						baos.write(b, pos, b.length - pos);
					}
					bis.close();

				} catch (Exception e) {
					jlog.error("process() - Exception: {}", e.getMessage());
				}
			}
		}).start();
	}
	
	public void stop(){
		try {
			if(bis != null)
				bis.close();
			bis= null;
		} catch (IOException e) {
		}
	}
	
	// needs to be overriden, this is for debugging
	protected void handleJPEG(byte[] capture, int jpegSize) {
		long tm= System.currentTimeMillis();
		long delta= tm-lasttime;
		if(delta >= 1000){
			int fps= (int)(frameCnt/(delta/1000));
			frameCnt= 0;
			lasttime= tm;
			jlog.debug("Got jpeg frame of: {} bytes - {} fps", jpegSize, fps);
		}
	
		frameCnt++;
	}

	private int skipLines(byte[] b, int pos, int cnt) {
		int newpos= pos;
		while(newpos < b.length) {
			byte c = b[newpos++];
			if(c == 0x0a && --cnt <= 0)
				break;
		}
		return (newpos >= b.length) ? -1 : newpos;
	}

	// find the given string of bytes in the given buffer
	private int findString(byte[] buf, int off, int len, String string) {
		byte [] find= string.getBytes();
		int pos= indexOf(buf, off, len, find);
		return pos;
	}

	/**
	 * Search the data byte array for the first occurrence
	 * of the byte array pattern.
	 */
	private int indexOf(byte[] data, int off, int len, byte[] pattern) {
		int j = 0;

		for (int i = off; i < len; i++) {
			while (j > 0 && pattern[j] != data[i]) {
				j = failure[j - 1];
			}
			if (pattern[j] == data[i]) {
				j++;
			}
			if (j == pattern.length) {
				return i - pattern.length + 1;
			}
		}
		return -1;
	}

	/**
	 * Computes the failure function using a boot-strapping process,
	 * where the pattern is matched against itself.
	 */
	private int[] computeFailure(byte[] pattern) {
		int[] failure = new int[pattern.length];

		int j = 0;
		for (int i = 1; i < pattern.length; i++) {
			while (j>0 && pattern[j] != pattern[i]) {
				j = failure[j - 1];
			}
			if (pattern[j] == pattern[i]) {
				j++;
			}
			failure[i] = j;
		}

		return failure;
	}

	static class HTTPAuthenticator extends Authenticator {
	    private String username, password;

	    public HTTPAuthenticator(String user, String pass) {
	      username = user;
	      password = pass;
	    }

	    protected PasswordAuthentication getPasswordAuthentication() {
	      jlog.debug("Requesting Host  : " + getRequestingHost());
	      jlog.debug("Requesting Port  : " + getRequestingPort());
	      jlog.debug("Requesting Prompt : " + getRequestingPrompt());
	      jlog.debug("Requesting Protocol: " + getRequestingProtocol());
	      jlog.debug("Requesting Scheme : " + getRequestingScheme());
	      jlog.debug("Requesting Site  : " + getRequestingSite());
	      return new PasswordAuthentication(username, password.toCharArray());
	    }
	  }
}
