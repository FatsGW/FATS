package com.android.fatsgw.fats.exchangers;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Intent;
import android.util.Log;
import android.util.LongSparseArray;

import com.android.fatsgw.fats.FatsApp;
import com.android.fatsgw.fats.MainActivity;
import com.android.fatsgw.fats.database.FatsDatabase;
import com.android.fatsgw.fats.dataholders.DataPack;
import com.android.fatsgw.fats.dataholders.RegisteredApp;
import com.android.fatsgw.fats.ui.StatusFragment;

public class WifiDirectPolicyHandler
{
	private LinkedList<Long> _waitingQueue;
	private LinkedList<Long> _sendingQueue;
	private LinkedList<Long> _refreshQueue;
	private LinkedList<Long> _interruptQueue;
	private int _sendingUnitCount;	

	private LongSparseArray<LinkedList<DataPack>> _segmentedPacks;

	public WifiDirectPolicyHandler()
	{
		FatsDatabase db = new FatsDatabase();
		//setup queue of apps
		_waitingQueue = new LinkedList<Long>();
		long[] appIds = db.getAllRegisteredAppId();
		db.close();
		for (long id: appIds)
			_waitingQueue.add(id);

		//setup all the queues
		_sendingQueue = new LinkedList<Long>();
		_refreshQueue = new LinkedList<Long>();
		_interruptQueue = new LinkedList<Long>();

		//unit count
		_sendingUnitCount = 0;

		//holder for any segmented datapacks
		_segmentedPacks = new LongSparseArray<LinkedList<DataPack>>();
	}


	public ArrayList<DataPack> getDataPacksToSend()
	{
		ArrayList<DataPack> packList = new ArrayList<DataPack>();
		
		//put everything in sending queue back to waiting queue
		_waitingQueue.addAll(_sendingQueue);
		
		//clear sending queue
		_sendingQueue = new LinkedList<Long>();
		_sendingUnitCount = 0;

		//organize data from queues to data packs, while maintaining WiFiDirect service limit
		organizeDataPacks(_interruptQueue, packList);
		organizeDataPacks(_refreshQueue, packList);
		organizeDataPacks(_waitingQueue, packList);

		//send data to GUI
		String[] message = new String[_sendingQueue.size()];
		FatsDatabase db = new FatsDatabase();
		for (int i = 0; i < message.length; i++)
			message[i] = db.getRegisteredAppPackageName(_sendingQueue.get(i));
		db.close();
		Intent i = new Intent(StatusFragment.STATUS_APP_LIST);
		i.putExtra(StatusFragment.EXTRA_APP_LIST, message);
		FatsApp.getLocalBroadcastManager().sendBroadcast(i);
		
		return packList;
	}


	private void organizeDataPacks(LinkedList<Long> fromQueue, ArrayList<DataPack> toList)
	{
		FatsDatabase db = new FatsDatabase();
		for (int i = 0; i < fromQueue.size(); i++)
		{
			if (_sendingUnitCount >= WifiDirectExchanger.MAX_SERVICES)
				break;

			RegisteredApp app = db.getRegisteredApp(fromQueue.get(i));
			if (app == null) //invalid ids
			{
				fromQueue.remove(i);
				i--;
			}
			else
			{
				//look for a fitting app data 
				int dataUnits = calculateDataUnits(app);
				if (_sendingUnitCount + dataUnits > WifiDirectExchanger.MAX_SERVICES)
					continue;
				else
				{
					//shift from waiting to sending queue.
					_sendingUnitCount += dataUnits;
					_sendingQueue.push(app.getIdentifier());
					fromQueue.remove(i);
					i--;

					DataPack pack = DataPack.MakeSendingDataPack(app);
					if (dataUnits > 1)
					{
						DataPack[] segments = DataPack.SplitAppData(pack, WifiDirectExchanger.MAX_BYTES);
						for (DataPack segment : segments)
							toList.add(segment);
					}
					else
						toList.add(pack);
				}
			}
		}
		db.close();
	}


	public boolean onDataPackReceived(DataPack pack)
	{
		//handle segmented packet
		if (pack.getPackMaxNumber() > 1)
		{
			handleSegmentedPacket(pack);
			return false;
		}
		
		FatsDatabase db = new FatsDatabase();
		db.insertPack(pack);
		db.close();
		
		//adjusts app's position in the queue, depending on where it exists currently
		long appId = pack.getAppIdentifier();
		if (_sendingQueue.contains(appId))
		{
			Log.d(MainActivity.DEBUG_TAG, "found in sending queue");
			//add app into refresh queue
			_sendingQueue.remove(appId);
			_refreshQueue.add(appId);
			
			return false;
		}
		else if (_waitingQueue.contains(appId))
		{
			Log.d(MainActivity.DEBUG_TAG, "found in waiting queue");
			//add app into interrupt queue
			_waitingQueue.remove(appId);
			_interruptQueue.add(appId);

			return true; //needs to swap immediately
		}
		else if (_interruptQueue.contains(appId))
		{
			Log.d(MainActivity.DEBUG_TAG, "found in interrupt queue");
			
			return true; //needes to swap immediately
		}
		else
		{
			Log.d(MainActivity.DEBUG_TAG, "not found");
			return false;
		}
	}


	private void handleSegmentedPacket(DataPack pack)
	{
		long packId = pack.getAppIdentifier();

		//check whether pack is created in the sparse array
		LinkedList<DataPack> packSegments = _segmentedPacks.get(packId);
		if (packSegments == null)
		{
			//create a new pack segment and add into sparse array
			LinkedList<DataPack> newPackSegment = new LinkedList<DataPack>();
			newPackSegment.add(pack);
			_segmentedPacks.append(packId, newPackSegment);
		}
		else
		{
			//add new pack into existing segment and check whether it is ready for merging
			packSegments.add(pack);
			if (packSegments.size() == pack.getPackMaxNumber())
			{
				//merge and handle merged packet
				DataPack mergedPack = DataPack.MergeDataPacks((DataPack[])packSegments.toArray());
				onDataPackReceived(mergedPack);

				//remove this segment from the sparse array
				_segmentedPacks.delete(packId);
			}
		}
	}


	public void refresh()
	{
		FatsDatabase db = new FatsDatabase();
		//setup queue of apps
		_waitingQueue = new LinkedList<Long>();
		long[] appIds = db.getAllRegisteredAppId();
		db.close();
		for (long id: appIds)
			_waitingQueue.add(id);

		//setup all the queues
		_sendingQueue = new LinkedList<Long>();
		_refreshQueue = new LinkedList<Long>();
		_interruptQueue = new LinkedList<Long>();
	}


	private int calculateDataUnits(RegisteredApp app)
	{
		return (int) Math.ceil((double)app.getData().length() / WifiDirectExchanger.MAX_BYTES);
	}

}
