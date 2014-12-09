package com.android.fatsgw.fats.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.fatsgw.fats.database.DatabaseContract.PasserByRecordsDb;
import com.android.fatsgw.fats.database.DatabaseContract.ReceivedDataDb;
import com.android.fatsgw.fats.database.DatabaseContract.RegisteredAppsDb;

public class DatabaseHelper extends SQLiteOpenHelper
{
	public static final int DATABASE_VERSION = 32;
	public static final String DATABASE_NAME = "DataExchange.db";
	
	public DatabaseHelper(Context ctx)
	{
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL(RegisteredAppsDb.SQL_CREATE_ENTRIES);
		db.execSQL(PasserByRecordsDb.SQL_CREATE_ENTRIES);
		db.execSQL(ReceivedDataDb.SQL_CREATE_ENTRIES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// TODO Proper upgrade functionalities
		
		//currently only dropping all tables and reinitializing
		db.execSQL(ReceivedDataDb.SQL_DELETE_ENTRIES);
		db.execSQL(PasserByRecordsDb.SQL_DELETE_ENTRIES);
		db.execSQL(RegisteredAppsDb.SQL_DELETE_ENTRIES);
	
		onCreate(db);
	}	
}
