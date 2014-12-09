package com.android.fatsgw.fats.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.fatsgw.fats.FatsApp;
import com.android.fatsgw.fats.R;
import com.android.fatsgw.fats.database.DatabaseContract;
import com.android.fatsgw.fats.database.FatsDatabase;

public class ReceivedDataFragment extends Fragment
{
	private SimpleCursorAdapter _dataAdapter;
	private ListView _listView;
	private TextView _receivedCountTextView;
	private BroadcastReceiver _receiver;



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		//this.setHasOptionsMenu(true);
		View rootView = inflater.inflate(R.layout.fragment_received, container, false);

		//initialize
		_listView = (ListView) rootView.findViewById(R.id.receivedListView);
		_receivedCountTextView = (TextView) rootView.findViewById(R.id.numReceivedTextView);

		//register database change listeners



		return rootView;
	}


	@Override
	public void onResume()
	{
		super.onResume();

		//load data into the list view
		displayListView();

		//register broadcast
		registerBroadcastReceivers(true);
	}


	@Override
	public void onPause()
	{
		super.onPause();

		//unregister receivers
		registerBroadcastReceivers(false);
		
		_dataAdapter.changeCursor(null);
		_dataAdapter = null;
	}


	private void registerBroadcastReceivers(boolean enable)
	{
		//initialize receiver
		if (_receiver == null)
		{
			_receiver = new BroadcastReceiver()
			{
				@Override
				public void onReceive(Context context, Intent intent)
				{
					//refresh list
					refreshListView();
				}	
			};
		}

		if (enable)
			FatsApp.getLocalBroadcastManager().registerReceiver(_receiver, new IntentFilter(FatsDatabase.DATABASE_EVENT_RECEIVED_DATA_CHANGED));
		else
			FatsApp.getLocalBroadcastManager().unregisterReceiver(_receiver);
	}

	private void refreshListView()
	{
		FatsDatabase db = new FatsDatabase();
		Cursor cursor = db.getAllAppReceivedDataCursor();
		_receivedCountTextView.setText("" + cursor.getCount());

		if (_dataAdapter != null)
			_dataAdapter.changeCursor(cursor);
	}


	private void displayListView()
	{
		FatsDatabase db = new FatsDatabase();
		Cursor cursor = db.getAllAppReceivedDataCursor();
		_receivedCountTextView.setText("" + cursor.getCount());



		//columns to be bound
		String[] fromColumns = new String[]
				{
				DatabaseContract.RegisteredAppsDb.COLUMN_NAME_APP_NAME,
				DatabaseContract.PasserByRecordsDb.COLUMN_NAME_DEVICE_NAME,
				DatabaseContract.ReceivedDataDb.COLUMN_NAME_DATA,
				DatabaseContract.ReceivedDataDb.COLUMN_NAME_DATA_VERSION,
				DatabaseContract.ReceivedDataDb.COLUMN_NAME_TIME_RECEIVED
				};

		//text view to bind to
		int[] toViews = new int[]
				{
				R.id.list_receivedAIDTextView,
				R.id.list_receivedPIDTextView,
				R.id.list_receivedDataTextView,
				R.id.list_receivedVersionTextView,
				R.id.list_receivedTimeTextView
				};

		_dataAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_received, cursor, fromColumns, toViews, 0);
		_listView.setAdapter(_dataAdapter);

	}

}
