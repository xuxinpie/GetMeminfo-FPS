package com.example.appinfo;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;


public class AppMemInfoService extends Service {
	
	private ViewGroup mLayout = null;
	private AppMemInfoFloatWindow memInfoWindow;
	private WindowManager mWindowManager;
    private WindowManager.LayoutParams layoutParams;
    private ActivityManager mActivityManager = null;
	
    
    private Runnable mInvalidateRunnable = new Runnable() {
        @Override
        public void run() {
            if(mLayout != null) {
            	String pidMemStr = getPidMemorySize();
                memInfoWindow.setPidMemString(pidMemStr);
                mLayout.invalidate();
                mLayout.post(this);
            }
        }
    };
    
	@Override
	public void onCreate() {
		super.onCreate();
		showMemInfoFloatWindow();
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) { 
        
        return START_STICKY;
    }
	
	private void showMemInfoFloatWindow() {
		mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		//获取ActivityManager服务的对象
        mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
		layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
		layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
				| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		layoutParams.format = PixelFormat.RGBA_8888;
		layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
		layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		layoutParams.x = this.getResources().getDisplayMetrics().widthPixels;
		layoutParams.y = 0;
		mLayout = new RelativeLayout(this);
		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		memInfoWindow = new AppMemInfoFloatWindow(this);
		//获得当前进程占用内存信息
        String pidMemStr = getPidMemorySize();
        memInfoWindow.setPidMemString(pidMemStr);
		mLayout.addView(memInfoWindow, rlp);
		mLayout.setOnTouchListener(new OnTouchListener() {
			private float mTouchX,mTouchY;
			private float mLastRawX,mLastRawY;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mTouchX=event.getX();
					mTouchY=event.getY();
					mLastRawX=event.getRawX();
					mLastRawY=event.getRawY();
					break;
				case MotionEvent.ACTION_MOVE:
					updateView(event);
					break;
				case MotionEvent.ACTION_UP:
					updateView(event);
					mTouchX=0;
					mTouchY=0;
					break;
				}
				
				return false;
			}
			
			private void updateView(MotionEvent event) {
				Rect rect=new Rect();
				mLayout.getWindowVisibleDisplayFrame(rect);
				float x=event.getRawX()-mTouchX;
				float y=event.getRawY()-mTouchY-rect.top;
				layoutParams.x=(int) x;
				layoutParams.y=(int) y;
				mWindowManager.updateViewLayout(mLayout, layoutParams);
			}
		});
		
		mLayout.post(mInvalidateRunnable);
		mWindowManager.addView(mLayout, layoutParams);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	    if(mLayout != null && mLayout.getParent() != null) {
	    	WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
	        windowManager.removeView(mLayout);
	        mLayout.removeAllViews();
	    }
	    memInfoWindow = null;
	}
	
	//获得当前进程所占内存大小
    private String getPidMemorySize() {
    	int pid = android.os.Process.myPid();
        int[] myMempid = new int[] { pid };
		// 此MemoryInfo位于android.os.Debug.MemoryInfo包中，用来统计进程的内存信息
		android.os.Debug.MemoryInfo[] memoryInfo = mActivityManager
				.getProcessMemoryInfo(myMempid);
		// 获取进程占内存用信息 kb单位
		int memSize = memoryInfo[0].dalvikPrivateDirty;
		return Integer.toString(memSize);
    }

}
