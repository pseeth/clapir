package com.example.myfirstapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.content.Context;
import android.util.Log;
import root.gast.audio.record.*;
import root.gast.audio.interp.*;
import android.widget.TextView;
import android.widget.ImageButton;
import android.graphics.Typeface;
import android.graphics.Color;
import android.media.AudioFormat;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class DisplayThread extends Thread {
	boolean keepRunning = true;
	boolean baseline = true;

	public void setBaseline(boolean val) {
		baseline = val;
	}

	@Override
	public void run() {
		while (keepRunning) {
		}

	}
}
