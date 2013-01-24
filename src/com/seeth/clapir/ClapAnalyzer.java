package com.seeth.clapir;

import root.gast.audio.record.AudioClipListener;

public interface ClapAnalyzer extends AudioClipListener {
	public double getBaseline();
	public void dispHistory();
	public boolean isListeningBase();
	public short[] getData();
	public boolean clap_heard();
	public boolean done();
	public StatusUpdate getStatus();
	public double getBaselineV();
	public Results getResults();
	public void setCancel(boolean cancel);
	public boolean getCancel();
	public void process();
}
