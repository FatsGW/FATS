package com.android.fatsgw.namecardfatsdemo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class FindNearbyActivity extends Activity
{
	//For FATS app
	public static final String FATS_APP_PACKAGE = "com.android.fatsgw.fats";
	public static final String FATS_APP_RECEIVER = "com.android.fatsgw.fats.ioservice.FatsIOService";
	public static final ComponentName FATS_APP = new ComponentName(FATS_APP_PACKAGE, FATS_APP_RECEIVER);

	public static final String ACTION_REGISTER = "com.android.fatsgw.fats.ioservice.REGISTER";
	public static final String ACTION_UPDATE = "com.android.fatsgw.fats.ioservice.UPDATE";
	public static final String ACTION_UNREGISTER = "com.android.fatsgw.fats.ioservice.UNREGISTER";
	public static final String ACTION_RETRIEVE = "com.android.fatsgw.fats.ioservice.RETRIEVE";

	public static final String ACTION_REPLY_ADDRESS = "com.example.apptotestfats.REPLY_ADDRESS";

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
	
	
	//For this APP
	public static final String APP_NAME = "FATS Demo";
	public static final String PACKAGE_NAME = "com.android.fatsgw.namecardfatsdemo";
	public static final String EXTRA_NAME = "com.android.fatsgw.namecardfatsdemo.NAME";
	public static final String EXTRA_MESSAGE = "com.android.fatsgw.namecardfatsdemo.MESSAGE";
	public static final String EXTRA_COLOUR = "com.android.fatsgw.namecardfatsdemo.COLOUR";
	public static final String EXTRA_ICON = "com.android.fatsgw.namecardfatsdemo.ICON";


	private ProfileGridAdapter adapter;
	private ArrayList<Profile> profiles;
	private Profile myProfile;
	private HashMap<String, Integer> hashmap;
	
	private Timer timer;
	
	private BroadcastReceiver _broadcastReceiver;
	private IntentFilter _intentFilter;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_findnearby);

		Intent intent = getIntent();
		String name = intent.getStringExtra(EXTRA_NAME);
		if (name.length() == 0)
			name = " ";
		
		String message = intent.getStringExtra(EXTRA_MESSAGE);
		if (message.length() == 0)
			message = " ";
		
		int colourId = intent.getIntExtra(EXTRA_COLOUR, 0);
		int iconId = intent.getIntExtra(EXTRA_ICON, 0);
		myProfile = new Profile(name, message, colourId, iconId);
		
		
		hashmap = new HashMap<String, Integer>();
		profiles = new ArrayList<Profile>();
		profiles.add(new Profile("You", message, colourId, iconId));

		adapter = new ProfileGridAdapter(this, profiles);
		GridView gridView = (GridView)findViewById(R.id.gridView);
		gridView.setAdapter(adapter);

		final Context ctx = this;
		gridView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id)
			{
				Profile profile = profiles.get(position);

				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
				alertDialogBuilder.setTitle("User: " + profile.name)
				.setMessage(profile.message)
				.setNeutralButton("Ok", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				})
				.create()
				.show();
			}

		});
	}
	
	protected void onStart()
	{
		super.onStart();
		registerToFats();
		System.out.println("Started");
	}
	
	protected void onResume()
	{
		super.onResume();
		registerBroadcastReceiver();
		
		//start timer to keep retrieving from FATS every second
		startTimer();
	}
	
	protected void onPause()
	{
		super.onPause();
		unregisterBroadcastReceiver();
		stopTimer();
	}
	
	protected void onStop()
	{
		super.onStop();
		unregister();
		System.out.println("Stopped");
	}


	private void registerToFats()
	{
		StringBuilder data = new StringBuilder();
		char colourId = (char) myProfile.colourId;
		System.out.println("colourid:" + colourId);
		char iconId = (char) myProfile.iconId;
		data.append(colourId);
		data.append(iconId);
		data.append(myProfile.name);
		data.append("~@");
		data.append(myProfile.message);
		
		System.out.println(data);
		register(data.toString());
	}
	
	
	private void dataReceiver(String id, String data)
	{
		//check if data already exists
		if (hashmap.containsKey(id))
			return;
		
		hashmap.put(id, 1);
		char colourId = data.charAt(0);
		char iconId = data.charAt(1);
		
		String []nameAndMessage = data.substring(2).split("~@");
		
		Profile newProfile = new Profile(nameAndMessage[0], nameAndMessage[1], (int)colourId, (int)iconId);
		profiles.add(newProfile);
		
		
		adapter.notifyDataSetChanged();
	}

	private void startTimer()
	{
		timer = new Timer();
		
		//initialize 
		TimerTask retrieveTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				retrieve();
			}
		};

		//start a delayed data sending
		timer.schedule(retrieveTimerTask, 1000, 1000);
	}
	
	private void stopTimer()
	{
		timer.cancel();
		timer.purge();
	}

	
	private void registerBroadcastReceiver()
	{
		if (_broadcastReceiver == null)
		{
			_broadcastReceiver = new BroadcastReceiver()
			{
				@Override
				public void onReceive(Context ctx, Intent intent)
				{
					String action = intent.getAction();
					if (action == null)
						return;

					if (action.equals(ACTION_REPLY_ADDRESS))
					{
						String opCode = intent.getStringExtra(EXTRA_OP_CODE);
						if (opCode.equals(ACTION_REGISTER))
						{
							//get reply status
							if (intent.getBooleanExtra(EXTRA_STATUS, false))
								System.out.println("Register succeeded");
							else
								System.out.println("Register failed");
						}
						else if (opCode.equals(ACTION_UNREGISTER))
						{
							//get reply status
							if (intent.getBooleanExtra(EXTRA_STATUS, false))
								System.out.println("Unregister succeeded");
							else
								System.out.println("Unregister failed");
						}
						else if (opCode.equals(ACTION_UPDATE))
						{
							//get reply status
							if (intent.getBooleanExtra(EXTRA_STATUS, false))
								System.out.println("Update succeeded");
							else
								System.out.println("Update failed");
						}
						else if (opCode.equals(ACTION_RETRIEVE))
						{
							//get reply status
							if (intent.getBooleanExtra(EXTRA_STATUS, false))
							{
								StringBuilder output = new StringBuilder();
								
								String[] pIDs = intent.getStringArrayExtra(EXTRA_PASSERBY_ID);
								String[] pNames = intent.getStringArrayExtra(EXTRA_PASSERBY_NAME);
								String[] pDatas = intent.getStringArrayExtra(EXTRA_PASSERBY_DATA);
								String[] pTimes = intent.getStringArrayExtra(EXTRA_PASSERBY_TIME);
							
								for (int i = 0; i < pIDs.length; i++)
								{
									dataReceiver(pIDs[i], pDatas[i]);
									output.append("ID:" + pIDs[i] + "\n");
									output.append("Name:" + pNames[i] + "\n");
									output.append("Data:" + pDatas[i] + " \n");
									output.append("Time:" + pTimes[i] + "\n\n");
								}
								
								if (pIDs.length == 0)
									System.out.println("Retrieve succeded: no passerbys");
								else
									System.out.println("Retrieve succeeded \n" + output.toString());
							}
							else
								System.out.println("Retrieve failed");
						}
					}
				}	
			};
		}

		if (_intentFilter == null)
		{
			_intentFilter = new IntentFilter();
			_intentFilter.addAction(ACTION_REPLY_ADDRESS);
		}

		this.registerReceiver(_broadcastReceiver, _intentFilter);
		Log.d("test", "Registered intent");
	}

	private void unregisterBroadcastReceiver()
	{
		this.unregisterReceiver(_broadcastReceiver);
		Log.d("test", "Unregistered intent");
	}
	
	private void register(String data)
	{
		//target to send to
		Intent i = new Intent(ACTION_REGISTER);
		i.setComponent(FATS_APP);
		i.putExtra(EXTRA_APP_REPLY_ADDRESS, ACTION_REPLY_ADDRESS);

		i.putExtra(EXTRA_APP_NAME, APP_NAME);
		i.putExtra(EXTRA_APP_PACKAGE_NAME, PACKAGE_NAME);
		i.putExtra(EXTRA_APP_DATA, data);
		

		startService(i);
	}

	private void unregister()
	{
		Intent i = new Intent(ACTION_UNREGISTER);
		i.setComponent(FATS_APP);
		i.putExtra(EXTRA_APP_REPLY_ADDRESS, ACTION_REPLY_ADDRESS);

		i.putExtra(EXTRA_APP_PACKAGE_NAME, PACKAGE_NAME);
	
		startService(i);
	}

	private void update(String data)
	{
		Intent i = new Intent(ACTION_UPDATE);
		i.setComponent(FATS_APP);
		i.putExtra(EXTRA_APP_REPLY_ADDRESS, ACTION_REPLY_ADDRESS);
		
		i.putExtra(EXTRA_APP_PACKAGE_NAME, PACKAGE_NAME);
		i.putExtra(EXTRA_APP_DATA, data);
		
		startService(i);
	}

	private void retrieve()
	{
		Intent i = new Intent(ACTION_RETRIEVE);
		i.setComponent(FATS_APP);
		i.putExtra(EXTRA_APP_REPLY_ADDRESS, ACTION_REPLY_ADDRESS);
		
		i.putExtra(EXTRA_APP_PACKAGE_NAME, PACKAGE_NAME);
		
		startService(i);
	}
}
