package com.android.fatsgw.fats.exchangers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import com.android.fatsgw.fats.DataExchangeService;
import com.android.fatsgw.fats.FatsApp;
import com.android.fatsgw.fats.MainActivity;
import com.android.fatsgw.fats.database.FatsDatabase;
import com.android.fatsgw.fats.dataholders.DataPack;
import com.android.fatsgw.fats.ui.StatusFragment;
import com.android.fatsgw.fats.utils.Converter;

public class WifiDirectExchanger implements Exchanger
{
	public final static int MAX_SERVICES = 13;
	public final static int MAX_BYTES = 85;
	private final static String DATA_KEY = "@";

	private Context _ctx; 
	private boolean _hasStarted; //prevent starting twice
	private boolean _isSendingData; //prevent simultaenous discovery
	private boolean _hasError; //wifi direct has thrown an error
	private boolean _isWifiRestarting; //wifi 

	private WifiDirectPolicyHandler _policy;

	//================= WiFi Direct =================
	private WifiP2pManager _wdManager;
	private WifiManager _wifiManager;
	private Channel _wdChannel;


	//================= WiFi Direct Discovery Timer =================
	private final static long BROADCAST_DURATION = 120000; //1 minute
	private final static long BROADCAST_INTERMISSION = 5000; //1 minute
	private final static long PRE_BROADCAST_DELAY = 1000; // 1sec
	private final static long FORCE_RESTART_TIME = 10000; //10 seconds
	private final static long WIFI_RESTART_TIME = 1800000; //30 minutes

	private Timer _broadcastTimer;
	private Timer _wifiRestartTimer;
	private boolean _forceRestarting;
	private boolean _wifiRestartTimedOut; //time to restart wifi


	//================= Broadcast Receiver =================
	private IntentFilter _wifiIntentFilter= new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
	private IntentFilter _databaseIntentFilter = new IntentFilter(FatsDatabase.DATABASE_EVENT_REGISTERED_APP_CHANGED);
	private BroadcastReceiver _dbBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction(); 
			if (action == null)
				return;

			if (action.equals(FatsDatabase.DATABASE_EVENT_REGISTERED_APP_CHANGED))
			{
				Log.d(MainActivity.DEBUG_TAG, "received broadcast: database app change");
				_policy.refresh(); //refresh when registered app list changes
				startSendingData();
			}
		}
	};
	private BroadcastReceiver _wifiBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction(); 
			if (action == null)
				return;

			if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION))
			{
				int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
				switch (wifiState)
				{
				case WifiManager.WIFI_STATE_ENABLED:
					Log.d(MainActivity.DEBUG_TAG, "wifi state changed to ENABLED");
					Log.d(MainActivity.DEBUG_TAG, "Is Sending Data = " + _isSendingData);
					if (!_isSendingData)
					{
						Log.d(MainActivity.DEBUG_TAG, "Starting timers!");
						stopAllBroadcastTimer();
						startDelayedSendingData(); //delay for wifi to fully enable all settings
						//startWifiRestartTimer();
					}
					break;
				case WifiManager.WIFI_STATE_DISABLING:
					Log.d(MainActivity.DEBUG_TAG, "wifi state changed to DISABLING");
					break;
				case WifiManager.WIFI_STATE_DISABLED:
					Log.d(MainActivity.DEBUG_TAG, "wifi state changed to DISABLED");

					//stopSendingData();
					//stopWifiRestartTimer();

					//if wifi is disabled by us, start wifi again
					if (_isWifiRestarting)
					{
						_isWifiRestarting = false;
						_wifiManager.setWifiEnabled(true);
					}
					break;
				}
			}
		}
	}; 


	public WifiDirectExchanger(Context ctx)
	{
		_ctx = ctx;
		_hasStarted = false;
		_isSendingData = false;
		_isWifiRestarting = false;
		_hasError = false;

	
	}


	/**Starts sending data through Service Discovery Broadcast.
	 * Will stop previous broadcast before starting a new one.
	 */
	private void WFDstartDiscovery()
	{
		//clear any existing discovery processes
		WFDstopDiscovery();

		//set up discovery requests
		_wdManager.setDnsSdResponseListeners(_wdChannel, null, getTxtRecordListener());
		_wdManager.addServiceRequest(_wdChannel, WifiP2pDnsSdServiceRequest.newInstance(), new ActionListener()
		{
			@Override
			public void onSuccess()
			{
				Log.d(MainActivity.DEBUG_TAG, "WifiDirect: Data sending request added.");
			}

			@Override
			public void onFailure(int errorCode)
			{
				_hasError = true;
				Log.e(MainActivity.DEBUG_TAG, "WifiDirect: Failed to add SENDING REQUEST. Error code:" + errorCode);

				//update main screen
				Intent i = new Intent(StatusFragment.STATUS_ERROR);
				i.putExtra(StatusFragment.EXTRA_ERROR, "Add service request ERROR");
				FatsApp.getLocalBroadcastManager().sendBroadcast(i);
			}
		});


		//start the discovery service
		_wdManager.discoverServices(_wdChannel, new ActionListener()
		{
			@Override
			public void onSuccess()
			{		
				Log.d(MainActivity.DEBUG_TAG, "WifiDirect: Data sending started.");
			}

			@Override
			public void onFailure(int errorCode)
			{				
				_hasError = true;
				Log.e(MainActivity.DEBUG_TAG, "WifiDirect: Data sending failed to start. Error code:" + errorCode);

				//update main screen
				Intent i = new Intent(StatusFragment.STATUS_ERROR);
				i.putExtra(StatusFragment.EXTRA_ERROR, "Discover Services ERROR");
				FatsApp.getLocalBroadcastManager().sendBroadcast(i);
			}
		});
	}


	/** Stops and clears the Service Discovery Broadcast.
	 * 
	 */
	private void WFDstopDiscovery()
	{
		//stops the discovery
		_wdManager.clearServiceRequests(_wdChannel, new ActionListener()
		{
			@Override
			public void onSuccess()
			{
				Log.d(MainActivity.DEBUG_TAG, "WifiDirect: Data sending stopped");
			}

			@Override
			public void onFailure(int errorCode)
			{
				Log.e(MainActivity.DEBUG_TAG, "WifiDirect: Unable to stop sending data. Error code:" + errorCode);
			}
		});
	}


	/** Grabs data from FatsDatabase directly.
	 * Will clean up previously registered data and update with new data.
	 * @return True if there are data for sending.
	 */
	private boolean WFDregisterData()
	{
		//clear any existing registered data
		WFDclearRegisteredData();

		//get data to send
		ArrayList<DataPack> dataPacksToSend = _policy.getDataPacksToSend();
		//no apps registered
		if (dataPacksToSend.size() == 0)
		{
			Log.d(MainActivity.DEBUG_TAG, "WifiDirect: No registered apps");
			return false;
		}

		for (int i = 0; i < dataPacksToSend.size(); i++)
		{
			//setup text record
			Map<String, String> txtRecord = new HashMap<String, String>();
			txtRecord.put(DATA_KEY, dataPacksToSend.get(i).getSendableDataString());

			//register local service  

			WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(Converter.longToSpecialString(dataPacksToSend.get(i).getAppIdentifier()), DataExchangeService.SERVICE_TYPE, txtRecord);
			_wdManager.addLocalService(_wdChannel, serviceInfo, new ActionListener()
			{
				@Override
				public void onSuccess()
				{
					Log.d(MainActivity.DEBUG_TAG, "WifiDirect: Registered app as local service");
				}

				@Override
				public void onFailure(int reason)
				{
					_hasError = true;
					Log.e(MainActivity.DEBUG_TAG, "WifiDirect: Failed to register app as local service. Error code: " + reason);

					//update main screen
					Intent i = new Intent(StatusFragment.STATUS_ERROR);
					i.putExtra(StatusFragment.EXTRA_ERROR, "Add LOCAL service ERROR");
					FatsApp.getLocalBroadcastManager().sendBroadcast(i);
				}	
			});
		}

		return true;
	}


	/** Clears all registered data.
	 * 
	 */
	private void WFDclearRegisteredData()
	{
		_wdManager.clearLocalServices(_wdChannel, new ActionListener()
		{
			@Override
			public void onSuccess()
			{
				Log.d(MainActivity.DEBUG_TAG, "WifiDirect: Local services cleared.");
			}

			@Override
			public void onFailure(int errorCode)
			{
				Log.e(MainActivity.DEBUG_TAG, "WifiDirect: Failed to clear Local Services. Error code:" + errorCode);
			}
		});
	}


	private DnsSdTxtRecordListener getTxtRecordListener()
	{
		return new DnsSdTxtRecordListener()
		{
			@Override
			public void onDnsSdTxtRecordAvailable(String instanceNameDotServiceType, Map<String, String> txtRecord, WifiP2pDevice srcDevice)
			{
				String[] instanceNameServiceType = instanceNameDotServiceType.split("\\.");
				Log.d(MainActivity.DEBUG_TAG, "Record found: " + instanceNameDotServiceType);

				//service type belongs to our app
				if(instanceNameServiceType[1].equals(DataExchangeService.SERVICE_TYPE))
				{		
					//convert raw data back to datapacks
					DataPack receivedPack = DataPack.MakeReceivingDataPack(Converter.specialStringToLong(instanceNameServiceType[0]), srcDevice.deviceAddress, srcDevice.deviceName, txtRecord.get(DATA_KEY), FatsApp.getCurrentTime()); 

					if (receivedPack == null)
						Log.e(MainActivity.DEBUG_TAG, "Invalid Data Received");
					else
					{
						//insert datapack into policy handler for handling
						boolean hasInterrupt = _policy.onDataPackReceived(receivedPack);

						//start force restart timer to refresh the packets being sent
						if (hasInterrupt)
							startForceRestartTimer();
					}
				}
				else
				{
					Log.d(MainActivity.DEBUG_TAG, "Unknown local service found:" + instanceNameDotServiceType);
				}
			}
		};
	}


	private void startDelayedSendingData()
	{
		Log.d(MainActivity.DEBUG_TAG, "_broadcastTimer = " + _broadcastTimer);
		if (_broadcastTimer == null)
			return;

		//initialize 
		TimerTask delayTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				startSendingData();
			}
		};

		//start a delayed data sending
		_broadcastTimer.schedule(delayTimerTask, PRE_BROADCAST_DELAY);
		Log.d(MainActivity.DEBUG_TAG, "Broadcast delay timer running...");
	}


	/** Starts data sending and a timer to automatic restart the sending after it ends.
	 * 
	 */
	private void startSendingData()
	{
		Log.d(MainActivity.DEBUG_TAG, "sending data...");
		//default to false first
		_isSendingData = false;

		//stops any running timer
		stopAllBroadcastTimer();

		//don't attempt to send if wifi is not on
		if (!_wifiManager.isWifiEnabled())
		{			
			_wifiRestartTimedOut = false; //already off, do not need to restart
			return;
		}

		//need to restart wifi now
		if (_wifiRestartTimedOut)
		{
			restartWifi();
			_wifiRestartTimedOut = false;
			return;
		}	

		//register data to send
		boolean hasDataToSend = WFDregisterData();
		if (hasDataToSend)
		{
			//start broadcasting
			WFDstartDiscovery();

			Log.d(MainActivity.DEBUG_TAG, "starting timer data...");
			//start timer to control broadcast duration
			startBroadcastDurationTimer();

			_isSendingData = true;
		}
	}


	/** Stops the sending and clears the automatic timer.
	 * 
	 */
	private void stopSendingData()
	{
		_isSendingData = false;
		
		//stop the timers
		stopAllBroadcastTimer();

		//stops the discovery
		WFDstopDiscovery();

		//clear any data in cache
		WFDclearRegisteredData();

		//update main screen
		Intent i = new Intent(StatusFragment.STATUS_OFF);
		FatsApp.getLocalBroadcastManager().sendBroadcast(i);
	}


	private void startBroadcastDurationTimer()
	{
		if (_broadcastTimer == null)
			return;

		//initialize
		TimerTask durationTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				//if force restart timer has started, but regular broadcast ended first, restart automatically
				if (_forceRestarting)
				{
					Log.d(MainActivity.DEBUG_TAG, "Broadcast force restarting...");
					startSendingData();
					_forceRestarting = false;
				}
				else
				{
					Log.d(MainActivity.DEBUG_TAG, "Broadcast duration timer ended.");
					//stop broadcasting
					stopSendingData();
					
					//disable wifi
					_wifiManager.setWifiEnabled(false);
					
					//start intermission
					startBroadcastIntermissionTimer();
				}
			}
		};


		//start a duration timer task
		_broadcastTimer.schedule(durationTimerTask, BROADCAST_DURATION);

		//update main screen
		Intent i = new Intent(StatusFragment.STATUS_DURATION);
		i.putExtra(StatusFragment.EXTRA_VALUE, BROADCAST_DURATION);
		FatsApp.getLocalBroadcastManager().sendBroadcast(i);

		Log.d(MainActivity.DEBUG_TAG, "Broadcast duration timer running...");
	}


	private void startBroadcastIntermissionTimer()
	{
		if (_broadcastTimer == null)
			return;


		TimerTask intermissionTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				Log.d(MainActivity.DEBUG_TAG, "Broadcast intermission ended");
				//intermission over, start sending again
				//startSendingData();
				
				_wifiManager.setWifiEnabled(true);
			}
		};

		//start an intermission timer
		_broadcastTimer.schedule(intermissionTimerTask, BROADCAST_INTERMISSION);

		//update main screen
		Intent i = new Intent(StatusFragment.STATUS_INTERMISSION);
		i.putExtra(StatusFragment.EXTRA_VALUE, BROADCAST_INTERMISSION);
		FatsApp.getLocalBroadcastManager().sendBroadcast(i);

		Log.d(MainActivity.DEBUG_TAG, "Broadcast intermission timer running...");
	}


	private void stopAllBroadcastTimer()
	{
		if (_broadcastTimer != null)
		{
			_broadcastTimer.cancel();
		}
		_broadcastTimer = new Timer();

	}


	private void startForceRestartTimer()
	{
		//don't start force restart twice
		if (_forceRestarting)
			return;

		//initialize timer task

		TimerTask forceRestartTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				//restart data sending
				Log.d(MainActivity.DEBUG_TAG, "Broadcast force restarting...");
				startSendingData();
				_forceRestarting = false;
			}
		};


		//starts a force restart timer
		_broadcastTimer.schedule(forceRestartTimerTask, FORCE_RESTART_TIME);
		//so that force restart isn't run twice
		_forceRestarting = true;

		//update main screen
		Intent i = new Intent(StatusFragment.STATUS_HOTSWAP);
		FatsApp.getLocalBroadcastManager().sendBroadcast(i);

		Log.d(MainActivity.DEBUG_TAG, "Broadcast force restart timer running...");
	}


	/** Register or unregister intent filters
	 * @param enable
	 */
	private void enableIntentFilters(boolean enable)
	{
		if (enable)
		{
			FatsApp.getLocalBroadcastManager().registerReceiver(_dbBroadcastReceiver, _databaseIntentFilter);
			_ctx.registerReceiver(_wifiBroadcastReceiver, _wifiIntentFilter);
		}
		else
		{
			FatsApp.getLocalBroadcastManager().unregisterReceiver(_dbBroadcastReceiver);
			_ctx.unregisterReceiver(_wifiBroadcastReceiver);
		}
	}

	private void startWifiRestartTimer()
	{
		if (_wifiRestartTimer != null)
			_wifiRestartTimer.cancel();

		_wifiRestartTimer = new Timer();
		TimerTask restartTask = new TimerTask()
		{
			@Override
			public void run()
			{
				_wifiRestartTimedOut = true;
				Log.d(MainActivity.DEBUG_TAG, "Wifi Restart Timer TIMED OUT");
			}
		};
		_wifiRestartTimer.schedule(restartTask, WIFI_RESTART_TIME);
		
		Log.d(MainActivity.DEBUG_TAG, "Wifi Restart Timer running...");
	}

	private void stopWifiRestartTimer()
	{
		if (_wifiRestartTimer != null)
			_wifiRestartTimer.cancel();
		_wifiRestartTimedOut = false;
	}

	private void restartWifi()
	{
		if (_wifiManager.isWifiEnabled())
		{
			_isWifiRestarting = true;
			_wifiManager.setWifiEnabled(false);
		}
		else
		{
			_isWifiRestarting = false;
			_wifiManager.setWifiEnabled(true);
		}
	}

	@Override
	public boolean start()
	{	
		Log.d(MainActivity.DEBUG_TAG, "WifiDirect: Starting Exchanger");

		if (_hasStarted)
		{
			Log.e(MainActivity.DEBUG_TAG, "WifiDirect: Unable to start. Already Started");
			return false;
		}
		else
		{
			//prevent starting twice
			_hasStarted = true;

			//initialize policy
			_policy = new WifiDirectPolicyHandler();

			//register intent filters
			enableIntentFilters(true);

			//get android wifi manager (required for restarting wifi)
			_wifiManager = (WifiManager) _ctx.getSystemService(Context.WIFI_SERVICE);
			//Set up for Android WiFi Direct
			_wdManager = (WifiP2pManager) _ctx.getSystemService(Context.WIFI_P2P_SERVICE);
			_wdChannel = _wdManager.initialize(_ctx, _ctx.getMainLooper(), null);

			//start wifi is disabled, restart wifi if enabled
			restartWifi(); 

			return true;
		}
	}


	@Override
	public boolean stop()
	{
		Log.d(MainActivity.DEBUG_TAG, "WifiDirect: Stopping Exchanger");
		_hasStarted = false;

		stopSendingData();
		
		stopWifiRestartTimer();

		_policy = null;
		_wifiManager = null;
		_broadcastTimer = null;


		//unregister intent filters
		enableIntentFilters(false);

		return true;
	}


	@Override
	public boolean restart()
	{
		Log.d(MainActivity.DEBUG_TAG, "WifiDirect: Restarting Exchanger");

		//allow restarts only if it is already running
		if (!_hasStarted)
		{
			Log.e(MainActivity.DEBUG_TAG, "WifiDirect: Unable to restart an exchanger that is not running.");
			return false;
		}

		boolean stopSucceeded = stop();
		boolean startSucceeded = start();

		return stopSucceeded && startSucceeded;
	}
}
