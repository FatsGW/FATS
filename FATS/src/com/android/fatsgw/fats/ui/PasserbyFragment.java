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

public class PasserbyFragment extends Fragment
{
	private SimpleCursorAdapter _dataAdapter;
	private ListView _listView;
	private TextView _passerbyCountTextView;

	private BroadcastReceiver _receiver;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		//this.setHasOptionsMenu(true);
		View rootView = inflater.inflate(R.layout.fragment_passerby, container, false);

		//initialize
		_listView = (ListView) rootView.findViewById(R.id.passerbyListView);
		_passerbyCountTextView = (TextView) rootView.findViewById(R.id.numPasserbysTextView);

		return rootView;
	}


	@Override
	public void onResume()
	{
		super.onResume();

		displayListView();
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
			FatsApp.getLocalBroadcastManager().registerReceiver(_receiver, new IntentFilter(FatsDatabase.DATABASE_EVENT_PASSERBY_CHANGED));
		else
			FatsApp.getLocalBroadcastManager().unregisterReceiver(_receiver);
	}


	private void refreshListView()
	{
		FatsDatabase db = new FatsDatabase();
		Cursor cursor = db.getAllPasserByRecordsCursor();
		_passerbyCountTextView.setText("" + cursor.getCount());

		if (_dataAdapter != null)
			_dataAdapter.changeCursor(cursor);
	}


	private void displayListView()
	{
		FatsDatabase db = new FatsDatabase();
		Cursor cursor = db.getAllPasserByRecordsCursor();
		_passerbyCountTextView.setText("" + cursor.getCount());

		//columns to be bound
		String[] fromColumns = new String[]
				{
				DatabaseContract.PasserByRecordsDb.COLUMN_NAME_DEVICE_NAME,
				DatabaseContract.PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID,
				DatabaseContract.PasserByRecordsDb.COLUMN_NAME_TIMES_MET,
				DatabaseContract.PasserByRecordsDb.COLUMN_NAME_TIME_STAMP,
				};

		//text view to bind to
		int[] toViews = new int[]
				{
				R.id.list_pbNameTextView,
				R.id.list_pbIdTextView,
				R.id.list_pbTimesMetTextView,
				R.id.list_pbTimeStampTextView
				};

		_dataAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_passerby, cursor, fromColumns, toViews, 0);
		_listView.setAdapter(_dataAdapter);

	}
}
