package it.beppi.knobselector;

import android.os.Handler;

public class BTimer { 
  private Handler handler; 
  private Runnable tickHandler, delegate;
	  
  private long interval; 
  public long getInterval() { return interval; } 
  public void setInterval(long delay) { interval = delay; } 
  
  private boolean ticking; 
  public boolean isTicking(){ return ticking; }
  
  public BTimer(long interv) 
  { 
    interval = interv; 
    handler = new Handler(); 
  } 
  
  public BTimer(long interv, Runnable onTickHandler) 
  { 
    interval = interv; 
    setOnTickHandler(onTickHandler); 
    handler = new Handler(); 
  } 
  
  public void start(long interv, Runnable onTickHandler) 
  { 
    if (ticking) return; 
    interval = interv; 
    setOnTickHandler(onTickHandler); 
    handler.postDelayed(delegate, interval); 
    ticking = true; 
  } 
    
  public void start() 
  { 
    if (ticking) return; 
    handler.postDelayed(delegate, interval); 
    ticking = true; 
  } 
  
  public void stop() 
  { 
    handler.removeCallbacks(delegate); 
    ticking = false; 
  } 
  
  public void setOnTickHandler(Runnable onTickHandler) 
  { 
    if (onTickHandler == null) 
      return; 
    
    tickHandler = onTickHandler; 
    
    delegate = new Runnable() { 
      public void run() 
      { 
        if (tickHandler == null) return; 
        handler.postDelayed(delegate, interval); 
        tickHandler.run(); 
      } 
    }; 
  } 
}