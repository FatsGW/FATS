package com.android.fatsgw.fats.database;

import com.android.fatsgw.fats.dataholders.DataPack;

import android.provider.BaseColumns;

public final class DatabaseContract
{
	private DatabaseContract()
	{
	}
	
	public static abstract class RegisteredAppsDb implements BaseColumns
	{
		public static final String TABLE_NAME = "registeredapps";
		public static final String COLUMN_NAME_APP_ID = "appid";
		public static final String COLUMN_NAME_PACK_NAME = "packagename";
		public static final String COLUMN_NAME_APP_NAME = "appname";
		public static final String COLUMN_NAME_DATA = "appdata";
		public static final String COLUMN_NAME_DATA_VERSION = "dataversion";
		
		public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
															_ID + " INTEGER PRIMARY KEY," +
															COLUMN_NAME_APP_ID + " INTEGER UNIQUE NOT NULL," + 
															COLUMN_NAME_PACK_NAME + " TEXT NOT NULL," + 
															COLUMN_NAME_APP_NAME + " TEXT NOT NULL," + 
															COLUMN_NAME_DATA + " VARCHAR(" + DataPack.APP_DATA_LIMIT + ") NOT NULL," +
															COLUMN_NAME_DATA_VERSION + " INTEGER NOT NULL" +
															")";
		
		public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;
	}
	
	public static abstract class PasserByRecordsDb implements BaseColumns
	{
		public static final String TABLE_NAME = "passerbyrecords";
		public static final String COLUMN_NAME_PASSERBY_ID = "macid";
		public static final String COLUMN_NAME_DEVICE_NAME = "devicename";
		public static final String COLUMN_NAME_TIMES_MET = "timesmet";
		public static final String COLUMN_NAME_TIME_STAMP = "timestamp";
		
		public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
															_ID + " INTEGER PRIMARY KEY, " +
															COLUMN_NAME_PASSERBY_ID + " TEXT UNIQUE NOT NULL," +
															COLUMN_NAME_DEVICE_NAME + " TEXT NOT NULL," +
															COLUMN_NAME_TIMES_MET + " INTEGER," +
															COLUMN_NAME_TIME_STAMP + " INTEGER" + 
															")";
		
		public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;
	}
	
	public static abstract class ReceivedDataDb
	{
		public static final String TABLE_NAME = "receiveddata";
		public static final String COLUMN_NAME_PASSERBY_ID = PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID;
		public static final String COLUMN_NAME_APP_ID = RegisteredAppsDb.COLUMN_NAME_APP_ID;
		public static final String COLUMN_NAME_DATA = RegisteredAppsDb.COLUMN_NAME_DATA;
		public static final String COLUMN_NAME_DATA_VERSION = "dataversion";
		public static final String COLUMN_NAME_TIME_RECEIVED = "timereceived";
		
		public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
															COLUMN_NAME_PASSERBY_ID + " TEXT NOT NULL," + 
															COLUMN_NAME_APP_ID + "  VARCHAR(" + DataPack.APP_IDENTIFIER_LIMIT + ") NOT NULL," +
															COLUMN_NAME_DATA + " VARCHAR(" + DataPack.APP_DATA_LIMIT + ") NOT NULL," + 
															COLUMN_NAME_DATA_VERSION + " INTEGER NOT NULL," +
															COLUMN_NAME_TIME_RECEIVED + " NOT NULL," +
															"FOREIGN KEY (" + COLUMN_NAME_PASSERBY_ID + ") REFERENCES " + PasserByRecordsDb.TABLE_NAME + "(" + PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID + ") ON DELETE CASCADE," +
															"FOREIGN KEY (" + COLUMN_NAME_APP_ID + ") REFERENCES " + RegisteredAppsDb.TABLE_NAME + "(" + RegisteredAppsDb.COLUMN_NAME_APP_ID + ") ON DELETE CASCADE," +
															"PRIMARY KEY (" + COLUMN_NAME_PASSERBY_ID + "," + COLUMN_NAME_APP_ID + "," + COLUMN_NAME_DATA_VERSION + ")" +
															")";
		
		public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;
	}
}


