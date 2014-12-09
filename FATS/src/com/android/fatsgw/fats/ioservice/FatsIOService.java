package com.android.fatsgw.fats.ioservice;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.android.fatsgw.fats.MainActivity;
import com.android.fatsgw.fats.database.FatsDatabase;
import com.android.fatsgw.fats.dataholders.DataPack;
import com.android.fatsgw.fats.utils.Converter;

public class FatsIOService extends IntentService
{
	public static final String ACTION_REGISTER = "com.android.fatsgw.fats.ioservice.REGISTER";
	public static final String ACTION_UPDATE = "com.android.fatsgw.fats.ioservice.UPDATE";
	public static final String ACTION_UNREGISTER = "com.android.fatsgw.fats.ioservice.UNREGISTER";
	public static final String ACTION_RETRIEVE = "com.android.fatsgw.fats.ioservice.RETRIEVE";

	public static final String EXTRA_STATUS = "com.android.fatsgw.fats.ioservice.OP_STATUS";
	public static final String EXTRA_OP_CODE = "com.android.fatsgw.fats.ioservice.OP_CODE";

	public static final String EXTRA_APP_PACKAGE_NAME = "com.android.fatsgw.fats.ioservice.APP_PACKAGE"; //fully qualified path of app
	public static final String EXTRA_APP_REPLY_ADDRESS = "com.android.fatsgw.fats.ioservice.APP_REPLY_ADDRESS"; //fully qualified path of receiving class
	public static final String EXTRA_APP_NAME = "com.android.fatsgw.fats.ioservice.APP_NAME";
	public static final String EXTRA_APP_DATA = "com.android.fatsgw.fats.ioservice.APP_DATA";
	public static final String EXTRA_PASSERBY_ID = "com.android.fatsgw.fats.ioservice.PASSERBY_ID";
	public static final String EXTRA_PASSERBY_NAME = "com.android.fatsgw.fats.ioservice.PASSERBY_NAME";
	public static final String EXTRA_PASSERBY_DATA = "com.android.fatsgw.fats.ioservice.PASSERBY_DATA";
	public static final String EXTRA_PASSERBY_TIME = "com.android.fatsgw.fats.ioservice.PASSERBY_TIME";

	private FatsDatabase _db;

	public FatsIOService()
	{
		super("FatsIOService");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
		if (_db != null)
			_db.close();
		
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		if (_db == null)
			_db = new FatsDatabase();

		String action = intent.getAction();
		if (action == null)
			return;

		if (action.equals(ACTION_REGISTER))
			register(intent);
		else if (action.equals(ACTION_UNREGISTER))
			unregister(intent);
		else if (action.equals(ACTION_UPDATE))
			update(intent);
		else if (action.equals(ACTION_RETRIEVE))
			retrieve(intent);
		else
			Log.d(MainActivity.DEBUG_TAG, "FatsIOService: Unknown action received");
	}


	private void register(Intent intent)
	{
		//check for all necessary data
		String packageName = intent.getStringExtra(EXTRA_APP_PACKAGE_NAME);
		if (packageName == null)
			return;

		String replyAddress = intent.getStringExtra(EXTRA_APP_REPLY_ADDRESS);
		if (replyAddress == null)
			return;

		String appName = intent.getStringExtra(EXTRA_APP_NAME);
		if (appName == null)
			return;

		String appData = intent.getStringExtra(EXTRA_APP_DATA);
		if (appData == null)
			return;

		//convert app package name into app id (app id used internally only)
		long appId = Converter.packageNameToId(packageName);

		//register app into database and check status
		boolean registerStatus = _db.insertRegisteredApp(appId, packageName, appName, appData);

		//send register status back to app
		Intent i = new Intent(replyAddress);
		i.putExtra(EXTRA_OP_CODE, intent.getAction());
		i.putExtra(EXTRA_STATUS, registerStatus);
		sendBroadcast(i);
		
		System.out.println("Registration received");
	}


	private void unregister(Intent intent)
	{
		//check for all necessary data
		String packageName = intent.getStringExtra(EXTRA_APP_PACKAGE_NAME);
		if (packageName == null)
			return;

		String replyAddress = intent.getStringExtra(EXTRA_APP_REPLY_ADDRESS);
		if (replyAddress == null)
			return;

		//convert app package name into app id
		long appId = Converter.packageNameToId(packageName);

		//unregister app and check status
		boolean unregisterStatus = _db.clearRegisteredApp(appId);

		//send unregister status back to app
		Intent i = new Intent(replyAddress);
		i.putExtra(EXTRA_OP_CODE, intent.getAction());
		i.putExtra(EXTRA_STATUS, unregisterStatus);
		sendBroadcast(i);
		
		System.out.println("Unregistration received");
	}


	private void update(Intent intent)
	{
		//check for all necessary data
		String packageName = intent.getStringExtra(EXTRA_APP_PACKAGE_NAME);
		if (packageName == null)
			return;

		String replyAddress = intent.getStringExtra(EXTRA_APP_REPLY_ADDRESS);
		if (replyAddress == null)
			return;

		String appData = intent.getStringExtra(EXTRA_APP_DATA);
		if (appData == null)
			return;

		//convert app package name into app id
		long appId = Converter.packageNameToId(packageName);

		//update in database
		boolean updateStatus = _db.updateRegisteredApp(appId, appData);

		//send update status back to app
		Intent i = new Intent(replyAddress);
		i.putExtra(EXTRA_OP_CODE, intent.getAction());
		i.putExtra(EXTRA_STATUS, updateStatus);
		sendBroadcast(i);
	}


	private void retrieve(Intent intent)
	{
		//check for necesssary data
		String packageName = intent.getStringExtra(EXTRA_APP_PACKAGE_NAME);
		if (packageName == null)
			return;

		String replyAddress = intent.getStringExtra(EXTRA_APP_REPLY_ADDRESS);
		if (replyAddress == null)
			return;

		//convert app package name into app id
		long appId = Converter.packageNameToId(packageName);

		//retrieve all data
		DataPack[] dataPacks = _db.getAppReceviedData(appId);
		Intent replyIntent = new Intent(replyAddress);

		//delete retrieved data from database ( 1 time retrieval only )
		_db.clearAppReceivedData(appId);

		//convert from data packs into multiple string array
		int amountOfPacks = dataPacks.length;
		String[] passerbyIds = new String[amountOfPacks];
		String[] passerbyNames = new String[amountOfPacks];
		String[] passerbyData = new String[amountOfPacks];
		String[] passerbyTime = new String[amountOfPacks];

		for (int i = 0; i < amountOfPacks; i++)
		{
			passerbyIds[i] = dataPacks[i].getSrcMac();
			passerbyNames[i] = dataPacks[i].getSrcDeviceName();
			passerbyData[i] = dataPacks[i].getAppData();
			passerbyTime[i] = dataPacks[i].getTimeStamp();
		}

		replyIntent.putExtra(EXTRA_STATUS, true);
		replyIntent.putExtra(EXTRA_PASSERBY_ID, passerbyIds);
		replyIntent.putExtra(EXTRA_PASSERBY_NAME, passerbyNames);
		replyIntent.putExtra(EXTRA_PASSERBY_DATA, passerbyData);
		replyIntent.putExtra(EXTRA_PASSERBY_TIME, passerbyTime);


		replyIntent.putExtra(EXTRA_OP_CODE, intent.getAction());
		sendBroadcast(replyIntent);
		
		System.out.println("Retrieve received");
	}
}
