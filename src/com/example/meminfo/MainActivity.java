package com.example.meminfo;


import java.io.IOException;
import java.io.RandomAccessFile;

import com.example.appinfo.AppMemInfoService;
import com.example.fps.FpsService;
import com.qin.ammp.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static String TAG = "AM_MEMORYIPROCESS";
	
	private ActivityManager mActivityManager = null;
	
	private TextView tvAvailMem;
	private TextView tvTotalMem;
	private TextView tvPidMem;
	private Button btProcessInfo;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        tvAvailMem = (TextView)findViewById(R.id.tvAvailMemory);
        tvTotalMem = (TextView)findViewById(R.id.tvTotalMemory);
        tvPidMem = (TextView) findViewById(R.id.tvPidMemory);
        btProcessInfo =(Button)findViewById(R.id.btProcessInfo);
        //跳转到显示进程信息界面
        btProcessInfo.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this,BrowseProcessInfoActivity.class);
				startActivity(intent);
			}
		});               
        
        //获取ActivityManager服务的对象
        mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        
        //获得总的内存信息
//        String totalMemStr = getSystemTotalMemorySize();
        String totalMemStr = readTotalRam();
        Log.i(TAG, "The Total Memory Size is "+ totalMemStr); 
        //显示
        tvTotalMem.setText(totalMemStr);
        
        //获得可用内存信息
        String availMemStr = getSystemAvaialbeMemorySize();
        Log.i(TAG, "The Availabel Memory Size is " + availMemStr); 
        //显示
        tvAvailMem.setText(availMemStr);
       
        //获得当前进程占用内存信息
        String pidMemStr = getPidMemorySize();
        Log.i(TAG, "The Current Process Memory Size is " + pidMemStr + " KB"); 
        //显示
        tvPidMem.setText(pidMemStr + " KB");
        
        //显示Float Window
//        showFps(true);
        
        showMemInfo(true);
        
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
//    	showFps(false);
    	showMemInfo(false);
    }
    
    //显示系统当前的帧率
    private void showFps(boolean show) {
    	if (show) {
			Intent intent = new Intent(this, FpsService.class);
			startService(intent);
		}else {
			Intent intent = new Intent(this, FpsService.class);
			stopService(intent);
		}
	}
    
    //显示当前app所占内存
    private void showMemInfo(boolean show) {
    	if (show) {
			Intent intent = new Intent(this, AppMemInfoService.class);
			startService(intent);
		}else {
			Intent intent = new Intent(this, AppMemInfoService.class);
			stopService(intent);
		}
    }

	//获得系统可用的内存信息
    private String getSystemAvaialbeMemorySize(){
    	//获得MemoryInfo对象
        MemoryInfo memoryInfo = new MemoryInfo();
        //获得系统可用内存，保存在MemoryInfo对象上
        mActivityManager.getMemoryInfo(memoryInfo);
        long memSize = memoryInfo.availMem;
        
        //字符类型装换
        String availMemStr = formateFileSize(memSize);
        
        return availMemStr ;
    }
    
    //获得手机的内存信息
    private String getSystemTotalMemorySize() {
		MemoryInfo memoryInfo = new MemoryInfo();
		mActivityManager.getMemoryInfo(memoryInfo);
		long memSize = memoryInfo.totalMem;
		String totalMemStr = formateFileSize(memSize);
		return totalMemStr;
	}
    //获得手机的内存信息 针对低版本手机
	public synchronized String readTotalRam() {
        long tm = 0;
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
            String load = reader.readLine();
            String[] totrm = load.split(" kB");
            String[] trm = totrm[0].split(" ");
            tm = Integer.parseInt(trm[trm.length - 1]);
            tm = Math.round(tm * 1024); //将KB单位转为基础的B单位，然后用formateFileSize进行转换
            String totalMemString = formateFileSize(tm);
            return totalMemString;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
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
  
    //调用系统函数，字符串转换 long -String KB/MB
    private String formateFileSize(long size){
    	return Formatter.formatFileSize(MainActivity.this, size); 
    }
    
}