
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
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class MySurfaceView extends SurfaceView implements Callback {
	SurfaceHolder mSurfaceHolder;	
	DrawingThread mThread;
	private boolean recording;
	private int[] history;
	private int historyIndex;
	private int width;
	private int height;
	private boolean validhistory;
	private boolean baseline;
	private boolean surfaceCreated;
	static final float ALPHA= 0.15f;

	Smooth s;
		
	private static final String TAG = "MySurfaceView";
		public MySurfaceView(Context context, AttributeSet attributeSet) {
			super(context, attributeSet);
			s = new Smooth();
			mSurfaceHolder = getHolder();
			getHolder().addCallback(this);
			mThread = new DrawingThread();
			history = new int[44];
			historyIndex = 0;
			for (int i = 0; i < history.length; i++) {
				history[i] = height/2;
			}
			validhistory = false;
			recording = false;
		}
		

		public void setRecording() {
			recording = true;
		}

		public void stopRecording() {
			recording = false;
			for (int i = 0; i < history.length; i++) {
				history[i] = height/2;
			}
		}

		public void fromClap(short[] data) {
			for (int i = 0; i < data.length; i++) {
				history[i] = data[i];
			}
			validhistory = true;
		}

		public void setBaseline(boolean bl) {
			baseline = bl;
		}


		public void drawData(Canvas c, Paint p) {
			
			float mean, current;
			mean = 0;
			float thresh = 60;
			for (int i = 0; i < history.length; i++) {
				current = 20* ((float) Math.log10((float) (Math.abs(history[i]))/Math.pow(2,15)));
				if (current < -1*thresh) {
					current = -1*thresh;
				}
				mean += current;
			}
						
			mean /= history.length;
			mean += thresh;

			Paint p2 = new Paint();
			p2.setStrokeWidth(3);
			p2.setColor(Color.LTGRAY);
			
			float wcolor = (float) (mean/thresh)*width;

			c.drawLine(0, (float) height/2, wcolor, (float) height/2, p);
			c.drawLine(wcolor, (float) height/2, width, (float) height/2, p2);

			
		/*	
			float max = 0;
			float[] data = new float[history.length];
			float current;
			for (int i = 0; i < history.length; i++) {
				current = Math.abs(history[i]);
				if (current > max) {
					max = current;
				}
			}

			for (int i = 0; i < history.length; i++) {
				data[i] = ((float)history[i]) / max;
			}
			
			int skip = 10;
			
			float[] smooth = s.smooth(data, skip);

			float[] points = new float[2*smooth.length/skip];
			for (int i = 0; i < points.length-1; i = i + 2) {
				points[i] = width*((float) i)/points.length;
				points[i+1] = height*(smooth[i]/2)+height/2;
			}
			
			c.drawLines(points, 0, points.length, p);*/
		}

		protected void onDraw(Canvas canvas) {
			Paint p = new Paint();
			p.setStrokeWidth(3);
			canvas.drawColor(Color.WHITE);
			if (recording && validhistory) {
				if (baseline) {
					p.setColor(Color.BLUE);
				}
				else {
					p.setColor(Color.RED);
				}
				drawData(canvas, p);
			}
			else {
				p.setColor(Color.LTGRAY);
				canvas.drawLine(0, (float) height/2, width, (float) height/2, p);
			}
		}

		public void startThread() {

			if (mThread.getState() == Thread.State.TERMINATED) {
				mThread = new DrawingThread();
			}
			mThread.keepRunning = true;
			mThread.start();
		}

		public void surfaceCreated(SurfaceHolder holder) {
			Log.d(TAG, "creating surface");
			Canvas c = getHolder().lockCanvas();
			width = c.getWidth();
			height = c.getHeight();
			getHolder().unlockCanvasAndPost(c);
			
			startThread();

		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.d(TAG, "destroying surface");
			terminateThread();
		}

		public void terminateThread() {

			mThread.keepRunning = false;
			boolean retry = true;
			while (retry) {
			try {
				mThread.join();
				retry = false;
			}
			catch (InterruptedException e) {
			}
			}
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.d(TAG, "changing surface");
		}

		private class DrawingThread extends Thread {
			boolean keepRunning = true;

			@Override
			public void run() {
				Canvas c;
				while (keepRunning) {
					c = null;
					try {
						c = mSurfaceHolder.lockCanvas();
						synchronized (mSurfaceHolder) {
							onDraw(c);
						}
					} finally {
						if (c != null) {
							mSurfaceHolder.unlockCanvasAndPost(c);
						}
					}

					try {
						Thread.sleep(10);
					}	catch (InterruptedException e) {}
				}
			}
		}
	}
