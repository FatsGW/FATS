package com.android.fatsgw.fats;

import android.app.Application;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;

public class FatsApp extends Application
{
	private static FatsApp _INSTANCE;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		_INSTANCE = this;
	}
	
	
	/** For getting the global context of the app. This context will only be cleaned up when all activities and services stop.
	 * @return
	 */
	public static Context getContext()
	{
		return _INSTANCE.getApplicationContext();
	}
	
	
	/** Gets the LocalBroadcastManager bound to this App's context
	 * @return
	 */
	public static LocalBroadcastManager getLocalBroadcastManager()
	{
		return LocalBroadcastManager.getInstance(getContext());
	}
	
	
	/** Gets a String of the current time formatted accordingly
	 * @return String of current time formatted in Y/m/d H:M:S
	 */
	public static String getCurrentTime()
	{
		Time now = new Time();
		now.setToNow();
		return now.format("%Y/%m/%d %H:%M:%S");
	}
}
