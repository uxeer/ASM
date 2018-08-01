package com.asm.gongbj.gradle;
import android.content.*;
import android.app.*;
import com.asm.gongbj.tools.*;
import com.asm.gongbj.gradle.sync.*;
import java.util.*;
import java.io.*;

public class GradleBuild
{
	private Activity ac;
	private int resultValue;
	private ProgressListener progL;
	private ErrorListener errorL;
	public GradleBuild(Activity con){
		ac = con;
	}
	public void setProgressListener(ProgressListener prog){
		progL = prog;
	}
	public void setErrorListener(ErrorListener error){
		errorL = error;
	}
	public void run(String androidGradlePath, String mainGradlePath){
		//Ready
		if(progL==null){
			errorL.onError(new ProgressFail("Cannot start building because ProgressListener is null",androidGradlePath,"Gradle"));
			return;
		}
		if(errorL==null){
			errorL.onError(new ProgressFail("Cannot start building because ErorrListener is null",androidGradlePath,"Gradle"));
			return;
		}
		resultValue = 1;
		//Prepare
		String androidJar = Aapt.getAndroidJarPath();
		if(androidJar==null){
			progL.onProgressChange("Preparing android.jar...");
			try
			{
				androidJar = Aapt.requestAndroidJar(ac);
			}
			catch (Exception e)
			{
				errorL.onError(new ProgressFail("Error while preparing android.jar","","Gradle"));
				resultValue=0;
			}

		}if(resultValue==0)return;
		
		//Setting for Sync
		Syncer s = new Syncer(ac);
		
		s.setProgressListener(new Syncer.ProgressListener(){
				@Override
				public void onProgressStart(){
					progL.onProgressStart();
				}
				@Override
				public void onProgressChange(String progressName){
					progL.onProgressChange(progressName);
				}
				@Override
				public void onprogressFinish(){
				}
			});
		s.setErrorListener(new Syncer.ErrorListener(){
				@Override
				public boolean onError(ProgressFail progressFail){
					resultValue=0;
					errorL.onError(progressFail);
					return true;
				}
			});
		
		//Start Sync
		SyncData syncD = null;
		try{
			syncD = s.sync(androidGradlePath,mainGradlePath);
		}catch(Exception e){
			errorL.onError(new ProgressFail("Error while syncing...",androidGradlePath,"sync"));
			resultValue=0;
		}
		if(resultValue==0||syncD==null)return;
		
		//Start Ecj
		Ecj ecj = new Ecj(androidJar);
		progL.onProgressChange("Starting compiler...");
		ArrayList<String> projects = new ArrayList<String>();
		for(String str : syncD.getSyncedProjectPath()){
			String p1 = str + "/src/main/java";
			String p2 = str + "/build/gen";
			if(new File(p1).exists())projects.add(p1);
			if(new File(p2).exists())projects.add(p2);
		}
		progL.onProgressChange("Java compiling...");
		ecj.compile(mainGradlePath+"/src/main/java",(String[])projects.toArray(),mainGradlePath+"/build/bin/class",syncD.getScanedJar());
		//Start aapt
		Aapt aapt = new Aapt(androidJar);
		
		
		
	}
	public static interface ProgressListener{

		public void onProgressStart();
		public void onProgressChange(String progressName);
		public void onprogressFinish();
	}
	public static interface ErrorListener{
		public boolean onError(ProgressFail progressFail);
	}
}