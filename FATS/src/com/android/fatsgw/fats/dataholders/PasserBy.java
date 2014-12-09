package com.android.fatsgw.fats.dataholders;

public class PasserBy
{
	private String _macAddress;
	private String _deviceName;
	private int _timesMet;
	private long _timeStamp;
	
	public PasserBy(String mac, String deviceName, int timesMet, long timeStamp)
	{
		_macAddress = mac;
		_deviceName = deviceName;
		_timesMet = timesMet;
		_timeStamp = timeStamp;
	}
	
	
	public PasserBy(String mac, String deviceName)
	{
		_macAddress = mac;
		_deviceName = deviceName;
		_timesMet = 0;
	}
	
	
	public PasserBy(PasserBy target)
	{
		_macAddress = target._macAddress;
		_deviceName = target._deviceName;
		_timesMet = target._timesMet;
		_timeStamp = target._timeStamp;
	}
	
	
	public PasserBy()
	{
		_macAddress = null;
		_deviceName = null;
		_timesMet = 0;
		_timeStamp = 0;
	}
	
	
	public void setMacAddress(String macAddress)
	{
		_macAddress = macAddress;
	}
	
	
	public void setDeviceName(String deviceName)
	{
		_deviceName = deviceName;
	}
	
	
	public void setTimesMet(int timesMet)
	{
		_timesMet = timesMet;
	}
	

	public void setTimeStamp(long timeStamp)
	{
		_timeStamp = timeStamp;
	}
	
	
	public String getMacAddress()
	{
		return _macAddress;
	}
	
	
	public String getDeviceName()
	{
		return _deviceName;
	}
	
	
	public int getTimesMet()
	{
		return _timesMet;
	}
	
	public long getTimeStamp()
	{
		return _timeStamp;
	}
}
