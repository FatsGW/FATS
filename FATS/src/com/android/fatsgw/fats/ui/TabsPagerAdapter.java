package com.android.fatsgw.fats.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

public class TabsPagerAdapter extends FragmentPagerAdapter
{
	public TabsPagerAdapter(FragmentManager fm)
	{
		super(fm);
	}

	@Override
	public Fragment getItem(int index)
	{
		switch (index)
		{
			case 0:
				return new StatusFragment();
			case 1:
				return new AppsFragment();
			case 2:
				return new PasserbyFragment();
			case 3:
				return new ReceivedDataFragment();
			default:
				return null;
		}

	}

	@Override
	public int getCount()
	{
		return 4;
	}

	

}
