package com.example.meminfo;

import java.util.ArrayList;
import java.util.List;

import com.qin.ammp.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class BrowseProcessInfoActivity extends Activity  implements OnItemClickListener{

	private static String TAG = "ProcessInfo";
	private static final int KILL_PORCESS = 1;
	private static final int SEARCH_RUNNING_APP = 2;

	private ActivityManager mActivityManager = null;
	// ProcessInfo Model类 用来保存所有进程信息
	private List<ProcessInfo> processInfoList = null;

	private ListView listviewProcess;
    private TextView tvTotalProcessNo ; 
	
    private String [] dialogItems  = new String[] {"杀死该进程","查看运行在该进程的应用程序"} ;
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.browse_process_list);

		listviewProcess = (ListView) findViewById(R.id.listviewProcess);
		listviewProcess.setOnItemClickListener(this);
		
		tvTotalProcessNo =(TextView)findViewById(R.id.tvTotalProcessNo);
		
		// 获得ActivityManager服务的对象
		mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		// 获得系统进程信息
		getRunningAppProcessInfo();
		// 为ListView构建适配器对象
		BrowseProcessInfoAdapter mprocessInfoAdapter = new BrowseProcessInfoAdapter(this, processInfoList);
		listviewProcess.setAdapter(mprocessInfoAdapter);
	
		tvTotalProcessNo.setText("当前系统进程共有："+processInfoList.size());
	}
    //杀死该进程，并且刷新
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1,  final int position, long arg3) {
		// TODO Auto-generated method stub
	    new AlertDialog.Builder(this).setItems(dialogItems, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				//杀死进程
				if(which == 0) {
				   //杀死该进程，释放进程占用的空间
				   mActivityManager.killBackgroundProcesses(processInfoList.get(position).getProcessName());
		           //刷新界面
				   getRunningAppProcessInfo() ;
				   BrowseProcessInfoAdapter mprocessInfoAdapter = new BrowseProcessInfoAdapter(
				   BrowseProcessInfoActivity.this, processInfoList);
				   listviewProcess.setAdapter(mprocessInfoAdapter);
				   tvTotalProcessNo.setText("当前系统进程共有："+processInfoList.size());
				}
				//查看运行在该进程的应用程序
				else if(which ==1){   
					ProcessInfo processInfo = processInfoList.get(position);
					
					Intent intent = new Intent();
				    intent.putExtra("EXTRA_PKGNAMELIST", processInfo.pkgnameList);
				    intent.putExtra("EXTRA_PROCESS_ID", processInfo.getPid());
				    intent.putExtra("EXTRA_PROCESS_NAME", processInfo.getProcessName());
				    intent.setClass(BrowseProcessInfoActivity.this, BrowseRunningAppActivity.class);
				    startActivity(intent);
				}
		   }
	    }).create().show();
	}
	// 获得系统进程信息
	private void getRunningAppProcessInfo() {
		// ProcessInfo Model类   用来保存所有进程信息
	    processInfoList = new ArrayList<ProcessInfo>();

		// 通过调用ActivityManager的getRunningAppProcesses()方法获得系统里所有正在运行的进程
		List<ActivityManager.RunningAppProcessInfo> appProcessList = mActivityManager
				.getRunningAppProcesses();

		for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessList) {
			// 进程ID号
			int pid = appProcessInfo.pid;
			// 用户ID 类似于Linux的权限不同，ID也就不同 比如 root等
			int uid = appProcessInfo.uid;
			// 进程名，默认是包名或者由属性android：process=""指定
			String processName = appProcessInfo.processName;
			// 获得该进程占用的内存
			int[] myMempid = new int[] { pid };
			// 此MemoryInfo位于android.os.Debug.MemoryInfo包中，用来统计进程的内存信息
			Debug.MemoryInfo[] memoryInfo = mActivityManager
					.getProcessMemoryInfo(myMempid);
			// 获取进程占内存用信息 kb单位
			int memSize = memoryInfo[0].dalvikPrivateDirty;

			Log.i(TAG, "processName: " + processName + "  pid: " + pid
					+ " uid:" + uid + " memorySize is -->" + memSize + "kb");
			// 构造一个ProcessInfo对象
			ProcessInfo processInfo = new ProcessInfo();
			processInfo.setPid(pid);
			processInfo.setUid(uid);
			processInfo.setMemSize(memSize);
			processInfo.setPocessName(processName);
			//保存所有运行在该应用程序的包名
			processInfo.pkgnameList = appProcessInfo.pkgList ;
			processInfoList.add(processInfo);

			// 获得每个进程里运行的应用程序(包),即每个应用程序的包名
			String[] packageList = appProcessInfo.pkgList;
			
			Log.i(TAG, "process id is " + pid + "has " + packageList.length);
			for (String pkg : packageList) {
				Log.i(TAG, "packageName " + pkg + " in process id is -->"+ pid);
			}
		}
	}
}
