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

public class AppsFragment extends Fragment
{
	private SimpleCursorAdapter _dataAdapter;
	private ListView _listView;
	private TextView _appCountTextView;
	private BroadcastReceiver _receiver;



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		//this.setHasOptionsMenu(true);
		View rootView = inflater.inflate(R.layout.fragment_app, container, false);

		//initialize
		_listView = (ListView) rootView.findViewById(R.id.appListView);
		_appCountTextView = (TextView) rootView.findViewById(R.id.numAppsTextView);

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
		{
			IntentFilter filter = new IntentFilter();
			filter.addAction(FatsDatabase.DATABASE_EVENT_REGISTERED_APP_CHANGED);
			filter.addAction(FatsDatabase.DATABASE_EVENT_REGISTERED_DATA_CHANGED);

			FatsApp.getLocalBroadcastManager().registerReceiver(_receiver, filter);
		}
		else
			FatsApp.getLocalBroadcastManager().unregisterReceiver(_receiver);
	}

	private void refreshListView()
	{
		FatsDatabase db = new FatsDatabase();
		Cursor cursor = db.getAllRegisteredAppCursor();
		_appCountTextView.setText("" + cursor.getCount());

		if (_dataAdapter != null)
			_dataAdapter.changeCursor(cursor);

	}


	private void displayListView()
	{
		FatsDatabase db = new FatsDatabase();
		Cursor cursor = db.getAllRegisteredAppCursor();
		_appCountTextView.setText("" + cursor.getCount());

		//columns to be bound
		String[] fromColumns = new String[]
				{
				DatabaseContract.RegisteredAppsDb.COLUMN_NAME_APP_ID,
				DatabaseContract.RegisteredAppsDb.COLUMN_NAME_APP_NAME,
				DatabaseContract.RegisteredAppsDb.COLUMN_NAME_PACK_NAME,
				DatabaseContract.RegisteredAppsDb.COLUMN_NAME_DATA_VERSION,
				DatabaseContract.RegisteredAppsDb.COLUMN_NAME_DATA
				};

		//text view to bind to
		int[] toViews = new int[]
				{
				R.id.list_appIDTextView,
				R.id.list_appNameTextView,
				R.id.list_appPNameTextView,
				R.id.list_appVersionTextView,
				R.id.list_appDataTextView
				};

		_dataAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_app, cursor, fromColumns, toViews, 0);
		_listView.setAdapter(_dataAdapter);


	}
}
