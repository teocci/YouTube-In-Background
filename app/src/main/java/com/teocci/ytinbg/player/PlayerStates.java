package com.teocci.ytinbg.player;

public class PlayerStates {
	 /**
     * Playing state which can either be stopped, playing, or reading the header before playing
     */
	public static final int	READY_TO_PLAY = 2;
	public static final int PLAYING = 3; 
	public static final int STOPPED = 4; 
    public int playerState = STOPPED;
    
    public int get() {
    	return playerState;
    }
    
    public void set(int state) { 
    	playerState = state;
    }

    /**
     * Checks whether the player is ready to play, this is the state used also for Pause (phase 2)
     *
     * @return <code>true</code> if ready, <code>false</code> otherwise
     */
    public synchronized boolean isReadyToPlay() {
        return playerState == PlayerStates.READY_TO_PLAY;
    }
    
    
    /**
     * Checks whether the player is currently playing (phase 3)
     *
     * @return <code>true</code> if playing, <code>false</code> otherwise
     */
    public synchronized boolean isPlaying() {
        return playerState == PlayerStates.PLAYING;
    }
    
    
    /**
     * Checks whether the player is currently stopped (not playing)
     *
     * @return <code>true</code> if playing, <code>false</code> otherwise
     */
    public synchronized boolean isStopped() {
        return playerState == PlayerStates.STOPPED;
    }
}
