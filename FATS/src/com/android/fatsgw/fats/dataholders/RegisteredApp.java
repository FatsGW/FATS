package com.android.fatsgw.fats.dataholders;

public class RegisteredApp
{
	private long _identifier;
	private String _data;
	private String _packageName;
	private String _name;
	
	private int _version;
	
	public RegisteredApp(long identifier, String data, String packageName, String name, int version)
	{
		_identifier = identifier;
		_data = data;
		_packageName = packageName;
		_name = name;
		_version = version;
	}
	
	
	public RegisteredApp(long identifier, String data, int version)
	{
		_identifier = identifier;
		_data = data;
		_version = version;
		
		_packageName = null;
		_name = null;
	}
	
	
	public RegisteredApp(RegisteredApp target)
	{
		_identifier = target._identifier;
		_data = target._data;
		_packageName = target._packageName;
		_name = target._name;
		_version = target._version;
	}
	
	
	public RegisteredApp()
	{
		_identifier = 0;
		_data = null;
		_packageName = null;
		_name = null;
		_version = 0;
	}
	
	
	public void setIdentifier(long identifier)
	{
		_identifier = identifier;
	}
	
	
	public void setData(String data)
	{
		_data = data;
	}
	
	
	public void setPackageName(String packageName)
	{
		_packageName = packageName;
	}
	
	
	public void setName(String name)
	{
		_name = name;
	}
	
	
	public void setVersion(int version)
	{
		_version = version;
	}
	
	
	public long getIdentifier()
	{
		return _identifier;
	}
	
	
	public String getData()
	{
		return _data;
	}
	
	
	public String getPackageName()
	{
		return _packageName;
	}
	
	
	public String getName()
	{
		return _name;
	}
	
	
	public int getVersion()
	{
		return _version;
	}
}
