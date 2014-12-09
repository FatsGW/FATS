package com.android.fatsgw.fats.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.fatsgw.fats.FatsApp;
import com.android.fatsgw.fats.R;
import com.android.fatsgw.fats.database.FatsDatabase;

public class StatusFragment extends Fragment
{
	private TextView _bcDurationTextView;
	private TextView _bcIntermissionTextView;
	private TextView _currentStateTextView;
	private TextView _bcDurationCountTextView;
	private TextView _bcIntermissionCountTextView;
	private TextView _broadcastList;
	private BroadcastReceiver _receiver;
	
	private String _broadcastText = "-";
	private String _durationText = "-";
	private String _intermissionText = "-";
	private String _stateText = "OFF";
	private int _durationCount = 0;
	private int _intermissionCount = 0;

	public static final String STATUS_APP_LIST = "status.app.list";
	public static final String EXTRA_APP_LIST = "status.extra.applist";

	public static final String STATUS_DURATION = "status.state.duration";
	public static final String STATUS_INTERMISSION = "status.state.intermission";
	public static final String STATUS_HOTSWAP = "status.state.hotswap";
	public static final String STATUS_OFF = "status.state.off";
	public static final String STATUS_ERROR = "status.state.error";
	public static final String EXTRA_ERROR =  "status.extra.error";
	public static final String EXTRA_VALUE = "status.extra.timevalue";


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		//this.setHasOptionsMenu(true);
		View rootView = inflater.inflate(R.layout.fragment_status, container, false);

		//initialize
		_broadcastList = (TextView) rootView.findViewById(R.id.broadcastingTextView);
		_bcDurationTextView = (TextView) rootView.findViewById(R.id.bcDurationTextView);
		_bcIntermissionTextView = (TextView) rootView.findViewById(R.id.bcIntermissionTextView);
		_currentStateTextView = (TextView) rootView.findViewById(R.id.currentStateTextView);
		_bcDurationCountTextView = (TextView) rootView.findViewById(R.id.broadcastCountTextView);
		_bcIntermissionCountTextView = (TextView) rootView.findViewById(R.id.intermissionCountTextView);


		_currentStateTextView.setKeepScreenOn(true); //TODO: REMOVE
		return rootView;
	}


	@Override
	public void onResume()
	{
		super.onResume();
		
		//insert broadcast message
		_broadcastList.setText(_broadcastText);
		
		//insert all text
		_bcDurationTextView.setText(_durationText);
		_bcIntermissionTextView.setText(_intermissionText);
		_currentStateTextView.setText(_stateText);
		_bcDurationCountTextView.setText("" + _durationCount);
		_bcIntermissionCountTextView.setText("" + _intermissionCount);

		//register broadcast
		registerBroadcastReceivers(true);
	}


	@Override
	public void onPause()
	{
		super.onPause();

		//unregister receivers
		registerBroadcastReceivers(false);
		
		//save message
		_broadcastText = _broadcastList.getText().toString();
		_durationText = _bcDurationTextView.getText().toString();
		_intermissionText = _bcIntermissionTextView.getText().toString();
		_stateText = _currentStateTextView.getText().toString();
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
					if (intent.getAction().equals(STATUS_APP_LIST))
					{
						String[] message = intent.getStringArrayExtra(EXTRA_APP_LIST);
						String msg = "";
						for (int i = 0; i < message.length; i++)
							msg += message[i] + " \n";
						_broadcastList.setText(msg);
					}
					else if (intent.getAction().equals(STATUS_INTERMISSION))
					{
						long intermissionDuration = intent.getLongExtra(EXTRA_VALUE, 0);
						_bcIntermissionTextView.setText("" + intermissionDuration);
						_currentStateTextView.setText("Intermission");
						_currentStateTextView.setTextColor(Color.RED);
						_intermissionCount++;
						_bcIntermissionCountTextView.setText("" + _intermissionCount);
					}
					else if (intent.getAction().equals(STATUS_DURATION))
					{
						long broadcastDuration = intent.getLongExtra(EXTRA_VALUE, 0);
						_bcDurationTextView.setText("" + broadcastDuration);
						_currentStateTextView.setText("Broadcast");
						_currentStateTextView.setTextColor(Color.GREEN);
						_durationCount++;
						_bcDurationCountTextView.setText("" + _durationCount);
					}
					else if (intent.getAction().equals(STATUS_HOTSWAP))
					{
						_currentStateTextView.setText("Hot Swap");
						_currentStateTextView.setTextColor(Color.BLUE);
					}
					else if (intent.getAction().equals(STATUS_OFF))
					{
						_currentStateTextView.setText("OFF");
						_currentStateTextView.setTextColor(Color.BLACK);
					}
					else if (intent.getAction().equals(STATUS_ERROR))
					{
						String errorMessage = intent.getStringExtra(EXTRA_ERROR);
						_currentStateTextView.setText(errorMessage);
						_currentStateTextView.setTextColor(Color.YELLOW);
					}
				}	
			};
		}

		if (enable)
		{
			IntentFilter filter = new IntentFilter();
			filter.addAction(FatsDatabase.DATABASE_EVENT_REGISTERED_APP_CHANGED);
			filter.addAction(FatsDatabase.DATABASE_EVENT_REGISTERED_DATA_CHANGED);

			//listen to intent on currently broadcasting apps
			filter.addAction(STATUS_DURATION);
			filter.addAction(STATUS_INTERMISSION);
			filter.addAction(STATUS_HOTSWAP);
			filter.addAction(STATUS_OFF);
			filter.addAction(STATUS_APP_LIST);
			filter.addAction(STATUS_ERROR);
			FatsApp.getLocalBroadcastManager().registerReceiver(_receiver, filter);
		}
		else
			FatsApp.getLocalBroadcastManager().unregisterReceiver(_receiver);
	}
}
