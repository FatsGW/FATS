package com.android.fatsgw.namecardfatsdemo;

public class Profile
{
	public static final int[] colourPickerArray = {0xFFFF0000, 
													0xFFFFFF00, 
													0xFF00FF00, 
													0xFF00FFFF, 
													0xFF0000FF, 
													0xFFFF00FF, 
													0xFF000000, 
													0xFF777777}; 
	public static final int[] iconPickerArray = {R.drawable.ic_launcher, 
												R.drawable.ic_action_bad, 
												R.drawable.ic_action_call, 
												R.drawable.ic_action_discard, 
												R.drawable.ic_action_edit, 
												R.drawable.ic_action_favorite, 
												R.drawable.ic_action_flash_on, 
												R.drawable.ic_action_good, 
												R.drawable.ic_action_half_important, 
												R.drawable.ic_action_select_all, 
												R.drawable.ic_action_share,
												R.drawable.ic_action_camera,
												R.drawable.ic_action_web_site,
												R.drawable.ic_action_place,
												R.drawable.ic_action_map};
	
	public String name;
	public String message;
	public int colourId;
	public int iconId;

	public Profile(String name, String message, int colourId, int iconId)
	{
		this.name = name;
		this.message = message;
		this.colourId = colourId;
		this.iconId = iconId;
	}
	
	public String toString()
	{
		return "Name:" + name +"\nMessage:" + message + "\nColour Id:" + colourId + "\nIcon Id:" +iconId;
	}
}
