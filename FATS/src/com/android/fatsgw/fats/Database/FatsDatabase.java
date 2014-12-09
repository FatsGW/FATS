package com.android.fatsgw.fats.database;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;
import android.util.Log;

import com.android.fatsgw.fats.FatsApp;
import com.android.fatsgw.fats.MainActivity;
import com.android.fatsgw.fats.database.DatabaseContract.PasserByRecordsDb;
import com.android.fatsgw.fats.database.DatabaseContract.ReceivedDataDb;
import com.android.fatsgw.fats.database.DatabaseContract.RegisteredAppsDb;
import com.android.fatsgw.fats.dataholders.DataPack;
import com.android.fatsgw.fats.dataholders.PasserBy;
import com.android.fatsgw.fats.dataholders.RegisteredApp;

public final class FatsDatabase 
{
	public static final String DATABASE_EVENT_CHANGED = "com.android.fatsgw.fats.database.DATABASE_CHANGED";
	public static final String DATABASE_EVENT_REGISTERED_APP_CHANGED = "com.android.fatsgw.fats.database.REGISTERED_APP_CHANGED"; //when list of app changes (does not include data update)
	public static final String DATABASE_EVENT_REGISTERED_DATA_CHANGED = "com.android.fatsgw.fats.database.REGISTERED_DATA_CHANGED";
	public static final String DATABASE_EVENT_PASSERBY_CHANGED = "com.android.fatsgw.fats.database.PASSERBY_CHANGED";
	public static final String DATABASE_EVENT_RECEIVED_DATA_CHANGED = "com.android.fatsgw.fats.database.RECEIVED_DATA_CHANGED";
	
	
	
	private static final long PASSERBY_TIME_LIMIT = 120000;//120000; //2 mins
	private SQLiteDatabase _db;

	public FatsDatabase()
	{
		super();
		_db = DatabaseHandle.getWritableInstance();
		_db.setForeignKeyConstraintsEnabled(true); //handles constraints and cascading
	}


	public void close()
	{
		_db.close();
		_db = null;
	}

	
	public void insertPack(DataPack packToAdd)
	{
		insertAndUpdatePasserBy(packToAdd, PASSERBY_TIME_LIMIT);
		
		//checks if data received belongs to an app already registered
		String packageName = getRegisteredAppPackageName(packToAdd.getAppIdentifier());
		if (packageName == null) // app do not exist
			return;
		else
		{
			//insert and broadcast message if successfully changed
			if (insertAndStackReceivedData(packToAdd))
				broadcastDatabaseChanged(DATABASE_EVENT_RECEIVED_DATA_CHANGED);
		}
	}

	/** Inserts the DataPack into the database. 
	 * If data from the same app and source already exists, it will be added ONLY IF the versions are different.
	 * Can add up to 256 different versions. 
	 * @param packToAdd
	 * @return returns true if new entry added. false if conflicted entry exists
	 */
	private boolean insertAndStackReceivedData(DataPack packToAdd)
	{
		//insert received data into database. since constraints in place, will automatically not be added if conflict in versions
		ContentValues values = new ContentValues();
		values.put(ReceivedDataDb.COLUMN_NAME_APP_ID, packToAdd.getAppIdentifier());
		values.put(ReceivedDataDb.COLUMN_NAME_PASSERBY_ID, packToAdd.getSrcMac());
		values.put(ReceivedDataDb.COLUMN_NAME_DATA, packToAdd.getAppData());
		values.put(ReceivedDataDb.COLUMN_NAME_DATA_VERSION, packToAdd.getPackVersion());
		values.put(ReceivedDataDb.COLUMN_NAME_TIME_RECEIVED, packToAdd.getTimeStamp());

		try
		{
			_db.insertOrThrow(ReceivedDataDb.TABLE_NAME, null, values);
			return true;
		}
		catch (SQLException e)
		{
			//conflict exists. data with same source and version exists.
			e.printStackTrace();
			return false;
		}
	}


	/** Inserts a passerby record into the database if it does not exist.
	 * If it already exists, it will increment the times met counter, and update the device name if necessary
	 * @param packtoAdd
	 * @param timeLimit Time difference threshold in which to increase the times met counter
	 */
	private void insertAndUpdatePasserBy(DataPack packToAdd, long timeLimit) 
	{
		boolean broadcastEvent = false;
		//check if passerby exists
		String[] columns = new String[]{PasserByRecordsDb.COLUMN_NAME_TIMES_MET, PasserByRecordsDb.COLUMN_NAME_TIME_STAMP};
		String selection = PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID + " = '" + packToAdd.getSrcMac() + "'";

		Cursor cursor = _db.query(PasserByRecordsDb.TABLE_NAME, columns, selection, null, null, null, null);
		if (cursor != null && cursor.moveToFirst())
		{
			//PASSERBY EXISTS
			long currentTime = System.currentTimeMillis();
			if (currentTime - cursor.getLong(cursor.getColumnIndex(PasserByRecordsDb.COLUMN_NAME_TIME_STAMP))  > timeLimit)
			{
				//update times met counter if threshold exceeded
				ContentValues values = new ContentValues();
				values.put(PasserByRecordsDb.COLUMN_NAME_TIMES_MET, cursor.getInt(0) + 1);
				values.put(PasserByRecordsDb.COLUMN_NAME_DEVICE_NAME, packToAdd.getSrcDeviceName());
				values.put(PasserByRecordsDb.COLUMN_NAME_TIME_STAMP, currentTime);
				_db.update(PasserByRecordsDb.TABLE_NAME, values, selection, null);
				
				broadcastEvent = true;
			}
		}
		else
		{
			//PASSERBY DO NOT EXIST

			//add new entry to the passerby database
			ContentValues values = new ContentValues();
			values.put(PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID, packToAdd.getSrcMac());
			values.put(PasserByRecordsDb.COLUMN_NAME_DEVICE_NAME, packToAdd.getSrcDeviceName());
			values.put(PasserByRecordsDb.COLUMN_NAME_TIMES_MET, 1);
			values.put(PasserByRecordsDb.COLUMN_NAME_TIME_STAMP, System.currentTimeMillis());
			_db.insert(PasserByRecordsDb.TABLE_NAME, null, values);
			
			broadcastEvent = true;
		}
		
		if (broadcastEvent)
			broadcastDatabaseChanged(DATABASE_EVENT_PASSERBY_CHANGED);
	}


	/** Insert a new registered app into the database.
	 * @param appId
	 * @param appPackageName
	 * @param appData
	 * @return Returns true if succeeded and false if an error occurred
	 */
	public boolean insertRegisteredApp(long appId, String appPackageName, String appName, String appData)
	{
		//register the app into the database
		ContentValues values = new ContentValues();
		values.put(RegisteredAppsDb.COLUMN_NAME_APP_ID, appId);
		values.put(RegisteredAppsDb.COLUMN_NAME_APP_NAME,  appName);
		values.put(RegisteredAppsDb.COLUMN_NAME_PACK_NAME, appPackageName);
		values.put(RegisteredAppsDb.COLUMN_NAME_DATA, appData);
		values.put(RegisteredAppsDb.COLUMN_NAME_DATA_VERSION, 0);

		long id = _db.insert(RegisteredAppsDb.TABLE_NAME, null, values);
		if (id != -1)
		{
			broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_APP_CHANGED);
			return true;
		}
		else
			return false;
	}
	
	
	public boolean updateRegisteredApp(long appId, String appData)
	{
		String selection = RegisteredAppsDb.COLUMN_NAME_APP_ID + "= '" + appId + "'";
		Cursor cursor = _db.query(RegisteredAppsDb.TABLE_NAME, null, selection, null, null, null, null);
		if (cursor.moveToFirst())
		{
			ContentValues values = new ContentValues();
			values.put(RegisteredAppsDb.COLUMN_NAME_DATA, appData);
			int dataVersion = cursor.getInt(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_DATA_VERSION)) + 1;
			dataVersion %= DataPack.VERSION_LIMIT;
			values.put(RegisteredAppsDb.COLUMN_NAME_DATA_VERSION, dataVersion);
			_db.update(RegisteredAppsDb.TABLE_NAME, values, selection, null);
			
			broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_DATA_CHANGED);
			return true;
		}
		else
			return false;
	}


	/** Gets all the received data of the specified app
	 * @param appIdentifier
	 * @return an array of Data Packs. Returns a 0 array if no data packs found
	 */
	public DataPack[] getAppReceviedData(long appIdentifier)
	{
		/*
		 * SELECT	PasserbyRecordsDb.PASSERBY_ID,
		 * 			PasserByRecordsDb.DEVICE_NAME,
		 * 			ReceivedDataDb.DATA,
		 * 			ReceivedDataDb.TIME_RECEIVED
		 * FROM		PasserbyRecordsDb,
		 * 			ReceivedDataDb
		 * WHERE	ReceivedDataDb.PASSERBY_ID = PasserbyRecordsDb.PASSERBY_ID
		 * AND		ReceivedDataDb.APP_ID = appIdentifier
		 * ORDER BY ReceivedDataDb.TIME_RECEIVED ASC;
		 * 
		 */

		//get all data received from other sources for the specified app
		String passerByDeviceName = PasserByRecordsDb.TABLE_NAME + "." + PasserByRecordsDb.COLUMN_NAME_DEVICE_NAME;
		String passerById = PasserByRecordsDb.TABLE_NAME + "." + PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID;
		String receivedDataData = ReceivedDataDb.TABLE_NAME + "." + ReceivedDataDb.COLUMN_NAME_DATA;
		String receivedDataTime = ReceivedDataDb.TABLE_NAME + "." + ReceivedDataDb.COLUMN_NAME_TIME_RECEIVED;
		String receivedDataPasserById = ReceivedDataDb.TABLE_NAME + "." + ReceivedDataDb.COLUMN_NAME_PASSERBY_ID;
		String receivedDataAppId = ReceivedDataDb.TABLE_NAME + "." + ReceivedDataDb.COLUMN_NAME_APP_ID;

		final String query = "SELECT "	+ 	passerById 				+ "," +
				passerByDeviceName 		+ "," +
				receivedDataData 		+ "," + 
				receivedDataTime 		+
				" FROM "	+ 	PasserByRecordsDb.TABLE_NAME + "," +
				ReceivedDataDb.TABLE_NAME +
				" WHERE "	+ 	receivedDataPasserById + "=" + passerById +
				" AND "	+	receivedDataAppId + "=" + "'" + appIdentifier + "'" +
				" ORDER BY " + receivedDataTime + " ASC";
		Cursor cursor = _db.rawQuery(query, null);
		cursor.moveToFirst();

		//convert database data into DataPacks
		DataPack[] receivedData = new DataPack[cursor.getCount()];
		for (int i = 0; i < receivedData.length; i++)
		{
			receivedData[i] = DataPack.MakeRetrievedDataPack(appIdentifier, cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
			cursor.moveToNext();
		}
		cursor.close();
		cursor = null;

		return receivedData;
	}

	
	public Cursor getAllAppReceivedDataCursor()
	{
		//force return hidden _id
		/*
		 * select	ReceivedDataDb.rowid _id, 
		 * 			ReceivedDataDb.data,
		 * 			ReceivedDataDb.dataVersion
		 * 			ReceivedDataDb.timeReceived
		 * 			RegisteredAppsDb.appname
		 * 			PasserByRecordsDb.devicename
		 * from		ReceivedDataDb,
		 * 			PasserbyRecordsDb,
		 * 			RegisteredAppsDb
		 * where	ReceivedDataDb.passerbyid = PasserbyRecordsDb.id
		 * and		ReceivedDataDb.appid = RegisteredAppsDb.id
		 * order by	ReceivedDataDb.timereceived DESC
		 * 			
		 */
		
		String query = 
				"SELECT rd.rowid AS _id," +
						"rd." + ReceivedDataDb.COLUMN_NAME_DATA + "," +
						"rd." + ReceivedDataDb.COLUMN_NAME_DATA_VERSION + "," +
						"rd." + ReceivedDataDb.COLUMN_NAME_TIME_RECEIVED + "," +
						"ra." + RegisteredAppsDb.COLUMN_NAME_APP_NAME + "," +
						"pb." + PasserByRecordsDb.COLUMN_NAME_DEVICE_NAME + " " +
				"FROM " + ReceivedDataDb.TABLE_NAME +" AS rd," +
						RegisteredAppsDb.TABLE_NAME + " AS ra," +
						PasserByRecordsDb.TABLE_NAME + " AS pb " +
				"WHERE rd." + ReceivedDataDb.COLUMN_NAME_PASSERBY_ID + " = pb." + PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID + " " +
				"AND rd." + ReceivedDataDb.COLUMN_NAME_APP_ID + " = ra." + RegisteredAppsDb.COLUMN_NAME_APP_ID + " " +
				"ORDER BY rd." + ReceivedDataDb.COLUMN_NAME_TIME_RECEIVED + " DESC";
		
		
		Cursor c =  _db.rawQuery(query, null);
		String[] names = c.getColumnNames();
		for (String name : names)
			System.out.println("column name:" + name);
		return c;
	}
	
	/** Gets the package name of the registered app.
	 * @param appIdentifier
	 * @return Returns the package name of the app. Returns null if app specified does not exist
	 */
	public String getRegisteredAppPackageName(long appIdentifier)
	{
		//get only the package name column
		Cursor cursor = _db.query(RegisteredAppsDb.TABLE_NAME, 
				new String[]{RegisteredAppsDb.COLUMN_NAME_PACK_NAME},
				RegisteredAppsDb.COLUMN_NAME_APP_ID + "= '" + appIdentifier + "'", 
				null, null, null, null);

		//result holder
		String data = null;

		//if there is result
		if (cursor.moveToFirst())
			data = cursor.getString(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_PACK_NAME));

		cursor.close();
		return data;
	}


	/** Get the data registered by the specified app.
	 * @param appIdentifier
	 * @return Returns the data registered. Returns null if app specified does not exist
	 */
	public String getRegisteredAppData(long appIdentifier)
	{
		//get only the registered data column
		Cursor cursor = _db.query(RegisteredAppsDb.TABLE_NAME, 
				new String[]{RegisteredAppsDb.COLUMN_NAME_DATA}, 
				RegisteredAppsDb.COLUMN_NAME_APP_ID + "= '" + appIdentifier + "'", 
				null, null, null, null);

		//result holder
		String data = null;

		//if there is a result
		if (cursor.moveToFirst())
			data = cursor.getString(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_DATA));

		cursor.close();
		return data;
	}


	/** Gets the app id of the registered app via package name.
	 * @param appPackageName
	 * @return Returns the app id. Returns null if specified app does not exist.
	 */
	public String getRegisteredAppId(String appPackageName)
	{
		//get only the app id column
		Cursor cursor = _db.query(RegisteredAppsDb.TABLE_NAME, 
				new String[]{RegisteredAppsDb.COLUMN_NAME_APP_ID},
				RegisteredAppsDb.COLUMN_NAME_PACK_NAME + "= '" + appPackageName + "'", 
				null, null, null, null);

		//result holder
		String data = null;

		//if there is result
		if (cursor.moveToFirst())
			data = cursor.getString(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_APP_ID));

		cursor.close();
		return data;
	}


	/** Gets all app id of registered apps.
	 * @return returns a String[] of all registered apps. Returns 0 array if no apps are registered.
	 */
	public long[] getAllRegisteredAppId()
	{
		//get all registered app ids
		Cursor cursor = _db.query(RegisteredAppsDb.TABLE_NAME, 
				new String[]{RegisteredAppsDb.COLUMN_NAME_APP_ID}, 
				null, null, null, null, null);
		cursor.moveToFirst();

		//result holder
		long[] data = new long[cursor.getCount()];
		int columnIndex = cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_APP_ID);
		for (int i = 0; i < data.length; i++)
		{
			data[i] = cursor.getLong(columnIndex);
			cursor.moveToNext();
		}
		cursor.close();
		return data;
	}


	public RegisteredApp getRegisteredApp(long appIdentifier)
	{
		String selection = RegisteredAppsDb.COLUMN_NAME_APP_ID + " = '" + appIdentifier + "'";
		Cursor cursor = _db.query(RegisteredAppsDb.TABLE_NAME, null, selection, null, null, null, null);
		if (cursor.moveToFirst())
		{
			return new RegisteredApp(cursor.getLong(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_APP_ID)), 
						cursor.getString(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_DATA)), 
						cursor.getString(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_PACK_NAME)), 
						cursor.getString(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_APP_NAME)), 
						cursor.getInt(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_DATA_VERSION)));
		}
		else
			return null;
	}
	
	
	/** Gets all registered apps and the corresponding data
	 * @return an array of RegisteredApp. Returns a 0 array if no apps are registered
	 */
	public RegisteredApp[] getAllRegisteredApp()
	{
		Cursor cursor = getAllRegisteredAppCursor();
		if (cursor.moveToFirst())
		{
			//loop through all registered apps
			RegisteredApp[] apps = new RegisteredApp[cursor.getCount()];
			for (int i = 0; i < apps.length; i++)
			{
				apps[i] = new RegisteredApp(cursor.getLong(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_APP_ID)), 
						cursor.getString(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_DATA)), 
						cursor.getString(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_PACK_NAME)), 
						cursor.getString(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_APP_NAME)), 
						cursor.getInt(cursor.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_DATA_VERSION)));
				cursor.moveToNext();
			}
			cursor.close();
			return apps;
		}
		else
		{
			cursor.close();
			return new RegisteredApp[0];
		}
	}
	
	
	public Cursor getAllRegisteredAppCursor()
	{
		return _db.query(RegisteredAppsDb.TABLE_NAME, null, null, null, null, null, null);
	}


	/** Gets all recorded passerbys and the corresponding data
	 * @return an array of PasserBy. Returns a 0 array if no passerbys are recorded
	 */
	public PasserBy[] getAllPasserByRecords()
	{
		Cursor cursor = getAllPasserByRecordsCursor();
		if (cursor.moveToFirst())
		{
			//loop through all passerbys
			PasserBy[] passerbys = new PasserBy[cursor.getCount()];
			for (int i = 0; i < passerbys.length; i++)
			{
				passerbys[i] = new PasserBy(cursor.getString(cursor.getColumnIndex(PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID)), 
						cursor.getString(cursor.getColumnIndex(PasserByRecordsDb.COLUMN_NAME_DEVICE_NAME)), 
						cursor.getInt(cursor.getColumnIndex(PasserByRecordsDb.COLUMN_NAME_TIMES_MET)),
						cursor.getLong(cursor.getColumnIndex(PasserByRecordsDb.COLUMN_NAME_TIME_STAMP)));
				cursor.moveToNext();
			}
			cursor.close();
			return passerbys;
		}
		else
		{
			//no recorded passerbys
			cursor.close();
			return new PasserBy[0];
		}
	}
	
	
	public Cursor getAllPasserByRecordsCursor()
	{
		return _db.query(PasserByRecordsDb.TABLE_NAME, null, null, null, null, null, null);
	}


	/** Clears the received data for the app without deleting the tables.
	 * @param appIdentifier
	 */
	public void clearAppReceivedData(long appIdentifier)
	{
		_db.delete(ReceivedDataDb.TABLE_NAME, 
				ReceivedDataDb.COLUMN_NAME_APP_ID + "= '" + appIdentifier + "'", 
				null);
		broadcastDatabaseChanged(DATABASE_EVENT_RECEIVED_DATA_CHANGED);
	}


	/** Clears all received data without deleting the table
	 * 
	 */
	public void clearAllAppReceivedData()
	{
		_db.delete(ReceivedDataDb.TABLE_NAME, null, null);
		broadcastDatabaseChanged(DATABASE_EVENT_RECEIVED_DATA_CHANGED);
	}


	/** Clears the registered app and all received data for that app without deleting the tables.
	 * @param appIdentifier
	 * @return true if app exists and is deleted.
	 */
	public boolean clearRegisteredApp(long appIdentifier)
	{
		//clears registered data + received data of specified app without deleting the tables
		this.clearAppReceivedData(appIdentifier); //handled by cascading, but just in case
		int rowsDeleted = _db.delete(RegisteredAppsDb.TABLE_NAME, 
				RegisteredAppsDb.COLUMN_NAME_APP_ID + "= '" + appIdentifier + "'", 
				null);		
		
		if (rowsDeleted == 0)
			return false;
		else
		{
			broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_APP_CHANGED);
			broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_DATA_CHANGED);
			broadcastDatabaseChanged(DATABASE_EVENT_RECEIVED_DATA_CHANGED);
			return true;
		}
	}


	/** Clears all registered apps and all received data without deleting the tables. 
	 * 
	 */
	public void clearAllRegisteredApp()
	{
		//clears all registered data + all received data without deleting the tables
		_db.delete(ReceivedDataDb.TABLE_NAME, null, null); //handled by cascading, but just in case
		_db.delete(RegisteredAppsDb.TABLE_NAME, null, null);
		
		broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_APP_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_DATA_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_RECEIVED_DATA_CHANGED);
	}

	
	/** Clears the recorded passerby and all received data from that passerby without deleting the tables
	 * @param passerById
	 */
	public void clearPasserBy(String passerById)
	{
		//clears recorded passerby + received data of specified passerby without deleting the tables\
		//received data automatically delete due to cascading.
		_db.delete(PasserByRecordsDb.TABLE_NAME,
					PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID + " = '" + passerById + "'",
					null);
		
		broadcastDatabaseChanged(DATABASE_EVENT_PASSERBY_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_RECEIVED_DATA_CHANGED);
	}
	

	/** Clears all passerbys recorded and all received data from those passerby without deleting the tables
	 * 
	 */
	public void clearAllPasserBy()
	{
		_db.delete(PasserByRecordsDb.TABLE_NAME, null, null); //cascades down and deletes the received data automatically
		
		broadcastDatabaseChanged(DATABASE_EVENT_PASSERBY_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_RECEIVED_DATA_CHANGED);
	}


	/** Clears all registered apps, all receieved data and all passerby records without deleting the tables.
	 * 
	 */
	public void clearEntireDatabase()
	{
		//clears entire database without deleting the tables(registered apps, passerby records, received data)
		_db.delete(ReceivedDataDb.TABLE_NAME, null, null); //handled by cascading, but just in case
		_db.delete(PasserByRecordsDb.TABLE_NAME, null, null);
		_db.delete(RegisteredAppsDb.TABLE_NAME, null, null);

		broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_APP_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_DATA_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_PASSERBY_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_RECEIVED_DATA_CHANGED);
	}


	//TEST FUNCTIONS
	public void testInsertData()
	{
		//REGISTERED APPS DATA
		try
		{

			for (int i = 0; i < 10; i++)
			{
				ContentValues registeredApps = new ContentValues();
				registeredApps.put(RegisteredAppsDb.COLUMN_NAME_APP_ID, "0000000" + i);
				registeredApps.put(RegisteredAppsDb.COLUMN_NAME_PACK_NAME, "com.test.app.version" + i);
				registeredApps.put(RegisteredAppsDb.COLUMN_NAME_APP_NAME, "Version " + i);
				registeredApps.put(RegisteredAppsDb.COLUMN_NAME_DATA, "this is some data for this app " + i);
				registeredApps.put(RegisteredAppsDb.COLUMN_NAME_DATA_VERSION, i);
				_db.insert(RegisteredAppsDb.TABLE_NAME, null, registeredApps);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


		//PASSERBY DATA
		try
		{
			for (int i = 0; i < 20; i++)
			{
				ContentValues passerby = new ContentValues();
				passerby.put(PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID, "AB:AB:AB:AB:AB:" + i);
				passerby.put(PasserByRecordsDb.COLUMN_NAME_DEVICE_NAME, "Passerby " + i);
				passerby.put(PasserByRecordsDb.COLUMN_NAME_TIMES_MET, i);
				passerby.put(PasserByRecordsDb.COLUMN_NAME_TIME_STAMP, System.currentTimeMillis());
				_db.insert(PasserByRecordsDb.TABLE_NAME, null, passerby);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


		//DATA RECIEVED 
		try
		{
			for (int i = 0; i < 10; i++)
			{
				ContentValues data = new ContentValues();
				data.put(ReceivedDataDb.COLUMN_NAME_APP_ID, "0000000" + (int)(i/2));
				data.put(ReceivedDataDb.COLUMN_NAME_PASSERBY_ID, "AB:AB:AB:AB:AB:" + i);
				data.put(ReceivedDataDb.COLUMN_NAME_DATA, "this is some received data " + i);
				data.put(ReceivedDataDb.COLUMN_NAME_DATA_VERSION, i);
				Time now = new Time();
				now.setToNow();
				data.put(ReceivedDataDb.COLUMN_NAME_TIME_RECEIVED, now.format("%Y/%m/%d %H:%M:%S"));
				_db.insert(ReceivedDataDb.TABLE_NAME, null, data);
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


		Log.d(MainActivity.DEBUG_TAG, "Test Data Added");

		broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_APP_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_REGISTERED_DATA_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_PASSERBY_CHANGED);
		broadcastDatabaseChanged(DATABASE_EVENT_RECEIVED_DATA_CHANGED);
	}


	public void testPrintAllData()
	{
		//REGISTERED APPS DATA
		Cursor c1 = _db.query(RegisteredAppsDb.TABLE_NAME, null, null, null, null, null, null);
		if (c1 == null)
		{
			Log.e(MainActivity.DEBUG_TAG, "error retrieving registered apps");
			return;
		}
		Log.d(MainActivity.DEBUG_TAG, "========== Registered Apps Data ==========");
		c1.moveToFirst();
		while(!c1.isAfterLast())
		{
			Log.d(MainActivity.DEBUG_TAG, "id:" + c1.getString(c1.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_APP_ID)) + "\n" +
					"pack:" + c1.getString(c1.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_PACK_NAME)) + "\n" +
					"name:" + c1.getString(c1.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_APP_NAME)) + "\n" +
					"data:" + c1.getString(c1.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_DATA)) + "\n" + 
					"version:" + c1.getString(c1.getColumnIndex(RegisteredAppsDb.COLUMN_NAME_DATA_VERSION)));
			c1.moveToNext();
		}
		c1.close();
		c1 = null;


		//PASSERBY DATA
		Cursor c2 = _db.query(PasserByRecordsDb.TABLE_NAME, null, null, null, null, null, null);
		if (c2 == null)
		{
			Log.e(MainActivity.DEBUG_TAG, "error retrieving passerby data");
			return;
		}
		Log.d(MainActivity.DEBUG_TAG, "========== Passerby Data ==========");
		c2.moveToFirst();
		while(!c2.isAfterLast())
		{
			Log.d(MainActivity.DEBUG_TAG, "id:" + c2.getString(c2.getColumnIndex(PasserByRecordsDb.COLUMN_NAME_PASSERBY_ID)) + "\n" +
					"name:" + c2.getString(c2.getColumnIndex(PasserByRecordsDb.COLUMN_NAME_DEVICE_NAME)) + "\n" +
					"times met:" + c2.getString(c2.getColumnIndex(PasserByRecordsDb.COLUMN_NAME_TIMES_MET)) + "\n" +
					"timestamp:" + c2.getLong(c2.getColumnIndex(PasserByRecordsDb.COLUMN_NAME_TIME_STAMP)));
			c2.moveToNext();
		}
		c2.close();
		c2 = null;


		//RECEIVED DATA
		Cursor c3 = _db.query(ReceivedDataDb.TABLE_NAME, null, null, null, null, null, null);
		if (c3 == null)
		{
			Log.e(MainActivity.DEBUG_TAG, "error retrieving received data");
			return;
		}
		Log.d(MainActivity.DEBUG_TAG, "========== Received Data ==========");
		c3.moveToFirst();
		while(!c3.isAfterLast())
		{
			Log.d(MainActivity.DEBUG_TAG, "passerby id:" + c3.getString(c3.getColumnIndex(ReceivedDataDb.COLUMN_NAME_PASSERBY_ID)) + "\n" + 
					"app id:" + c3.getString(c3.getColumnIndex(ReceivedDataDb.COLUMN_NAME_APP_ID)) + "\n" + 
					"version:" + c3.getString(c3.getColumnIndex(ReceivedDataDb.COLUMN_NAME_DATA_VERSION)) + "\n" + 
					"data:" + c3.getString(c3.getColumnIndex(ReceivedDataDb.COLUMN_NAME_DATA)) + "\n" +
					"time:" + c3.getString(c3.getColumnIndex(ReceivedDataDb.COLUMN_NAME_TIME_RECEIVED)));
			c3.moveToNext();
		}
		c3.close();
		c3 = null;
		
	}
	
	
	private void broadcastDatabaseChanged(String intentAction)
	{
		FatsApp.getLocalBroadcastManager().sendBroadcast(new Intent(intentAction));
	}

}
