package com.android.fatsgw.fats.utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class Converter
{
	private static int M_RADIX = 36;
	private Converter()
	{
		
	}
	
	public static long packageNameToId(String packageName)
	{
		//128 bit hash function
		HashFunction hashFunc = Hashing.murmur3_128();
		return hashFunc.hashBytes(packageName.getBytes()).asLong();
	}
	
	public static String longToSpecialString(long target)
	{
		//convert long to base 36 String
		StringBuilder specialString = new StringBuilder(Long.toString(target, M_RADIX));
		
		//remove negative sign
		if (specialString.charAt(0) == '-')
			specialString.delete(0, 1);
		
		//add 0s to pad to 13 characters
		if (specialString.length() < 13)
		{
			int diff = 13 - specialString.length();
			for (int i = 0; i < diff; i++)
				specialString.insert(0, "0");
		}
		
		//map negative numbers
		if (target < 0)
			specialString.replace(0, 1, Character.toString((char)(specialString.charAt(0) + 2)));
		
		return specialString.toString();
	}
	
	public static long specialStringToLong(String target)
	{	
		//check for negative numbers
		if (Integer.parseInt("" + target.charAt(0)) > 1)
		{
			StringBuilder sb = new StringBuilder(target);
			sb.replace(0, 1, Character.toString((char)(sb.charAt(0) - 2)));
			sb.insert(0, '-');
			target = sb.toString();
		}
		
		return Long.parseLong(target, M_RADIX);
	}
	
	

}
