package com.example.appinfo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.widget.TextView;

public class AppMemInfoFloatWindow extends TextView {
	
	//设置时间间隔，每200ms更新一次内存信息
	public static final long PERIOD = (long) (1000000000L / 5);
//	private MemInfoUpdateListener mListener = null;
	private long mInterval = 0L;
	private long mLastTime = 0L;
	private String pidMemString = " ";
	private String curClassName = " ";
	private MemInfoUpdatelistener mListener = null;
	
//	public interface MemInfoUpdateListener {
//		
//	}
	
	public AppMemInfoFloatWindow(Context context) {
		super(context);
		setTextColor(Color.parseColor("#ff0000"));
		setText(" ");
		
	}
	
	public interface MemInfoUpdatelistener {
		public void onUpdateMemInfo(String curClassName, String pidMemString);
	}
	
	
	@Override
	public void draw(Canvas canvas) {
		if(mLastTime == 0) {
			mLastTime = System.nanoTime();
		}
		super.draw(canvas);
		
		long timeNow = System.nanoTime();
		mInterval += timeNow - mLastTime;
		mLastTime = timeNow;
		if(mInterval >= PERIOD) {
			updateMemInfoWindow();
			mInterval = 0L;
		}
		
	}
	
//	public void setListener(MemInfoUpdateListener listener)
//	{
//	    mListener = listener;
//	}
	
	private void updateMemInfoWindow() {
		if (mListener != null) {
			mListener.onUpdateMemInfo(curClassName, pidMemString);
		}
		setText("当前进程所占内存：" + pidMemString + " KB" + "\n" +
				"当前顶层Activity：" +"\n" + curClassName + ""	
				);
	}

	public void setPidMemString(String pidMemString) {
		this.pidMemString = pidMemString;
	}

	public void setCurClassName(String curClassName) {
		this.curClassName = curClassName; 
	}
	
	public void setListener(MemInfoUpdatelistener listener) {
		mListener = listener;
	}
	

}
