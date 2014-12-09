package com.android.fatsgw.fats;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import com.android.fatsgw.fats.exchangers.Exchanger;
import com.android.fatsgw.fats.exchangers.WifiDirectExchanger;

public class DataExchangeService extends Service
{
	public static final String SERVICE_TYPE = "fats";

	private static final int EXCHANGER_WIFI_D = 0;
	private static final int EXCHANGER_NSD = 1;
	private static final int EXCHANGER_BLE = 2;

	private SparseArray<Exchanger> _exchangers;

	@Override
	public void onCreate()
	{
		Log.d(MainActivity.DEBUG_TAG, "Service Created");
		_exchangers = new SparseArray<Exchanger>();

		//activate the exchangers
		activateExchangers();
	}


	@Override
	public void onDestroy()
	{
		// clean up the exchangers
		if (_exchangers != null)
		{
			for (int i = 0; i < _exchangers.size(); i++)
				_exchangers.valueAt(i).stop();
			_exchangers.clear();
			_exchangers = null;
		}
		
		Log.d(MainActivity.DEBUG_TAG, "Service Destroyed");
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{		
		Log.d(MainActivity.DEBUG_TAG, "Service Start Command received");
		return Service.START_STICKY; //auto restart the service if forcefully terminated by the system
	}


	@Override
	public IBinder onBind(Intent intent)
	{
		return null; //do not allow binding
	}


	/** Checks the exchangers to activate
	 * @return an int array with the IDs of the exchangers to activate
	 */
	private int[] getExchangersToActivate()
	{
		//TODO: do a proper check of which exchanger to activate
		return new int[]{EXCHANGER_WIFI_D};
	}


	/** Activate the exchangers and store in a SparseArray to keep track of
	 * 
	 */
	private void activateExchangers()
	{
		//initialize exchangers
		int[] targetExchangers = getExchangersToActivate();
		for (int exchangerId: targetExchangers)
		{
			switch (exchangerId)
			{
			case EXCHANGER_WIFI_D:
				Exchanger wifiDirect = new WifiDirectExchanger(this);
				_exchangers.put(EXCHANGER_WIFI_D, wifiDirect);
				wifiDirect.start();
				break;
			case EXCHANGER_NSD:
				break;
			case EXCHANGER_BLE:
				break;
			}
		}
	}
}
