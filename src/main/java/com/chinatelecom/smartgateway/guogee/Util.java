package com.chinatelecom.smartgateway.guogee;


public class Util
{
	public static int Byte2Int(byte[] ipbyte, int offset)
	{
		if (null == ipbyte)
		{
			return 0;
		}
		int ipint = (ipbyte[0 + offset] & 0xff) << 24 | (ipbyte[1 + offset] & 0xff) << 16 
				| (ipbyte[2 + offset] & 0xff) << 8 | ipbyte[3 + offset] & 0xff;
		return ipint;
	}
	
	public static byte[] Int2Byte(int ipint)
	{
		byte[] ipbyte = new byte[4];
		ipbyte[0] = (byte) ((ipint >> 24) & 0xff);
		ipbyte[1] = (byte) ((ipint >> 16) & 0xff);
		ipbyte[2] = (byte) ((ipint >> 8) & 0xff);
		ipbyte[3] = (byte) (ipint & 0xff);
		return ipbyte;
	}
	
	public static short Byte2Short(byte[] portbyte, int offset)
	{
		if (null == portbyte)
		{
			return 0;
		}
		short portshort = (short) ((portbyte[0 + offset] & 0xff) << 8 | portbyte[1 + offset] & 0xff);
		return portshort;
	}
	
	public static byte[] Short2Byte(short portshort)
	{
		byte[] portbyte = new byte[2];
		portbyte[0] = (byte) ((portshort >> 8) & 0xff);
		portbyte[1] = (byte) (portshort & 0xff);
		return portbyte;
	}
	
	public static void UtilPrintln(String strPrintln)
	{
//		System.out.println(strPrintln);
		return;
	}
	
	// 将127.0.0.1形式的IP地址转换成十进制整数，这里没有进行任何错误处理
	public static long ipToLong(String strIp) {
		long[] ip = new long[4];
		// 先找到IP地址字符串中.的位置
		int position1 = strIp.indexOf(".");
		int position2 = strIp.indexOf(".", position1 + 1);
		int position3 = strIp.indexOf(".", position2 + 1);
		// 将每个.之间的字符串转换成整型
		ip[0] = Long.parseLong(strIp.substring(0, position1));
		ip[1] = Long.parseLong(strIp.substring(position1 + 1, position2));
		ip[2] = Long.parseLong(strIp.substring(position2 + 1, position3));
		ip[3] = Long.parseLong(strIp.substring(position3 + 1));
		return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
	}

	// 将十进制整数形式转换成127.0.0.1形式的ip地址
	public static String longToIP(long longIp) {
		StringBuffer sb = new StringBuffer("");
		// 直接右移24位
		sb.append(String.valueOf((longIp >>> 24)));
		sb.append(".");
		// 将高8位置0，然后右移16位
		sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
		sb.append(".");
		// 将高16位置0，然后右移8位
		sb.append(String.valueOf((longIp & 0x0000FFFF) >>> 8));
		sb.append(".");
		// 将高24位置0
		sb.append(String.valueOf((longIp & 0x000000FF)));
		return sb.toString();
	}
}