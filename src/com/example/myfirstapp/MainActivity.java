package com.example.myfirstapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build.VERSION;
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
import android.view.ViewGroup;
import java.math.*;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.animation.LayoutTransition;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.Keyframe;

import com.jjoe64.graphview.*;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.*;

import android.os.AsyncTask;
public class MainActivity extends Activity {
	private boolean isrecording;
	private boolean inversed;
	private ClapAnalyzer audioLogger;
	private AudioClipRecorder audioRecorder;
	private static final String TAG = "MainActivity";
	private int numberofclaps = 0;
	private Typeface robotoType;
	private MySurfaceView mySurfaceView;
	private SurfaceHolder mySurfaceHolder;

	
	protected ClapRecord recordTask;
	protected ClapAnalyze analyzeTask;
	private double baseline = 0;
	private boolean listenBase = true;
	private boolean surfaceCreated = false;
	private boolean micState = false;
	private NowLayout layout;
	
	private Claps claps;

	private LayoutTransition mTransition;
    Animator customAppearingAnim, customDisappearingAnim;
	
    Animator customChangingAppearingAnim, customChangingDisappearingAnim;

    private void createCustomAnimations(LayoutTransition transition) {

        // Changing while Adding
        PropertyValuesHolder pvhLeft =
                PropertyValuesHolder.ofInt("left", 0, 1);
        PropertyValuesHolder pvhTop =
                PropertyValuesHolder.ofInt("top", 0, 1);
        PropertyValuesHolder pvhRight =
                PropertyValuesHolder.ofInt("right", 0, 1);
        PropertyValuesHolder pvhBottom =
                PropertyValuesHolder.ofInt("bottom", 0, 1);
        PropertyValuesHolder pvhScaleX =
                PropertyValuesHolder.ofFloat("translationX", 1f, 0f, 1f);
        PropertyValuesHolder pvhScaleY =
                PropertyValuesHolder.ofFloat("translationY", 1f, 0f, 1f);
       customChangingAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(
                        this, pvhLeft, pvhTop, pvhRight, pvhBottom, pvhScaleX, pvhScaleY).
                setDuration(transition.getDuration(LayoutTransition.CHANGE_APPEARING));
        customChangingAppearingAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                View view = (View) ((ObjectAnimator) anim).getTarget();
                view.setScaleX(1f);
                view.setScaleY(1f);
            }
        });

		customChangingAppearingAnim = transition.getAnimator(LayoutTransition.CHANGE_APPEARING);

        // Changing while Removing
        Keyframe kf0 = Keyframe.ofFloat(0f, 0f);
        Keyframe kf1 = Keyframe.ofFloat(.9999f, 360f);
        Keyframe kf2 = Keyframe.ofFloat(1f, 0f);
        PropertyValuesHolder pvhRotation =
                PropertyValuesHolder.ofKeyframe("translationY", kf0, kf1, kf2);
        customChangingDisappearingAnim = ObjectAnimator.ofPropertyValuesHolder(
                        this, pvhLeft, pvhTop, pvhRight, pvhBottom, pvhRotation).
                setDuration(transition.getDuration(LayoutTransition.CHANGE_DISAPPEARING));
        customChangingDisappearingAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                View view = (View) ((ObjectAnimator) anim).getTarget();
                view.setRotation(0f);
            }
        });

        // Adding
        PropertyValuesHolder pvhTranslateY =
                PropertyValuesHolder.ofFloat("translationY",  300, 0);
        PropertyValuesHolder pvhAlpha =
                PropertyValuesHolder.ofFloat("alpha", 0, 1);

        customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, pvhTranslateY, pvhAlpha).
                setDuration(transition.getDuration(LayoutTransition.APPEARING));
        customAppearingAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                View view = (View) ((ObjectAnimator) anim).getTarget();
                view.setRotationY(0f);
            }
        });

        // Removing
        customDisappearingAnim = ObjectAnimator.ofFloat(null, "alpha", 1, 0).
                setDuration(transition.getDuration(LayoutTransition.DISAPPEARING)/2);
        customDisappearingAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                View view = (View) ((ObjectAnimator) anim).getTarget();
                view.setRotationX(0f);
            }
        });

    }

	private void setupCustomAnimations(LayoutTransition mTransition) {
		mTransition.setAnimator(LayoutTransition.APPEARING, customAppearingAnim);
		mTransition.setAnimator(LayoutTransition.DISAPPEARING, customDisappearingAnim);
		mTransition.setAnimator(LayoutTransition.CHANGE_APPEARING, customChangingAppearingAnim);
		mTransition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, customChangingDisappearingAnim);
		}

		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		inversed = false;
		isrecording = false;
		
		claps = new Claps();
		
		setContentView(R.layout.main);
		layout = (NowLayout) findViewById(R.id.mainLayout);
		robotoType = Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf");	
		TextView txt = (TextView) findViewById(R.id.infoHeading);
		txt.setTypeface(robotoType);
		mySurfaceView = (MySurfaceView) findViewById(R.id.waveform);
		mySurfaceHolder = mySurfaceView.getHolder();
		if (android.os.Build.VERSION.SDK_INT >= 14) {		
    		Animator customAppearingAnim, customDisappearingAnim;
    		Animator customChangingAppearingAnim, customChangingDisappearingAnim;
			mTransition = new LayoutTransition();
			layout.setLayoutTransition(mTransition);
		//
			Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_up_right);
			customAppearingAnim = mTransition.getAnimator(LayoutTransition.APPEARING);
			customDisappearingAnim = mTransition.getAnimator(LayoutTransition.DISAPPEARING);
			customChangingAppearingAnim = mTransition.getAnimator(LayoutTransition.CHANGE_APPEARING);
			customChangingDisappearingAnim = mTransition.getAnimator(LayoutTransition.CHANGE_DISAPPEARING);

			createCustomAnimations(mTransition);
			setupCustomAnimations(mTransition);
		}

		final ImageButton but = (ImageButton) findViewById(R.id.micButton);
		but.setBackgroundColor(Color.TRANSPARENT);
		but.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent me) {
				if (me.getAction() == MotionEvent.ACTION_DOWN) {
					if (listenBase) {
						but.setColorFilter(Color.BLUE);
					}
					else {
						but.setColorFilter(Color.RED);
					}
					return false;
				}
				else if (me.getAction() == MotionEvent.ACTION_UP) {
					if (listenBase) {
						but.setColorFilter(Color.BLUE);
					}
					else {
						but.setColorFilter(Color.RED);
					}
					return false;
				}
				return false;
			}
		});
		final ImageButton canc = (ImageButton) findViewById(R.id.cancelButton);
		canc.setBackgroundColor(Color.TRANSPARENT);
		canc.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent me) {
				if (me.getAction() == MotionEvent.ACTION_DOWN) {
					canc.setColorFilter(Color.RED);
					return false;
				}
				else if (me.getAction() == MotionEvent.ACTION_UP) {
					canc.setColorFilter(Color.DKGRAY);
					return false;
				}
				return false;
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isrecording) {
			audioLogger.setCancel(true);
		}
		numberofclaps = 0;
		listenBase = false;
		baseline = 0;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (isrecording) {
			audioLogger.setCancel(true);
		}
		mySurfaceView.terminateThread();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (mySurfaceHolder.getSurface().isValid()) {
			mySurfaceView.startThread();
		}
	}

	public void cancelRecord(View view) {
			/*	
				int num = 150;
				GraphViewData[] data = new GraphViewData[num];
				float st = 0;
				for (int i = 0; i < num; i++) {
					st+=0.2;
					data[i] = new GraphViewData(st, Math.sin(st));
				}
				displayGraph(R.layout.frcard, R.id.frText, R.id.frGraph, true, data, st, R.id.frGraphView);*/
		if (isrecording) {
			audioLogger.setCancel(true);
		}
	}

	public void startRecord(View view) {
		if (!isrecording) {
			Log.d(TAG, "in startRecord");
			final ImageButton but = (ImageButton) findViewById(R.id.micButton);
			if (listenBase) {
				but.setColorFilter(Color.BLUE);
				TextView txt = (TextView) findViewById(R.id.infoDescription);
				TextView txt2 = (TextView) findViewById(R.id.infoHeading);
				txt2.setText("Taking baseline.");
				txt.setText("Wait for a red icon and sound meter before clapping.");

			}
			else {
				but.setColorFilter(Color.RED);
			}
			mySurfaceView.setBaseline(listenBase);
		
			audioLogger = new ClapImpulseResponse(50*baseline, listenBase, baseline, mySurfaceView);
			audioRecorder = new AudioClipRecorder(audioLogger);
			recordTask = new ClapRecord();
			analyzeTask = new ClapAnalyze();
			mySurfaceView.setRecording();
			isrecording = true;
			analyzeTask.execute();
			recordTask.execute();
		}
	}

	public void addCard(int resource, ViewGroup root) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			LinearLayout child = (LinearLayout) inflater.inflate(resource, root);
	//		Animation anim;
//			if (inversed) {
//				anim = AnimationUtils.loadAnimation(this, R.anim.slide_up_right);
//			}
//			else {	
//				 anim = AnimationUtils.loadAnimation(this, R.anim.slide_up_left);
//			}
//			inversed = !inversed;
			layout.addView(child);
//			child.startAnimation(anim);

	}

	public void removeCard(int resource, ViewGroup root) {
		LinearLayout deadChild = (LinearLayout) findViewById(resource);

		layout.removeView(deadChild);

	}

	public void displayNumClaps() {
		View current = findViewById(R.id.countCard);
		if (current == null) {
			addCard(R.layout.clapcount, null);
			TextView txt = (TextView) findViewById(R.id.numclaps);
			txt.setTypeface(robotoType);
			}
		else {
			TextView txt = (TextView) findViewById(R.id.numclaps);
			txt.setText(numberofclaps + " claps.");
		}
		Log.d(TAG, "displaying clap");
	}

	public void displayGraph(int card, int resource, int graph, boolean line, int which, float end, int graphview) {
		View c = findViewById(resource);
		GraphView graphView = (GraphView) findViewById(graphview);
		if (c == null) {
			if (line) {
				graphView = new LineGraphView(this, "");
			}
			else {
				graphView = new BarGraphView(this, "");
			}
			switch (which) {
				case 0:
					graphView.addSeries(claps.getRoot().clapCurve);
					break;
				case 1:
					graphView.addSeries(claps.getRoot().freqDecay);
					break;
				case 2:
					graphView.addSeries(claps.getRoot().clapSpectra);
					break;
				case 3:
					graphView.addSeries(claps.getRoot().freqResponse);
					break;
			}
			
			graphView.setScalable(true);
			graphView.setScrollable(true);
			graphView.setViewPort(0, end);
			addCard(card, null);
			TextView txt = (TextView) findViewById(resource);
			txt.setTypeface(robotoType);

			LinearLayout g = (LinearLayout) findViewById(graph);
			g.addView(graphView);
			graphView.setId(graphview);
		}
		else {
			Clap current = claps.getRoot();
			Clap next = current.next;
			GraphViewData[] avg;
			GraphViewSeries avgseries;
			float[] avgs;

			switch (which) {
				case 0: 
					current.clapCurve.setStyle(Color.GRAY, 3);
					graphView.addSeries(current.clapCurve);
					next.clapCurve.setStyle(Color.LTGRAY, 3);
					next.clapCurve.setDescription("Previous samples");
					
					avg = new GraphViewData[current.clapData.length];
					avgs = claps.getAverage(0);

					for (int i = 0; i < avg.length; i++) {
						avg[i] = new GraphViewData(current.clapData[i].valueX, avgs[i]);
					}
					
					if (numberofclaps > 1)
						graphView.removeSeries(claps.clapCurve);
					
					avgseries = new GraphViewSeries("Average", null, avg);
					claps.clapCurve = avgseries;

					graphView.addSeries(avgseries);
					break;
				case 1:
					current.freqDecay.setStyle(Color.GRAY, 3);
					graphView.addSeries(current.freqDecay);
					next.freqDecay.setStyle(Color.LTGRAY, 3);
					next.freqDecay.setDescription("Previous samples");

					avg = new GraphViewData[current.freqDecayData.length];
					avgs = claps.getAverage(1);
					
					for (int i = 0; i < avg.length; i++) {
						avg[i] = new GraphViewData(current.freqDecayData[i].valueX, avgs[i]);
					}
					
					if (numberofclaps > 1)
						graphView.removeSeries(claps.freqDecay);
					
					avgseries = new GraphViewSeries("Average", null, avg);
					claps.freqDecay = avgseries;

					graphView.addSeries(avgseries);
					break;
				case 2:
					current.clapSpectra.setStyle(Color.GRAY, 3);
					graphView.addSeries(current.clapSpectra);
					next.clapSpectra.setStyle(Color.LTGRAY, 3);
					next.clapSpectra.setDescription("Previous samples");
					

					avg = new GraphViewData[current.clapSpecData.length];
					avgs = claps.getAverage(2);

					for (int i = 0; i < avg.length; i++) {
						avg[i] = new GraphViewData(current.clapSpecData[i].valueX, avgs[i]);
					}
					if (numberofclaps > 1)
						graphView.removeSeries(claps.clapSpectra);
					
					avgseries = new GraphViewSeries("Average", null, avg);
					claps.clapSpectra = avgseries;

					graphView.addSeries(avgseries);
					break;
				case 3:
					current.freqResponse.setStyle(Color.GRAY, 3);
					graphView.addSeries(current.freqResponse);
					next.freqResponse.setStyle(Color.LTGRAY, 3);
					next.freqResponse.setDescription("Previous samples");
					avg = new GraphViewData[current.freqRespData.length];
					avgs = claps.getAverage(3);

					for (int i = 0; i < avg.length; i++) {
						avg[i] = new GraphViewData(current.freqRespData[i].valueX, avgs[i]);
					}
					
					if (numberofclaps > 1)
						graphView.removeSeries(claps.freqResponse);
					
					avgseries = new GraphViewSeries("Average", null, avg);
					claps.freqResponse = avgseries;

					graphView.addSeries(avgseries);
					break;
			}
			

		}
		graphView.setShowLegend(true);
		graphView.setLegendAlign(LegendAlign.TOP);
		graphView.setLegendWidth(130);
		graphView.setViewPort(0, end);
		graphView.redrawAll();
	}
	
	private class ClapAnalyze extends AsyncTask<Void, Void, Integer> {
		private StatusUpdate record;
		private Clap clap;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			record = new StatusUpdate();
			clap = new Clap();
			
			record.base = !listenBase;
			record.done = true;
			record.clap_heard = true;
			record.RT60 = true;
			record.clapgraph = true;

			record.RT60s = true;
			record.dsSpectra = true;
			record.freqResp = true;
		}

		@Override
		protected void onPostExecute(Integer i) {
		}

		@Override
		protected void onProgressUpdate(Void... v) {
			super.onProgressUpdate(v);
			StatusUpdate status = audioLogger.getStatus();
			Results results = audioLogger.getResults();

			if (!status.base && !record.base) {
				Log.d(TAG, "false");
				ImageButton but = (ImageButton) findViewById(R.id.micButton);
				but.setColorFilter(Color.RED);
				addCard(R.layout.noiselevelcard, null);
				TextView txt = (TextView) findViewById(R.id.noiseLevel);
				txt.setTypeface(robotoType);
				record.base = !record.base;

				listenBase = false;
				baseline = audioLogger.getBaseline();
				double bl =audioLogger.getBaselineV();
				int roundOff = 2;
				BigDecimal bd = new BigDecimal(bl);
				bd = bd.setScale(roundOff, BigDecimal.ROUND_HALF_UP);
				bl = bd.doubleValue();
				txt.setText(bl + " dB.");
				mySurfaceView.setBaseline(listenBase);
			}


			if (status.RT60 && record.RT60) {
				View current = findViewById(R.id.reverbCard);	
				if (current == null) {
					addCard(R.layout.reverbcard, null);
				}
				TextView txtR = (TextView) findViewById(R.id.reverbTimeRecent);
				txtR.setTypeface(robotoType);
				


				BigDecimal rt = new BigDecimal(results.RT60);
				rt = rt.setScale(2, BigDecimal.ROUND_HALF_UP);
				double rtd = rt.doubleValue();
				
				TextView txt = (TextView) findViewById(R.id.reverbTimeDescription);
				TextView txtA = (TextView) findViewById(R.id.reverbTimeAverage);
				if (results.RT60 == -1) {
					txtR.setText("Too quiet.");
					txt.setText("The impulse must have enough energy to excite the room.");
				}
				else if (results.RT60 < 0) {
					txtR.setText(rtd + " seconds.");
					txt.setText("Overall reverberation time. Something has gone horribly wrong.");
				}
				else {
					float rtavg;
					numberofclaps++;
					clap.RT60 = results.RT60;
					clap.clapNumber = numberofclaps;
					claps.insert(clap);
					clap.inserted = true;
					float currentAvg = claps.getAverageRT60();
					float std = claps.std();
					if (currentAvg < 0) {
						rtavg = (float) rtd;
					}
					else {
						rtavg = currentAvg;
						BigDecimal rtav = new BigDecimal(rtavg);
						rtav = rtav.setScale(2, BigDecimal.ROUND_HALF_UP);
						rtavg = (float) rtav.doubleValue();
						BigDecimal st = new BigDecimal(std);
						st = st.setScale(2, BigDecimal.ROUND_HALF_UP);
						std = (float) st.doubleValue();
					}

					txtR.setText(rtd + " seconds.");
					txt.setText("Overall reverberation time.");
					txtA.setText("Mean: " + rtavg + ", Variance: " + std);
				}
				
				displayNumClaps();
				record.clap_heard = false;
				removeCard(R.id.infoCard, null);
				record.RT60 = false;
			}
			
			if (status.clapgraph && record.clapgraph) {
				if (clap.inserted) {
					int window = 6;
					GraphViewData[] data = new GraphViewData[results.clapData.length/window];
					float x = 0;
					for (int i = 0; i < data.length; i++) {
						x = ((float) (window*i*results.smoothwindow))/results.sampleRate;
						data[i] = new GraphViewData(x, results.clapData[window*i]);
					}
					clap.clapCurve = new GraphViewSeries("Most recent sample", null, data);
					clap.clapData = data;
					displayGraph(R.layout.clapcard, R.id.clapText, R.id.clapGraph, true, 0, x, R.id.clapGraphView);
					record.clapgraph = false;
				}
			}

			if (status.RT60s && record.RT60s) {
				GraphViewData[] data = new GraphViewData[results.RT60s.length];
				for (int i = 0; i < data.length; i++) {
					data[i] = new GraphViewData(results.freqs[i], results.RT60s[i]);
				}
				clap.freqDecay = new GraphViewSeries("Most recent sample", null, data);
				clap.freqDecayData = data;
				displayGraph(R.layout.fdecay, R.id.fdText, R.id.fdGraph, true, 1, results.freqs[results.freqs.length-1], R.id.fdGraphView);
				record.RT60s = false;
			}
			
			if (status.dsSpectra && record.dsSpectra) {
				GraphViewData[] data = new GraphViewData[results.dsSpectra.length];
				for (int i = 0; i < data.length; i++) {
					data[i] = new GraphViewData(results.freqs[i], results.dsSpectra[i]);
				}
				clap.clapSpectra = new GraphViewSeries("Most recent sample", null, data);
				clap.clapSpecData = data;
				displayGraph(R.layout.clapspec, R.id.csText, R.id.csGraph, true, 2, results.freqs[results.freqs.length-1], R.id.csGraphView);
				record.dsSpectra = false;
			}
			
			if (status.freqResp && record.freqResp) {
				GraphViewData[] data = new GraphViewData[results.freqResp.length];
				for (int i = 0; i < data.length; i++) {
					data[i] = new GraphViewData(results.freqs[i], results.freqResp[i]);
				}
				clap.freqResponse = new GraphViewSeries("Most recent sample", null, data);
				clap.freqRespData = data;
				displayGraph(R.layout.frcard, R.id.frText, R.id.frGraph, true, 3, results.freqs[results.freqs.length-1], R.id.frGraphView);
				record.freqResp = false;
			}

			if (status.done && record.done) {
				ImageButton but = (ImageButton) findViewById(R.id.micButton);
				but.setColorFilter(Color.DKGRAY);
				mySurfaceView.stopRecording();
				record.done = false;
			}
		}

	


		
		
		@Override
		protected Integer doInBackground(Void... v) {
			while (isrecording) {
				publishProgress();	
			}
			publishProgress();
			return 0;
	}
		
}

	private class Claps {
		public Clap root;
		public GraphViewSeries clapCurve;
		public GraphViewSeries clapSpectra;
		public GraphViewSeries freqResponse;
		public GraphViewSeries freqDecay;

		public Claps() {
			this.root = null;
		}

		public Clap getRoot() {
			return this.root;
		}

		public float getAverageRT60() {
			if (root == null) {
				Log.d(TAG, "no claps yet");
				return -1;
			}
			Clap current = root;
			float sum = 0;
			int num = 0;
			while (current != null) {
				sum += current.RT60;
				num++;
				current = current.next;
			}
			return sum/num;
		}
		
		public float[] getAverage(int which) {
			Clap current = root;
			int num = 0;
			
			float[] summed = new float[2];
			
			switch (which) {
				case 0:
					Clap longest = root;
					int max = 0;
					while (longest != null) {
						if (longest.clapData.length > max) {
							max = longest.clapData.length;	
						}
						longest = longest.next;
					}
					summed = new float[max];
					break;
				case 1:
					summed = new float[current.freqDecayData.length];
					break;
				case 2:
					summed = new float[current.clapSpecData.length];
					break;
				case 3:
					summed = new float[current.freqRespData.length];
					break;
			}

			for (int i = 0; i < summed.length; i++) {
				summed[i] = 0;
			}

			float c;
			while (current != null) {
				switch (which) {
					case 0:
						for (int i = 0; i < summed.length; i++) {
							if (i >= current.clapData.length) {
								c = 0;
							}
							else {
								c = (float) current.clapData[i].valueY;
							}
							summed[i] += c;
						}
						break;
					case 1:
						for (int i = 0; i < summed.length; i++) {
							c = (float) current.freqDecayData[i].valueY;
							summed[i] += c;
						}
						break;
					case 2:
						for (int i = 0; i < summed.length; i++) {
							c = (float) current.clapSpecData[i].valueY;
							summed[i] += c;
						}
						break;
					case 3:
						for (int i = 0; i < summed.length; i++) {
							c = (float) current.freqRespData[i].valueY;
							summed[i] += c;
						}
						break;
				}
				current = current.next;
				num++;
			}

			for (int i = 0; i < summed.length; i++) {
				summed[i] = summed[i] / num;
			}	
			return summed;
		}

		public float std() {
			if (root == null) {
				Log.d(TAG, "no claps yet");
				return -1;
			}
			Clap current = root;
			float sum = 0;
			float sqrs = 0;
			int num = 0;
			while (current != null) {
				sum += current.RT60;
				sqrs += current.RT60*current.RT60;
				num++;
				current = current.next;
			}
			float lower = (sum*sum)/num;
			float std = (float) Math.sqrt((sqrs - lower)/num);
			return std;
		}

		public void display() {
			Clap current = root;
			while (current != null) {
				Log.d(TAG, current.clapNumber + " -> ");
				current = current.next;
			}
		}

		public void insert(Clap ins) {
			ins.next = root;
			root = ins;
		}

	}

	private class Clap {
		public GraphViewSeries freqDecay;
		public GraphViewData[] freqDecayData;

		public GraphViewSeries freqResponse;
		public GraphViewData[] freqRespData;

		public GraphViewSeries clapCurve;
		public GraphViewData[]  clapData;

		public GraphViewSeries clapSpectra;
		public GraphViewData[] clapSpecData;

		public float RT60;
		public int clapNumber;
		public Clap next;
		public boolean inserted;

		public Clap() {
			this.inserted = false;
		}
	}

	private class ClapRecord extends AsyncTask<Void, Void, Integer> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Integer i) {
		}

		@Override
		protected void onProgressUpdate(Void... v) {
		}

	


		
		
		@Override
		protected Integer doInBackground(Void... v) {
			Log.d(TAG, "starting record");
			boolean heard = audioRecorder.startRecordingForTime(1, 
									AudioClipRecorder.RECORDER_SAMPLERATE_CD, 
									AudioFormat.ENCODING_PCM_16BIT);
			if (heard) {
				if (!audioLogger.getCancel()) {
					audioLogger.process();
				}
			}
			isrecording = false;
			return 0;
	}
}

}
