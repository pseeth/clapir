package com.seeth.clapir;
import android.util.Log;

public class Smooth {
	static final float ALPHA= 0.1f;
	public Smooth() {
		
	}

	public float[] smooth(float[] signal, int window) {
		int size = (int) Math.floor((double) (signal.length/window));
		float[] data = new float[size];
		
		data[0] = signal[0];
		for (int i = 1; i < size; i++) {
			data[i] = (1-ALPHA)*data[i-1] + ALPHA*(signal[i*window]);
		}
		return data;
	}

}
