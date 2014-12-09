package com.android.fatsgw.namecardfatsdemo;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProfileGridAdapter extends BaseAdapter
{
	private Context ctx;
	private ArrayList<Profile> profiles;
	
	public ProfileGridAdapter(Context ctx, ArrayList<Profile> listOfProfiles)
	{
		profiles = listOfProfiles;
		this.ctx = ctx;
	}

	@Override
	public int getCount()
	{
		return profiles.size();
	}

	@Override
	public Object getItem(int position)
	{
		
		return null;
	}

	@Override
	public long getItemId(int position)
	{
		
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View grid;
		LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null)
		{
			grid = new View(ctx);
			grid = inflater.inflate(R.layout.grid_single, null);
		}
		else
		{
			grid = (View) convertView;
		}
		
		TextView name = (TextView)grid.findViewById(R.id.grid_name);
		ImageView icon = (ImageView)grid.findViewById(R.id.grid_icon);
		LinearLayout ll = (LinearLayout)grid.findViewById(R.id.grid_layout);
		
		//map data onto screen
		Profile currProfile = profiles.get(position);
		System.out.println("Showing:" + currProfile);
		name.setText(currProfile.name);
		icon.setImageResource(Profile.iconPickerArray[currProfile.iconId]);
		icon.setBackgroundColor(Profile.colourPickerArray[currProfile.colourId]);
		
		return grid;
	}

}
