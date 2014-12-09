package com.android.fatsgw.fats;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.Toast;

import com.android.fatsgw.fats.database.FatsDatabase;
import com.android.fatsgw.fats.ui.TabsPagerAdapter;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener{

	public static final String DEBUG_TAG = "FATS";
	private final String[] _tabNames = {"Status", "Apps", "Passerbys", "Received Data"};
	private final String ON_OFF_STATE_KEY = "com.android.fatsgw.fats.ON_OFF_STATE";

	private ViewPager _viewPager;
	private TabsPagerAdapter _pageAdapter;
	private ActionBar _actionBar;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//initialize the ViewPager navigation
		_pageAdapter = new TabsPagerAdapter(getSupportFragmentManager());
		_viewPager = (ViewPager) findViewById(R.id.pager);
		_viewPager.setAdapter(_pageAdapter);

		//initialize ActionBar tab navigation
		_actionBar = getActionBar();
		_actionBar.setHomeButtonEnabled(false);
		_actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		for (String tabName: _tabNames)
			_actionBar.addTab(_actionBar.newTab().setText(tabName).setTabListener(this));

		//link ViewPager with ActionBar's tabs
		_viewPager.setOnPageChangeListener(new OnPageChangeListener()
		{
			@Override
			public void onPageScrollStateChanged(int arg0)
			{}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2)
			{}

			@Override
			public void onPageSelected(int position)
			{
				_actionBar.setSelectedNavigationItem(position);
			}
		});
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		//indeterminate circle
		final MenuItem circle = menu.findItem(R.id.spinningCircle);

		//setup on/off switch
		Switch onOffSwitch = (Switch)menu.findItem(R.id.actionBarSwitch).getActionView();
		onOffSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				saveOnOffState(isChecked);
				if (isChecked)
				{
					circle.setVisible(true);
					enableExchangeService(true);
				}
				else
				{	
					circle.setVisible(false);
					enableExchangeService(false);
				}
			}

		});

		//initial state
		boolean state = loadOnOffState();
		onOffSwitch.setChecked(state);
		circle.setVisible(state);

		return super.onPrepareOptionsMenu(menu);
	}




	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		FatsDatabase db = new FatsDatabase();
		switch (item.getItemId())
		{
		case R.id.testBtn:
			db.testInsertData();
			db.close();
			return true;
			
		case R.id.deleteAppBtn:
			db.clearAllRegisteredApp();
			db.close();
			return true;
			
		case R.id.deletePasserbyBtn:
			db.clearAllPasserBy();
			db.close();
			return true;
			
		case R.id.deleteReceivedBtn:
			db.clearAllAppReceivedData();
			db.close();
			return true;
			
		case R.id.deleteTestBtn:
			db.clearEntireDatabase();
			db.close();
			return true;
			
		case R.id.printAllBtn:
			db.testPrintAllData();
			db.close();
			return true;
			
		case R.id.readPowerValue:
			readPowerValue();
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}




	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft)
	{}


	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft)
	{
		//link ActionBar's tab with ViewPager
		_viewPager.setCurrentItem(tab.getPosition(), true);
	}


	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft)
	{}	


	private void enableExchangeService(boolean enable)
	{
		if (enable)
			startService(new Intent(this, DataExchangeService.class)); 
		else
			stopService(new Intent(this, DataExchangeService.class));
	}


	private void saveOnOffState(boolean state)
	{
		SharedPreferences prefs = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);
		prefs.edit().putBoolean(ON_OFF_STATE_KEY, state).apply();
	}


	private boolean loadOnOffState()
	{
		SharedPreferences prefs = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);
		return prefs.getBoolean(ON_OFF_STATE_KEY, false);
	}


	private boolean isServiceRunning(Class<?> serviceClass) 
	{
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) 
		{
			if (serviceClass.getName().equals(service.service.getClassName())) 
			{
				return true;
			}
		}
		return false;
	}
	
	private void readPowerValue()
	{
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = this.registerReceiver(null, ifilter);
		
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

		float batteryPct = level;
		Toast.makeText(this, "Battery:" + batteryPct, Toast.LENGTH_LONG).show();
		
	}


}
