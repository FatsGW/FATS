package com.android.fatsgw.fats.dataholders;

/**
 * @author GW
 *
 */
public class DataPack {
	private PasserBy _passerby;
	private RegisteredApp _app;
	
	private String _timeStamp = null;

	private int _packNumber = 1; //1 based. used internally
	private int _packMaxNumber = 1; //1 based. used internally

	public static final int APP_IDENTIFIER_LIMIT = 13; //bytes
	public static final int APP_DATA_LIMIT = 1170; //bytes
	public static final int VERSION_LIMIT = 255; 
	private static final int PACK_MAX_NUMBER_LIMIT = 15; 


	/** Create a Data Pack for sending data.
	 * @param appIdentifier Identifier of the app sending the data.
	 * @param appData App data to send.
	 * @param packVersion data version of data to be sent.
	 * @return a Data Pack with values initialized.
	 */
	public static DataPack MakeSendingDataPack(String appData, int packVersion)
	{
		//check for invalid data
		if (packVersion < 0 || packVersion > VERSION_LIMIT)
			throw new IllegalArgumentException("DataPack.MakeSendingDataPack: Invalid Pack Version");
		if (appData == null || appData.length() == 0 || appData.length() > APP_DATA_LIMIT)
			throw new IllegalArgumentException("DataPack.MakeSendingDataPack: Invalid App Data");

		DataPack dp = new DataPack();
		dp.setAppData(appData);
		dp.setVersion(packVersion);

		return dp;
	}
	
	
	public static DataPack MakeSendingDataPack(RegisteredApp app)
	{
		if (app == null)
			throw new IllegalArgumentException("DataPack.MakeSendingDatapack: RegisteredApp cannot be null");
		
		DataPack dp = new DataPack();
		dp.setAppData(app.getData());
		dp.setAppIdentifier(app.getIdentifier());
		dp.setVersion(app.getVersion());
		
		return dp;
	}


	/** Create a Data Pack from data received.
	 * @param appIdentifier Identifier of the app sending the data.
	 * @param srcMac Mac address of the source device.
	 * @param srcDeviceName Device name of the source device.
	 * @param dataString String which contains the data, time stamp, pack number, max pack number.
	 * @return a Data Pack with values initialized.
	 */
	public static DataPack MakeReceivingDataPack(long appIdentifier, String srcMac, String srcDeviceName, String dataString, String timeStamp)
	{
		if (srcMac == null)
			throw new IllegalArgumentException("DataPack.MakeReceivingDataPack: Invalid Src Mac");
		if (srcDeviceName == null)
			throw new IllegalArgumentException("DataPack.MakeReceivingDataPack: Invalid Src Device Name");
		if (dataString == null || dataString.length() < 3) // at least 3 bytes (time stamp, pack marker, data)
			throw new IllegalArgumentException("DataPack.MakeReceivingDataPack: Invalid Data String");
		if (timeStamp == null)
			throw new IllegalArgumentException("DataPack.MakeReceivingDataPack: Invalid Time Stamp");

		DataPack dp = new DataPack();
		dp.setAppIdentifier(appIdentifier);
		dp.setSrcMac(srcMac);
		dp.setSrcDeviceName(srcDeviceName);
		dp.setTimeStamp(timeStamp);
		dp.extractAndStoreInfo(dataString);
		
		return dp;
	}

	
	/** Create a Data Pack from data obtained from database.
	 * @param appIdentifier Identifier of the app sending the data.
	 * @param srcMac Mac address of the source device.
	 * @param srcDeviceName Device name of the source device.
	 * @param data App data .
	 * @return a Data Pack with values initialized.
	 */
	public static DataPack MakeRetrievedDataPack(long appIdentifier, String srcMac, String srcDeviceName, String data, String timeStamp)
	{
		if (srcMac == null)
			throw new IllegalArgumentException("DataPack.MakeRetrievedDataPack: Invalid Src Mac");
		if (srcDeviceName == null)
			throw new IllegalArgumentException("DataPack.MakeRetrievedDataPack: Invalid Src Device Name");
		if (data == null)
			throw new IllegalArgumentException("DataPack.MakeRetrievedDataPack: Invalid Data String");
		if (timeStamp == null)
			throw new IllegalArgumentException("DataPack.MakeRetrievedDataPack: Invalid Time Stamp");
		
		DataPack dp = new DataPack();
		dp.setAppIdentifier(appIdentifier);
		dp.setSrcMac(srcMac);
		dp.setSrcDeviceName(srcDeviceName);
		dp.setAppData(data);
		dp.setTimeStamp(timeStamp);

		return dp;
	}


	/** Breaks the Data Pack into many Data Packs depending on the app data size limit provided.
	 * @param packToSplit
	 * @param appDataSizeLimit Must be > 0
	 * @return Array of Data Packs with app data size <= the limit provided. Can only split up to 15 packs. 
	 */
	public static DataPack[] SplitAppData(DataPack packToSplit, int appDataSizeLimit)
	{
		//check for invalid data
		if (packToSplit == null)
			throw new IllegalArgumentException("DataPack.SplitAppData: Invalid Pack to Split");
		if (appDataSizeLimit <= 0)
			throw new IllegalArgumentException("DataPack.SplitAppData: Invalid App Data Size Limit");

		String packAppData = packToSplit.getAppData();

		//app data within size, splitting not required
		if (packAppData.length() <= appDataSizeLimit)
			return new DataPack[]{packToSplit};

		//calculate the number of packs to split into
		int numberOfPacksToSplitInto = packAppData.length() / appDataSizeLimit;
		if (packAppData.length() % appDataSizeLimit != 0)
			numberOfPacksToSplitInto++;

		//number of packs exceed splitting limit
		if (numberOfPacksToSplitInto > PACK_MAX_NUMBER_LIMIT)
			return null;

		//split data into smaller packs
		DataPack[] smallerPacks = new DataPack[numberOfPacksToSplitInto];
		for (int i = 0; i < numberOfPacksToSplitInto; i++)
		{
			DataPack smallPack = new DataPack(packToSplit);

			//set pack numbers
			smallPack.setPackMaxNumber(numberOfPacksToSplitInto);
			smallPack.setPackNumber(i+1); //1 based

			//split data
			int startIndex = i * appDataSizeLimit;
			if (i == numberOfPacksToSplitInto - 1) //last pack
				smallPack.setAppData(packAppData.substring(startIndex));
			else
				smallPack.setAppData(packAppData.substring(startIndex, startIndex + appDataSizeLimit));

			//store in return array
			smallerPacks[i] = smallPack;
		}

		return smallerPacks;
	}


	/** Merge the app data of the Data Packs into 1 Data Pack.
	 * @param smallPacks 
	 * @return Returns a Data Pack with the merged app data. Other data will be taken from the first Data Pack in the array.
	 */
	public static DataPack MergeDataPacks(DataPack[] smallPacks)
	{
		if (smallPacks == null || smallPacks.length == 0)
			throw new IllegalArgumentException("DataPack.MakeReceivingDataPack: Invalid Packs to Merge");

		//merge the app data
		StringBuilder mergedAppData = new StringBuilder();
		for (int i = 0; i < smallPacks.length; i++)
			mergedAppData.append(smallPacks[i].getAppData());

		//create the merged pack
		DataPack mergedPack = new DataPack(smallPacks[0]);
		mergedPack.setPackMaxNumber(1);
		mergedPack.setPackNumber(1);
		mergedPack.setAppData(mergedAppData.toString());

		return mergedPack;
	}


	/** Copy constructor.
	 * @param target
	 */
	private DataPack(DataPack target)
	{
		_passerby = new PasserBy(target._passerby);
		_app = new RegisteredApp(target._app);
		_packNumber = target._packNumber;
		_packMaxNumber = target._packMaxNumber;
		_timeStamp = target._timeStamp;
	}

	
	/** Default constructor
	 *  
	 */
	private DataPack()
	{
		_app = new RegisteredApp();
		_passerby = new PasserBy();
	}

	
	/** Extract information from the String and stores them into respective variables.
	 * @param dataString String which contains the data, time stamp, pack number, max pack number.
	 */
	private void extractAndStoreInfo(String dataString)
	{
		//extract pack version
		_app.setVersion((int)dataString.charAt(0));

		//extract pack marker (pack number + max pack number)
		int packMarker = dataString.charAt(1);
		_packNumber = packMarker >> 4; //truncate out least significant 4 bits
		_packMaxNumber = (packMarker << 28) >> 28; //truncate out most significant 4 bits

		//extract data
		_app.setData(dataString.substring(2));
	}


	/** Get the Data String of this DataPack.
	 * @return String which contains time stamp, pack marker and data.
	 */
	public String getSendableDataString()
	{
		//compact all the info into 1 String
		StringBuilder dataString = new StringBuilder();

		//add time stamp (1 byte)
		dataString.append((char) _app.getVersion());

		//add pack marker (1 byte)
		int packMarker = 0;
		packMarker = _packNumber << 4;
		packMarker += _packMaxNumber;
		dataString.append((char)packMarker);

		//add data (up to 90 bytes)
		dataString.append(_app.getData());

		return dataString.toString();
	}


	public long getAppIdentifier()
	{
		return _app.getIdentifier();
	}


	public String getSrcMac()
	{
		return _passerby.getMacAddress();
	}


	public String getSrcDeviceName()
	{
		return _passerby.getDeviceName();
	}


	public String getTimeStamp()
	{
		return _timeStamp;
	}


	public String getAppData()
	{
		return _app.getData();
	}


	public int getPackVersion()
	{
		return _app.getVersion();
	}


	public int getPackNumber()
	{
		return _packNumber;
	}


	public int getPackMaxNumber()
	{
		return _packMaxNumber;
	}


	public void setAppIdentifier(long appIdentifier)
	{
		_app.setIdentifier(appIdentifier);
	}


	public void setSrcMac(String srcMac)
	{
		_passerby.setMacAddress(srcMac);
	}


	public void setSrcDeviceName(String srcDeviceName)
	{
		_passerby.setDeviceName(srcDeviceName);
	}


	public void setTimeStamp(String timeStamp)
	{
		this._timeStamp = timeStamp;
	}
	
	
	private void setAppData(String appData)
	{
		_app.setData(appData);
	}


	private void setPackNumber(int packNumber)
	{
		this._packNumber = packNumber;
	}


	private void setPackMaxNumber(int packMaxNumber)
	{
		this._packMaxNumber = packMaxNumber;
	}

	
	private void setVersion(int version)
	{
		_app.setVersion(version);
	}

	
	@Override
	public String toString()
	{
		return "App Identifier:" + _app.getIdentifier() + "\n" +
				"Source MAC:" + _passerby.getMacAddress() + "\n" +
				"Source Device Name:" + _passerby.getDeviceName() + "\n" + 
				"Time Stamp:" + _timeStamp + "\n" +
				"Pack Number:" + _packNumber + "/" + _packMaxNumber + "\n" +
				"App Data:" + _app.getData();
	}

}
