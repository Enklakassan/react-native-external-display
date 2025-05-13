package com.externaldisplay;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.hardware.display.DisplayManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import java.util.Map;
import java.util.HashMap;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class ExternalDisplayScreen extends Presentation {
  private Activity mainActivity;
  private boolean preventFocusCapture = false; // Default to true for backward compatibility

  ExternalDisplayScreen(Context ctx, Display display) {
    super(ctx, display);
    // Store the main activity if ctx is an Activity
    if (ctx instanceof Activity) {
      this.mainActivity = (Activity) ctx;
    }
  }

  /**
   * Set whether focus capture should be prevented
   * @param prevent If true, prevent the screen from capturing focus
   */
  public void setPreventFocusCapture(boolean prevent) {
    this.preventFocusCapture = prevent;
    // Update the window flags if we've already created the window
    if (getWindow() != null) {
      updateWindowFlags();
    }
  }

  /**
   * Update the window flags based on the current preventFocusCapture setting
   */
  private void updateWindowFlags() {
    Window window = getWindow();
    if (window != null) {
      if (preventFocusCapture) {
        // FLAG_NOT_FOCUSABLE prevents this window from ever taking focus
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
      } else {
        // Clear the FLAG_NOT_FOCUSABLE flag to allow focus
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Apply focus prevention if enabled
    updateWindowFlags();
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    // If we're preventing focus capture, don't handle key events
    if (preventFocusCapture) {
      return false;
    }
    return super.dispatchKeyEvent(event);
  }
  
  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    // Process the touch event
    boolean handled = super.dispatchTouchEvent(event);
    
    // When a touch happens and we're preventing focus capture, redirect focus to main activity
    if (preventFocusCapture && mainActivity != null && event.getAction() == MotionEvent.ACTION_DOWN) {
      View mainView = mainActivity.getCurrentFocus();
      if (mainView != null) {
        mainView.requestFocus();
      }
    }
    
    return handled;
  }
}

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class ExternalDisplayHelper implements DisplayManager.DisplayListener {
  public static Map<String, Object> getScreenInfo(Display[] displays) {
    HashMap<String, Object> info = new HashMap<String, Object>();
    for (Display display : displays) {
      int displayId = display.getDisplayId();
      if (
        display.getDisplayId() == Display.DEFAULT_DISPLAY ||
        (display.getFlags() & Display.FLAG_PRESENTATION) == 0
      ) {
        continue;
      }
      HashMap<String, Object> data = new HashMap<String, Object>();
      DisplayMetrics displayMetrics = new DisplayMetrics();
      display.getMetrics(displayMetrics);
      data.put("id", displayId);
      data.put("width",  displayMetrics.widthPixels);
      data.put("height", displayMetrics.heightPixels);
      info.put(String.valueOf(display.getDisplayId()), data);
    }
    return info;
  }

  public interface Listener {
    void onDisplayAdded(Display[] displays, int displayId);
    void onDisplayChanged(Display[] displays, int displayId);
    void onDisplayRemoved(Display[] displays, int displayId);
  }

  private Listener listener = null;
  private DisplayManager dm = null;
  private Display displays = null;

  public ExternalDisplayHelper(Context context, Listener listener) {
    this.listener = listener;

    dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    dm.registerDisplayListener(this, null);
  }

  public Display getDisplay(int displayId) {
    return dm.getDisplay(displayId);
  }

  public Display[] getDisplays() {
    return dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
  }

  @Override
  public void onDisplayAdded(int displayId) {
    listener.onDisplayAdded(getDisplays(), displayId);
  }

  @Override
  public void onDisplayChanged(int displayId) {
    listener.onDisplayChanged(getDisplays(), displayId);
  }

  @Override
  public void onDisplayRemoved(int displayId) {
    listener.onDisplayRemoved(getDisplays(), displayId);
  }
}
