package com.seeth.clapir;

import root.gast.audio.record.*;
import android.util.Log;
import android.graphics.Color;
import edu.emory.mathcs.jtransforms.fft.*;

public class ClapImpulseResponse implements ClapAnalyzer
{
    private static final String TAG = "ClapImpulseResponse";

    private double volumeThreshold;
	public static int SampleRate;

    public static final int DEFAULT_LOUDNESS_THRESHOLD = 200000;	
	
	private int numSamples = 0;
	private int clapStart;
	private int clapEnd;
	private int clapTime;

	private StatusUpdate status;
	private Results results;

	private boolean clap_heard;
	private boolean done;

    private static final boolean DEBUG = false;
	
	private double baselineRMS;
	private double baselineV;

	private boolean listeningtobase;
	private boolean captureNextSecond = false;
	int startTime = 0;
	private int afterClap = 10;
	
	private short[] history;
	private short[] base;
	private int baseIndex = 0;
	private boolean cancel;

	private boolean recordhistory;
	private int currentIndex = 0;

	private MySurfaceView mySurfaceView;
	
	public boolean clap_heard() {
		return clap_heard;
	}

	public boolean done() {
		return done;
	}

	public double getBaselineV() {

		return 20*Math.log10(baselineRMS/Math.pow(2,15));
	}

	public StatusUpdate getStatus() {	
		return status;
			}

    public ClapImpulseResponse()
    {
        volumeThreshold = DEFAULT_LOUDNESS_THRESHOLD;
		listeningtobase = true;
		baselineRMS = 0;
		history = new short[44100*4];
		base = new short[44*1000];
    }

	public void setCancel(boolean cancel) {
		this.cancel = cancel;
	}

	public boolean getCancel() {
		return this.cancel;
	}

    public ClapImpulseResponse(double volumeThreshold, boolean listeningtobase, double baselineRMS, MySurfaceView mySurfaceView)
    {
        this.volumeThreshold = volumeThreshold;
		this.listeningtobase = listeningtobase;
		this.baselineRMS = baselineRMS;
		this.mySurfaceView = mySurfaceView;
		this.clap_heard = false;


		this.done = false;
		this.cancel = false;
		history = new short[44100];
		
		for (int i = 0; i < history.length; i++) {
			history[i] = 0;
		}

		base = new short[44*1000];

		recordhistory = false;
		
		status = new StatusUpdate();
		results = new Results();

		status.base = listeningtobase;
		status.clap_heard = false;
		status.done = false;
		status.RT60 = false;
		status.clapgraph = false;
    }

	@Override
	public short[] getData() {
		return history;
	}

	@Override  
	public void dispHistory() {
		short[] chunk = new short[44];
		for (int i = 0; i < history.length-44; i = i + 44) {
			for (int j = 0; j < 44; j++) {
				chunk[j] = history[i+j];
			}
			//Log.d(TAG, "rms of chunk " + i/44 + "is " + rootMeanSquared(chunk));
		}
	}

	@Override
	public double getBaseline() {
		return baselineRMS;
	}
	
	public boolean isListeningBase() {
		return listeningtobase;
	}


    @Override
    public boolean heard(short[] data, int sampleRate)
    {

		SampleRate = sampleRate;
		copyIntoBuffer(data);
		results.sampleRate = sampleRate;
		mySurfaceView.fromClap(data);
        boolean heard = false;
		//use the first 1000 ms to obtain a baseline measurement for the room
		
		if (listeningtobase) {
			if (numSamples >= 1000) {
				baselineRMS = rootMeanSquared(base);
				status.base = false;
				recordhistory = true;
				listeningtobase = false;
				volumeThreshold = baselineRMS*50;
			}
			else {
				for (int i = baseIndex; i < baseIndex + data.length; i++) {
					base[i] = data[i % data.length];
				}
				baseIndex += data.length;
			}
		}
		else {
			double currentRMS = rootMeanSquared(data);
			if (!clap_heard) {
				if (currentRMS > volumeThreshold) {
					//Log.d(TAG, "heard a clap at rms: " + currentRMS);
					clapStart = data.length*numSamples;
					clap_heard = true;
					status.clap_heard = true;
				}
			}
			else { 
				if (currentRMS < 2*baselineRMS) {
					clapTime = data.length*numSamples - clapStart;
					//Log.d(TAG, "clap lasted for: " + clapTime);
		//			currentIndex -= chunkSize;
					heard = true;
				}
			}
		}
		

/*		if (captureNextSecond) {
			currentIndex += chunkSize;
			if (numSamples >  startTime + AFTERCLAP) { //collect a bit more
			}
		}*/
		
		
		if (cancel) {
			//Log.d(TAG, "canceling record");
			status.done = true;
			heard = true;
		}
		numSamples++;
		currentIndex += data.length;
		return heard;
    }

	public void copyIntoBuffer(short[] data) {
		int chunkSize = data.length;
		boolean breakout = false;
		int wrap = 0;
		for (int i = currentIndex; i < currentIndex + chunkSize; i++) {
			if (i >= history.length) {
				breakout = true;
				wrap = i;
				break;
			}
			history[i] = data[i % chunkSize];
		}

		if (breakout) {
			currentIndex = 0;
			wrap = wrap % chunkSize;
			for (int i = currentIndex; i < data.length - wrap; i++) {
				history[i] = data[i+wrap];
			}
		}
	}

	public void process() {
		status.done = true;
		done = true;
		clapTime = clapTime;
		float[] clapdata = new float[clapTime];
		int j = 0;

		int startIndex;
		int endIndex = currentIndex;
		if (endIndex <= clapTime) {
			//Log.d(TAG, "in the weird case");
			startIndex = (endIndex - clapTime) % history.length;
			startIndex += history.length;
			//Log.d(TAG, startIndex + " to " + endIndex + " clapTime = " + clapTime);
			for (int i = startIndex; i < history.length; i++) {
				clapdata[j] = (float) history[i] / ((float) (Math.pow(2, 15)));
				j++;
			}
			for (int i = 0; i < endIndex; i++) {
				clapdata[j] = (float) history[i] / ((float) (Math.pow(2, 15)));
				j++;
	}
		}
		else {
			//Log.d(TAG, "in the normal case");
			startIndex = endIndex - clapTime;
			for (int i = startIndex; i < endIndex; i++) {
				clapdata[j] = (float) history[i] / ((float) (Math.pow(2,15)));
				j++;
			}
		}

		float RT60 = calcReverb(clapdata, true);
		results.RT60 = RT60;
		status.RT60 = true;
		if (RT60 < 0) {
			//Log.d(TAG, "Something went wrong");
			return;
		}
		status.clapgraph = true;

		Curve hamm = new Curve(spectrumResolution);
		hamm.hamm(false);
		
		int FFTlog = (int) (Math.log(spectrumResolution)/Math.log(2));
			
		int size = (int) Math.floor((Math.log(clapdata.length)/Math.log(2)));
		int paddedSize = (1 << (size+1));
		
		float[] paddedClap = new float[paddedSize];
		int i = 0;
		//pad signal to nearest power of two above size
		for (; i < clapdata.length; i++) {
			paddedClap[i] = clapdata[i];
		}
		for (; i < paddedClap.length; i++) {
			paddedClap[i] = 0;
		}
		
	
		int depth = OVERLAP*paddedSize/spectrumResolution;
		float[][] spectrogram = new float[depth][spectrumResolution];
		int stepSize = spectrumResolution/OVERLAP;
		/*
		FFT fft = new FFT();
		
		Complex[] current = new Complex[spectrumResolution];
		Complex[] spec = new Complex[spectrumResolution];
		int row = 0;
		
		for (int k = 0; k < paddedSize - spectrumResolution; k = k + stepSize) {
			for (int c = 0; c < spectrumResolution; c++) {
				current[c] = new Complex(hamm.array[c]*paddedClap[c+k], 0);
			}
			spec = fft.fft(current);
			for (int c = 0; c < spectrumResolution; c++) {
				spectrogram[row][c] = (float) spec[c].abs();
			}
			row++;
		}
		*/

		float[] current = new float[2*spectrumResolution];
		FloatFFT_1D fft = new FloatFFT_1D(spectrumResolution);
		int row = 0;

		for (int k = 0; k < paddedSize - spectrumResolution; k = k + stepSize) {
			for (int c = 0; c < spectrumResolution; c++) {
				current[c] = hamm.array[c]*paddedClap[c+k];
			}
			fft.realForwardFull(current);
			for (int c = 0; c < spectrumResolution-1; c++) {
				spectrogram[row][c] = (float) Math.hypot(current[2*c], current[2*c+1]);
			}
			row++;
		}

		float[] RT60s = new float[numFreqs];
		float[] freqs = specFreq();
		results.freqs = new float[numFreqs];
		float[] freqSpec = new float[depth];
		factor = (clapdata.length/stepSize)*SampleRate/OVERLAP;
		int freq;
		for (int f = 0; f < RT60s.length; f++) {
			freq = (int) Math.floor((freqs[f]/(SampleRate/2))*spectrumResolution);
			for (int k = 0; k < freqSpec.length; k++) {
				freqSpec[k] = spectrogram[k][freq];
			}
			RT60s[f] = calcReverb(freqSpec, false);
			if (RT60s[f] < 0) {
				RT60s[f] = 0;
			}
			results.freqs[f] = freq;
		}

		results.RT60s = RT60s;
		status.RT60s = true;
		
		Curve dsSpectra = new Curve(numFreqs);
		float energy;
		for (int f = 0; f < numFreqs; f++) {
			freq = (int) Math.floor((freqs[f]/(SampleRate/2))*spectrumResolution);
			energy = 0;
			for (int k = 0; k < Math.floor(.1*freqSpec.length); k++) {
				energy += spectrogram[k][freq];
			}
			dsSpectra.array[f] =  energy/(float)Math.floor(.1*freqSpec.length);
		}

		float reference = (float) baselineRMS/(float) Math.pow(2,15);
		dsSpectra.dbConvert(reference, true);
		results.dsSpectra = dsSpectra.array;
		status.dsSpectra = true;
		
		Curve freqResp = new Curve(numFreqs);
		float reverbEnergy;

		for (int f = 0; f < numFreqs; f++) {
			freq = (int) Math.floor((freqs[f]/(SampleRate/2))*spectrumResolution);
			energy = 0;
			for (int k = (int) Math.floor(.1*freqSpec.length); k < depth; k++) {
				energy += spectrogram[k][freq];
			}
			reverbEnergy =  energy/(float)(depth-Math.floor(.1*freqSpec.length));
			freqResp.array[f] = reverbEnergy / dsSpectra.array[f];
		}

		freqResp.dbConvert(reference, true);
		results.freqResp = freqResp.array;
		status.freqResp = true;
	}

	private float[] specFreq() {
		float[] result = new float[numFreqs];
		
		double sqrt2 = Math.sqrt(2);
		double sqrtsqrt2 = Math.sqrt(sqrt2);
		double x = 22.09708691207964; //1000/(sqrt(2)^11)
		for (int i = 0; i < numFreqs; i++) {
			result[i] = (float) x;
			x = x * sqrtsqrt2;
		}
		return result;
	}

	private static int numFreqs = 40;
	private static int spectrumResolution = 1024;
	private static int OVERLAP = 20;
	private static double directSoundLength = .01; //seconds
	private double spectrumTime = .1;
	private float factor = 1;
	
	private float calcReverb(float[] c, boolean overall) {
		Curve curve = new Curve(c);
		int claptime = c.length;
		float result;
		int directSoundSamples;
		float minsec = .05f; //seconds
		int minsamp = (int)Math.ceil(minsec/.01);

		if (overall) {
			directSoundSamples = (int)Math.floor(directSoundLength*SampleRate);
			int window = 44;
			claptime = (int) Math.floor(claptime/window);
			float reference = (float) baselineRMS/(float) Math.pow(2,15);
			curve.dbConvert(reference, true);
			Smooth s = new Smooth();
			curve.array = s.smooth(curve.array, window);
			curve.length = curve.array.length;
			directSoundSamples = (int) (Math.floor(directSoundSamples/window));
			results.clapData = curve.array;
			results.clapLength = curve.length;
			results.smoothwindow = window;
			factor = SampleRate/ ((float)window);
			if (directSoundSamples >= curve.length) {
				//Log.d(TAG, "Signal was too quiet, direct sound case.");
				return -1;
			}
		}
		else {
			Smooth s = new Smooth();
			curve.array = s.smooth(curve.array, 1);
			curve.length = curve.array.length;
			directSoundSamples = (int)Math.floor(curve.length * .2);
		}
		
		

		float directSoundSum = curve.sum(0, directSoundSamples);
		float tailSum = curve.sum(claptime-directSoundSamples, claptime);
		float decayEstimate = (directSoundSum - tailSum) / directSoundSamples;
		if (overall) {
		if (decayEstimate < 10) {
			//Log.d(TAG, "Signal was too quiet, decayEstimate case.");
			return -1;
		}
		}
		
		if (tailSum == Float.NEGATIVE_INFINITY) {
			//Log.d(TAG, "Signal was too quiet, NEGATIVE INFINITY case.");
			return -1;
		}
		
		Knee best = new Knee();
		Curve rSound = curve.subset(directSoundSamples, claptime);
		best = findKnee(rSound, minsamp);
		
		float slope = best.fit.slope;
		slope = slope*factor;
		
		result = -60/slope; //calculate RT60
		return result;
	}
    private double rootMeanSquared(short[] nums)
    {
        double ms = 0;
        for (int i = 0; i < nums.length; i++)
        {
            ms += nums[i] * nums[i];
        }
        ms /= nums.length;
        return Math.sqrt(ms);
    }

	
	private class Curve {
		public float[] array;
		public int length;


		//Make a Curve of size length
		public Curve(int length) {
			this.array = new float[length];
			this.length = length;
		}

		//Turn a float array into a Curve
		public Curve(float[] array) {
			this.array = new float[array.length];
			for (int i = 0; i < array.length; i++) {
				this.array[i] = array[i];
			}
			this.length = array.length;
		}

		//Create a float array of size length filled with floats a
		public void fill(float a) {
			for (int i = 0; i < length; i++) {
				array[i] = a;
			}
			this.length = length;
		}

		//generates a hamming window for this curve.
		public void hamm(boolean halfFlag) {
			int end = this.length;
			float item;
			if (halfFlag) {
				end = (int) Math.ceil(this.length/2);
			}
			for (int i = 0; i < end; i++) {
				item = (float) (.54 - .46*Math.cos(Math.PI*i/end));
				this.array[i] = item;
			}
		}

		public void ramp(float init, int sign, float slope) {
			for (int i = 0; i < length; i++) {
				array[i] = init;
				init += slope;
			}
			this.length = length;
		}

		private void square(int size) {
			for (int i = 0; i < size; i++) {
				array[i] = array[i] * array[i];
			}
		}

		//DOES NOT CHANGE THIS OBJECT
		public Curve mult(Curve B, int size) {
		/*	if (this.length != B.length || this.length != result.length) {
				//throw some exception here
				return 0;
			}*/
			Curve result = new Curve(size);
			for (int i = 0; i < size; i++) {
				result.array[i] = this.array[i]*B.array[i];
			}
			return result;
		}

		//DOES NOT CHANGE THIS OBJECT
		//computes this.array - B.array and returns as a Curve
		public Curve subtract(Curve B, int size) {
			Curve result = new Curve(size);
			for (int i = 0; i < size; i++) {
				result.array[i] = this.array[i] - B.array[i];
			}
			return result;
		}

		private float sum(int first, int last) {
			float sum = 0;
			for (int i = first; i < last; i++) {
				sum = sum + array[i];
			}
			return sum;
		}

		private void dbConvert(float reference, boolean power) {
			float alpha, beta;
			if (power) {
				alpha = 10;
			}
			else {
				alpha = 20;
			}
			for (int i = 0; i < length; i++) {
				array[i] = alpha*((float) Math.log10(Math.abs(array[i])/reference));
			}
		}

		private Curve subset(int start, int end) {
			Curve result = new Curve(end-start);
			for (int i = start; i < end; i++) {
				result.array[i-start] = this.array[i];
			}
			return result;
		}

	}

	private class Fit {
		float slope;
		float yIntercept;
	}

	private class Knee {
		Fit fit;
		float rmsError;
		int prefix;
	}
	
	private Fit regression(Curve curve, int size) {
		Fit result = new Fit();

		float meanX = (size-1)/2;
		float meanY = curve.sum(0, size)/size;
		
		Curve xSq = new Curve(size);
		xSq.ramp(0, 1, 1);
		Curve xy = curve.mult(xSq, size);
		float meanXY = xy.sum(0, size)/size;
		
		xSq.square(size);
		float meanXsq = xSq.sum(0, size)/size;

		float covarianceXY = meanXY - meanX*meanY;
		float varianceXsq = meanXsq - meanX*meanY;

		result.slope = covarianceXY/varianceXsq;
		result.yIntercept = meanY - result.slope*meanX;
		
		return result;
	}

	private float rmsError(Curve curve, Fit fit, int size) {
		float result;
		
		Curve fitLine = new Curve(size);
		fitLine.ramp(fit.yIntercept, 1, fit.slope);
		Curve diff = fitLine.subtract(curve, size);
		diff.square(size);
		
		result = diff.sum(0, size)/size;
		result = (float) Math.sqrt(result);
		return result;
	}

	private Knee findKnee(Curve curve, int min) {
		Knee result = new Knee();
		result.rmsError = Float.POSITIVE_INFINITY;
		float error;

		Fit fit = new Fit();
		for (int i = min-1; i < curve.length; i++) {
			fit = regression(curve, i);
			error = rmsError(curve, fit, curve.length);
			if (error < result.rmsError) {
				result.rmsError = error;
				result.fit = fit;
				result.prefix = i;
			}
		}
		return result;
	}

	public Results getResults() {
		return results;
	}


}


















