package com.e4net.rovio;

public interface CamEventListener extends java.util.EventListener{
    void handleClosed();
    void handleStop(String string, boolean b);
	void handleMove(String command, int deltax, int deltay);
    void handleZoom(boolean in, int delta);
}