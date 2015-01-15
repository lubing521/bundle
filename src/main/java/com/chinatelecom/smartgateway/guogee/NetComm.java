package com.chinatelecom.smartgateway.guogee;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.osgi.framework.*;



public class NetComm
{
	private DatagramSocket m_socket;
	ThreadNetComm m_ThreadNC;
	private boolean m_ThreadNCFlag;
	
	private static NetComm instance;
	public static NetComm getInstance()
	{
		if (instance == null) {
			instance = new NetComm();
		}
		return instance;
	}
	private NetComm()
	{
		m_socket = null;
		m_ThreadNC = null;
		m_ThreadNCFlag = false;
	}
	
	public synchronized boolean start(BundleContext Context)
	{
		Util.UtilPrintln("netcomm start() begin");
		boolean Flag = true;
		try
		{
			m_socket = new DatagramSocket(3000);
		}catch(Exception e)
		{
			Util.UtilPrintln("can not listen to:"+e);
			Flag = false;
		}
		
		Util.UtilPrintln("netcomm start() end : " + Flag);

		return Flag;
	}
	
	public synchronized boolean startThread()
	{
		Util.UtilPrintln("netcomm startThread() begin");
		boolean Flag = true;
		m_ThreadNCFlag = true;
		m_ThreadNC = new ThreadNetComm("netcomm");
		m_ThreadNC.start();
		Util.UtilPrintln("netcomm startThread() end : " + Flag);
		return Flag;
	}
	
	public synchronized void stop()
	{
		Util.UtilPrintln("netcomm stop() begin");
		m_ThreadNCFlag = false;
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		
		if (null != m_socket)
		{
			m_socket.close();
			m_socket = null;
		}
		m_ThreadNC = null;
		Util.UtilPrintln("netcomm stop() end ");
	}
	
	public synchronized void send(DatagramPacket sendPacket)
	{
		Util.UtilPrintln("send");
		if (null == m_socket)
		{
			return;
		}
    	byte[] tmp = sendPacket.getData();
    	String StrHex = " ";
		for (int i = 0; i < sendPacket.getLength(); i++)
		{
			StrHex += Integer.toHexString(tmp[i] & 0xff);
			StrHex += " ";
		}
		Util.UtilPrintln(StrHex);
		try {
			m_socket.send(sendPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
//		Util.UtilPrintln("send");
	}
	
	class ThreadNetComm extends Thread
	{
		ThreadNetComm(String name){
	        super(name);//调用父类带参数的构造方法
	    }
		byte[] arb = new byte[1024];
		DatagramPacket RecvPacket = new DatagramPacket(arb, arb.length);
	    public void run(){
	        Util.UtilPrintln("thread netcomm start");
	        while (m_ThreadNCFlag)
	        {
	        	boolean Errflag = false;
	        	try {
	        		m_socket.receive(RecvPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Util.UtilPrintln("m_ThreadNCFlag:" + m_ThreadNCFlag);
					Errflag = true;
				}
	        	if (Errflag)
	        	{
	        		//异常退出，不处理，直接返回
	        		continue;
	        	}
	        	Util.UtilPrintln("recv");
	        	if (46 > RecvPacket.getLength())
	        	{
	        		continue;
	        	}
	        	
	        	ISmartFrame RecvFrame = new ISmartFrame(RecvPacket);
	        	RecvFrame.FillIP( Util.Byte2Int(RecvPacket.getAddress().getAddress(), 0) );
	        	RecvFrame.FillPort((short) RecvPacket.getPort());
	        	
	        	byte[] tmp = RecvPacket.getData();
	        	String StrHex = " ";
	    		for (int i = 0; i < RecvPacket.getLength(); i++)
	    		{
	    			StrHex += Integer.toHexString(tmp[i] & 0xff);
	    			StrHex += " ";
	    		}
	    		Util.UtilPrintln(StrHex);
	        	Protocol.getInstance().DealNetFrame(RecvFrame);
	        }
	        Util.UtilPrintln("thread netcomm exit");
	        
	    }
	    
	}

}