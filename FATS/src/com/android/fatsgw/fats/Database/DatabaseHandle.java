package com.android.fatsgw.fats.database;

import android.database.sqlite.SQLiteDatabase;

import com.android.fatsgw.fats.FatsApp;

public final class DatabaseHandle
{
	//can only have 1 instance of Helper to prevent database access errors from multiple threads
	private static DatabaseHelper _INSTANCE = null;
	
	private DatabaseHandle()
	{
	}

	public static synchronized SQLiteDatabase getWritableInstance()
	{
		if (_INSTANCE == null)
			initializeDatabaseConnection();
		
		return _INSTANCE.getWritableDatabase();
	}
	
	private static synchronized void initializeDatabaseConnection()
	{
		//get context from Application since multiple background services will be accessing the database constantly.
		//as such, Database will automatically be closed if Application (and all its services) terminates.
		if (_INSTANCE == null)
			_INSTANCE = new DatabaseHelper(FatsApp.getContext());
	}
	

}
