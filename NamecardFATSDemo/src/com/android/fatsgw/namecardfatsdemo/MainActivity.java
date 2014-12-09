package com.android.fatsgw.namecardfatsdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class MainActivity extends Activity
{
	private static final int LEFT = 1;
	private static final int RIGHT = 2;
	
	EditText nameEditText;
	EditText messageEditText;
	View colourPickerView;
	ImageView iconPickerImageView;
	
	int colourPickerId = 0;
	int iconPickerId = 0;
	
	
	
	View.OnClickListener colourPickerListener;
	View.OnClickListener iconPickerListener;
	View.OnClickListener findOthersClickedListener;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		nameEditText = (EditText)findViewById(R.id.nameEditText);
		messageEditText = (EditText)findViewById(R.id.messageEditText);
		colourPickerView = (View)findViewById(R.id.colourPickerTextView);
		iconPickerImageView = (ImageView)findViewById(R.id.iconImageView);
		
		//default colours
		colourPickerView.setBackgroundColor(Profile.colourPickerArray[colourPickerId]);
		
		//setup button listeners
		colourPickerListener = new View.OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				switch(v.getId())
				{
				case R.id.leftBtn_head:
					colourPickerScroll(LEFT);
					break;
				case R.id.rightBtn_head:
					colourPickerScroll(RIGHT);
					break;
				}
			}
		};
		findViewById(R.id.leftBtn_head).setOnClickListener(colourPickerListener);
		findViewById(R.id.rightBtn_head).setOnClickListener(colourPickerListener);
		
		
		iconPickerListener = new View.OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				switch(v.getId())
				{
				case R.id.leftBtn_body:
					iconPickerScroll(LEFT);
					break;
				case R.id.rightBtn_body:
					iconPickerScroll(RIGHT);
					break;
				}
			}
		};
		findViewById(R.id.leftBtn_body).setOnClickListener(iconPickerListener);
		findViewById(R.id.rightBtn_body).setOnClickListener(iconPickerListener);
		
		
		findOthersClickedListener = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onFindOthersPressed();
			}
		};
		findViewById(R.id.findOthersBtn).setOnClickListener(findOthersClickedListener);
	}
	
	private void onFindOthersPressed()
	{
		Intent intent = new Intent(this, FindNearbyActivity.class);
		intent.putExtra(FindNearbyActivity.EXTRA_NAME, nameEditText.getText().toString());
		intent.putExtra(FindNearbyActivity.EXTRA_MESSAGE, messageEditText.getText().toString());
		intent.putExtra(FindNearbyActivity.EXTRA_COLOUR, colourPickerId);
		intent.putExtra(FindNearbyActivity.EXTRA_ICON, iconPickerId);
		
		startActivity(intent);
		
	}
	
	private void colourPickerScroll(int direction)
	{
		if (direction == RIGHT)
			colourPickerId = (colourPickerId + 1) % Profile.colourPickerArray.length;
		else
			colourPickerId = (--colourPickerId < 0)? Profile.colourPickerArray.length-1:colourPickerId;
		
		colourPickerView.setBackgroundColor(Profile.colourPickerArray[colourPickerId]);
	}
	
	private void iconPickerScroll(int direction)
	{
		if (direction == RIGHT)
			iconPickerId = (iconPickerId + 1) % Profile.iconPickerArray.length;
		else
			iconPickerId = (--iconPickerId < 0)? Profile.iconPickerArray.length-1:iconPickerId;
		
		iconPickerImageView.setImageResource(Profile.iconPickerArray[iconPickerId]);
	}
	
}
