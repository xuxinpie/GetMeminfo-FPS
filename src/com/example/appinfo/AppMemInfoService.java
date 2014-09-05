package com.example.appinfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;


public class AppMemInfoService extends Service implements OnClickListener, AppMemInfoFloatWindow.MemInfoUpdatelistener {
	
	private final static String TAG = AppMemInfoService.class.getSimpleName();
	private final DateFormat mFileNameFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
	private final static String SD_CARD_FILE_DIR = "MemInfo";
	private ViewGroup mLayout = null;
	private AppMemInfoFloatWindow memInfoWindow;
	private WindowManager mWindowManager;
    private WindowManager.LayoutParams layoutParams;
    private ActivityManager mActivityManager = null;
    private BlockingQueue<ArrayList<MemInfoRecord>> mRecordQueue = new LinkedBlockingQueue<ArrayList<MemInfoRecord>>();
    private ArrayList<MemInfoRecord> mCurrentRecords = null;
    private Worker mWorker = null;
    private Handler mHandler = new Handler();
    
    
    private Button mStartBtn = null;
    private Button mStopBtn = null;
    
    private final static int MEMINFO_VIEW_ID = 1;
    private final static int START_BTN_ID = 2;
    private final static int STOP_BTN_ID = 3;
	
    
    private Runnable mInvalidateRunnable = new Runnable() {
        @Override
        public void run() {
            if(mLayout != null) {
            	String pidMemStr = getPidMemorySize();
            	String curClassName = getCurClassName();
                memInfoWindow.setPidMemString(pidMemStr);
                memInfoWindow.setCurClassName(curClassName);
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
		memInfoWindow.setId(MEMINFO_VIEW_ID);
		memInfoWindow.setListener(this);
		//获得当前进程占用内存信息
        String pidMemStr = getPidMemorySize();
        String curClassName = getCurClassName();
        memInfoWindow.setPidMemString(pidMemStr);
        memInfoWindow.setCurClassName(curClassName);
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
					float dx=event.getRawX()-mLastRawX;
					float dy=event.getRawY()-mLastRawY;
					if(Math.abs(dx)<=5 && Math.abs(dy)<=5) {
						onClick(mLayout);
					}
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
		
		rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.BELOW, MEMINFO_VIEW_ID);
        mStartBtn = new Button(this);
        mStartBtn.setId(START_BTN_ID);
        mStartBtn.setText("start");
        mStartBtn.setOnClickListener(this);
        mLayout.addView(mStartBtn, rlp);
        
        rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.RIGHT_OF, START_BTN_ID);
        rlp.addRule(RelativeLayout.BELOW, MEMINFO_VIEW_ID);
        mStopBtn = new Button(this);
        mStopBtn.setId(STOP_BTN_ID);
        mStopBtn.setText("stop");
        mStopBtn.setOnClickListener(this);
        mLayout.addView(mStopBtn, rlp);
		
		mLayout.post(mInvalidateRunnable);
		mWindowManager.addView(mLayout, layoutParams);
		
		mStartBtn.setEnabled(true);
        mStopBtn.setEnabled(false);
        
        mWorker = new Worker();
        mWorker.start();
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
	    mStartBtn = null;
	    mStopBtn = null;
	    if (mWorker != null) {
			mWorker.quit();
			mWorker = null;
		}
	}
	
	/**
     * every record
     * 
     * @author xinux
     * @version 1.0
     * @date 2014-09-04
     */
    private static class MemInfoRecord {
    	
    	public String curClassName;
        public String pidMemStr;
        
    	public MemInfoRecord(String curClassName, String pidMem) {
    		this.curClassName = curClassName;
    		this.pidMemStr = pidMem;
        }
        
    }
	
	/**
     * record worker thread
     * 
     * @author xinux
     * @version 1.0
     * @date 2014-09-04
     */
    private class Worker extends Thread
    {
        private boolean mQuit = false;
        
        public void quit()
        {
            mQuit = true;
        }
        
        @Override
        public void run()
        {
            super.run();
            while(!mQuit)
            {
                ArrayList<MemInfoRecord> records = null;
                try {
                    records = mRecordQueue.take();
                }
                catch (InterruptedException e) {
                    
                    continue;
                }
                
                // write file
                StringBuilder sb = new StringBuilder();
                final int size = records.size();
                for(int i = 0;i < size; ++i) {
                    MemInfoRecord record = records.get(i);
                    sb.append("TopActivity:  ");
                    sb.append(record.curClassName);
                    sb.append("\t");
                    sb.append(record.pidMemStr);
                    sb.append(" Kb");
                    sb.append("\n");
                }
                final File file = createWritableFile();
                if(file == null) {
                    continue;
                }
                FileWriter fw = null;
                try {
                    fw = new FileWriter(file);
                    fw.write(sb.toString());
                    fw.flush();
                    
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "has write " + size + " MemInfo records into  :" + file.getAbsolutePath(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if(fw != null) {
                        try {
                            fw.close();
                        }
                        catch (IOException e) {
                            
                            e.printStackTrace();
                        }
                    }
                }
                records.clear();
            }
        }
    }
    
    /**
     * create writeable file in sd card
     * @return null if create fails
     */
    private File createWritableFile() {
        File memInfoDir = new File(Environment.getExternalStorageDirectory(), SD_CARD_FILE_DIR);
        boolean exists = memInfoDir.exists();
        if (!exists) {
            try {
                new File(memInfoDir, ".nomedia").createNewFile();
            } catch (IOException e) {
                Log.w(TAG, "Can't create \".nomedia\" file in application external cache directory", e);
            } if (!memInfoDir.mkdirs()) {
                Log.w(TAG, "Unable to create external cache directory");
                return null;
            }
        }
        
        File file = new File(memInfoDir, mFileNameFormat.format(new Date(System.currentTimeMillis())) + ".memInfolog");
        return file;
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
    
    //获取当前进程最顶层Activity的ClassName
    private String getCurClassName() {
    	ComponentName cn = mActivityManager.getRunningTasks(1).get(0).topActivity;
    	return cn.getClassName();
    }

	@Override
	public void onClick(View v) {
		final int id = v.getId();
        if(id == START_BTN_ID) {
            // start record
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                mStartBtn.setEnabled(false);
                mStopBtn.setEnabled(true);
                mCurrentRecords = new ArrayList<AppMemInfoService.MemInfoRecord>(3000);
            }
            else {
                Toast.makeText(getApplicationContext(), "sd card is unavailable！", Toast.LENGTH_LONG).show();
            }
        }
        else if(id == STOP_BTN_ID) {
            // stop record and save to file
            mStartBtn.setEnabled(true);
            mStopBtn.setEnabled(false);
            
            mRecordQueue.add(mCurrentRecords);
            mCurrentRecords = null;
        }
	}

	@Override
	public void onUpdateMemInfo(String curClassName, String pidMemString) {
		if(mStartBtn.isEnabled()) {
			return;
		}
		mCurrentRecords.add(new MemInfoRecord(curClassName, pidMemString));
	}
	


}
