package com.e4net.rovio;

import java.util.Observable;

public class CameraWindow extends Observable{
	CameraDlg videoWindow = null;

	public void stopVideoWindow() {
		if (videoWindow != null)
			videoWindow.close();
		videoWindow = null;
	}

	public void startVideoWindow(final String sharename) {
		if (videoWindow != null)
			videoWindow.close();
		
		videoWindow = new CameraDlg("Camera " + sharename, new CamEventListener() {
			public void handleClosed() {
				videoWindow = null;
				setChanged();
				notifyObservers(new Object[]{"closed"});
			}
			public void handleMove(String command, int deltax, int deltay) {
				System.out.println("Move " + command + " " + deltax + ", " + deltay);
				setChanged();
				notifyObservers(new Object[]{"move", deltax, deltay});
			}
			public void handleZoom(boolean in, int delta) {
				System.out.println("zoom " + in + " " + delta);
				setChanged();
				notifyObservers(new Object[]{"zoom", delta});
			}
			public void handleStop(String string, boolean b) {
				setChanged();
				notifyObservers(new Object[]{b?"stop":"start", b});				
			}
		});
		videoWindow.setVisible(true);
	}

	public void display(byte [] img){
		if(videoWindow != null)
			videoWindow.setImage(img);
	}
}
