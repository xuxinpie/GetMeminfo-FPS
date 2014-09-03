package com.example.meminfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.qin.ammp.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

public class BrowseRunningAppActivity extends Activity {

	private static String TAG = "BrowseRunningAppActivity";

	private ListView listview = null;

	private List<RunningAppInfo> mlistAppInfo = null;
    private TextView tvInfo = null ;
    
	private PackageManager pm;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browse_app_list);

		listview = (ListView) findViewById(R.id.listviewApp);
		tvInfo = (TextView)findViewById(R.id.tvInfo) ;
		
		mlistAppInfo = new ArrayList<RunningAppInfo>();

		// 查询某一特定进程的所有应用程序
		Intent intent = getIntent();
		//是否查询某一特定pid的应用程序
		int pid = intent.getIntExtra("EXTRA_PROCESS_ID", -1);
		
		if ( pid != -1) {
			//某一进程里所有正在运行的应用程序
			mlistAppInfo =querySpecailPIDRunningAppInfo(intent, pid);
		}
		else{
			// 查询所有正在运行的应用程序信息： 包括他们所在的进程id和进程名
			tvInfo.setText("所有正在运行的应用程序有-------");
		    mlistAppInfo = queryAllRunningAppInfo(); 
		}
		BrowseRunningAppAdapter browseAppAdapter = new BrowseRunningAppAdapter(this, mlistAppInfo);
		listview.setAdapter(browseAppAdapter);
	}

	// 查询所有正在运行的应用程序信息： 包括他们所在的进程id和进程名
	// 这里直接获取了系统里安装的所有应用程序，然后根据报名pkgname过滤获取所有真正运行的应用程序
	private List<RunningAppInfo> queryAllRunningAppInfo() {
		pm = this.getPackageManager();
		// 查询所有已经安装的应用程序
		List<ApplicationInfo> listAppcations = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		Collections.sort(listAppcations,new ApplicationInfo.DisplayNameComparator(pm));// 排序

		// 保存所有正在运行的包名 以及它所在的进程信息
		Map<String, ActivityManager.RunningAppProcessInfo> pgkProcessAppMap = new HashMap<String, ActivityManager.RunningAppProcessInfo>();

		ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		// 通过调用ActivityManager的getRunningAppProcesses()方法获得系统里所有正在运行的进程
		List<ActivityManager.RunningAppProcessInfo> appProcessList = mActivityManager
				.getRunningAppProcesses();

		for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
			int pid = appProcess.pid; // pid
			String processName = appProcess.processName; // 进程名
			Log.i(TAG, "processName: " + processName + "  pid: " + pid);

			String[] pkgNameList = appProcess.pkgList; // 获得运行在该进程里的所有应用程序包

			// 输出所有应用程序的包名
			for (int i = 0; i < pkgNameList.length; i++) {
				String pkgName = pkgNameList[i];
				Log.i(TAG, "packageName " + pkgName + " at index " + i+ " in process " + pid);
				// 加入至map对象里
				pgkProcessAppMap.put(pkgName, appProcess);
			}
		}
		// 保存所有正在运行的应用程序信息
		List<RunningAppInfo> runningAppInfos = new ArrayList<RunningAppInfo>(); // 保存过滤查到的AppInfo

		for (ApplicationInfo app : listAppcations) {
			// 如果该包名存在 则构造一个RunningAppInfo对象
			if (pgkProcessAppMap.containsKey(app.packageName)) {
				// 获得该packageName的 pid 和 processName
				int pid = pgkProcessAppMap.get(app.packageName).pid;
				String processName = pgkProcessAppMap.get(app.packageName).processName;
				runningAppInfos.add(getAppInfo(app, pid, processName));
			}
		}

		return runningAppInfos;

	}
	// 某一特定经常里所有正在运行的应用程序
	private List<RunningAppInfo> querySpecailPIDRunningAppInfo(Intent intent , int pid) {


		String[] pkgNameList = intent.getStringArrayExtra("EXTRA_PKGNAMELIST");
		String processName = intent.getStringExtra("EXTRA_PROCESS_NAME");
		
		//update ui
		tvInfo.setText("进程id为"+pid +" 运行的应用程序共有  :  "+pkgNameList.length);
				
		pm = this.getPackageManager();
	
		// 保存所有正在运行的应用程序信息
		List<RunningAppInfo> runningAppInfos = new ArrayList<RunningAppInfo>(); // 保存过滤查到的AppInfo

		for(int i = 0 ; i<pkgNameList.length ;i++){
		   //根据包名查询特定的ApplicationInfo对象
		   ApplicationInfo appInfo;
		  try {
			appInfo = pm.getApplicationInfo(pkgNameList[i], 0);
            runningAppInfos.add(getAppInfo(appInfo, pid, processName));
		  }
		  catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		  }  // 0代表没有任何标记;
		}
		return runningAppInfos ;
	}
	
	
	// 构造一个RunningAppInfo对象 ，并赋值
	private RunningAppInfo getAppInfo(ApplicationInfo app, int pid, String processName) {
		RunningAppInfo appInfo = new RunningAppInfo();
		appInfo.setAppLabel((String) app.loadLabel(pm));
		appInfo.setAppIcon(app.loadIcon(pm));
		appInfo.setPkgName(app.packageName);

		appInfo.setPid(pid);
		appInfo.setProcessName(processName);

		return appInfo;
	}
}

