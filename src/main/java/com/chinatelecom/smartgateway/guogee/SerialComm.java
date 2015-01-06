package com.chinatelecom.smartgateway.guogee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.*;

import com.chinatelecom.smartgateway.deviceservice.usbservice.CtUsbService;

public class SerialComm
{
	private int m_StateMachine;
	private BundleContext m_Context;
	private CtUsbService m_UsbService;
	private static SerialComm instance;  
	private int m_fd;
	private int m_devId;
	private m_ThreadRead m_ThreadRead;
	private boolean  m_ThreadReadFlag;
	@SuppressWarnings("unused")
	private GuogeeSetMsgConfig m_GuogeeMsg;
	private long m_lTimeWrite;
	
	private volatile List<SmartNode> nodeStatusList;

	private SerialComm()
	{
		m_StateMachine = 0; /*0未注册,1已注册，2收到USb消息，3锁定,状态机切换不能越级：1不能直接切换到3*/
		m_Context = null;
		m_UsbService = null;
		m_fd = 0;
		m_devId = 0;
		m_ThreadRead = null;
		m_ThreadReadFlag = false;
		m_GuogeeMsg = null;
		m_lTimeWrite = 0;
		nodeStatusList = new ArrayList<SmartNode>();
	}
	public static SerialComm getInstance()
	{
		if (instance == null) { 
			instance = new SerialComm();
		}
		return instance;
	}

	
	public synchronized boolean start(BundleContext Context)
	{
		Util.UtilPrintln("serialcomm start() begin");
		boolean Flag = true;
		m_Context = Context;
		// 同维要求GuogeeSetMsgConfig必须在usbRegister注册之前
		m_GuogeeMsg = new GuogeeSetMsgConfig(m_Context); /*创建一个实例，接收USB消息*/
		ServiceReference ref = m_Context.getServiceReference(CtUsbService.class.getName());
		m_UsbService = (CtUsbService) m_Context.getService(ref);
		RegisterUSB();
		Util.UtilPrintln("serialcomm start() end : " + Flag);
		return Flag;
	}
	
	public synchronized boolean startThread()
	{
		Util.UtilPrintln("serialcomm startThread() begin");
		boolean Flag = true;
		m_ThreadReadFlag = true;
		m_ThreadRead = new m_ThreadRead("read");
		m_ThreadRead.start();
		Util.UtilPrintln("serialcomm startThread() end : " + Flag);
		return Flag;
	}
	
	public synchronized void stop()
	{
		Util.UtilPrintln("serialcomm stop() begin");
		UnLockUSB();
		m_ThreadReadFlag = false;
		m_ThreadRead.interrupt();
		m_ThreadRead.interrupt();
		m_ThreadRead.interrupt();
		m_ThreadRead.interrupt();
		m_ThreadRead = null;
		UnRegisterUSB();
		m_GuogeeMsg = null;  /*不再引用，释放对象*/
		m_Context = null;
		
		Util.UtilPrintln("serialcomm stop() end");
	}
	
	/*不能设置synchronized，否则状态机无法从2到达3*/
	public void write(byte[] StrData, int Len)
	{
		if (1 >= m_StateMachine)
		{
			return;
		}
		if (0 < m_fd)
		{
			String StrHex = " ";
			for (int i = 0; i < Len; i++)
			{
				StrHex += Integer.toHexString(StrData[i] & 0xff);
				StrHex += " ";
			}
			Util.UtilPrintln("write " + Len + " : " +  StrHex);
			long lTmpTime = System.currentTimeMillis() - m_lTimeWrite;
			if (lTmpTime < 30)
			{
				//两次write必须间隔30ms
				try {
					Thread.sleep(30 - lTmpTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			String strWrite = m_UsbService.usbWrite(m_fd, StrData, 0, Len);
			m_lTimeWrite = System.currentTimeMillis();
			Util.UtilPrintln("usbWrite : " + strWrite);

//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			byte[] readbuff = new byte[546];
//			byte[] readdata = new byte[546];
//			Util.UtilPrintln("read begin!!!!");
//        	String strRead = m_UsbService.usbRead(m_fd, readbuff, 0, 4, -1);
//        	Util.UtilPrintln("usbRead: " + strRead);
//        	
//        	int nLen = (readbuff[2] & 0xff) << 8 | readbuff[3] & 0xff;
//            Util.UtilPrintln("nLen:" + nLen);
//            String strReaddata = m_UsbService.usbRead(m_fd, readdata, 0, nLen - 4, -1);
//            Util.UtilPrintln("usbRead: " + strReaddata);
//            System.arraycopy(readdata, 0, readbuff, 4, nLen - 4);
//        	
//        	String StrHexRead = " ";
//			for (int i = 0; i < 46; i++)
//			{
//				StrHexRead += Integer.toHexString(readbuff[i] & 0xff);
//				StrHexRead += " ";
//			}
//			Util.UtilPrintln(StrHexRead);
//			
//			ISmartFrame ReadFrame = new ISmartFrame(readbuff);
//            Protocol.getInstance().DealSerialFrame(ReadFrame);
		}
	}
	
	/*状态机0-1*/
	public void RegisterUSB()
	{
		String strReg = m_UsbService.usbRegister(m_Context, 0x1);
		Util.UtilPrintln("usbRegister : " + strReg);
		m_StateMachine = 1;
	}
	
	/*状态机1-0*/
	public void UnRegisterUSB()
	{
		String strUnReg = m_UsbService.usbUnregister(m_Context);
		Util.UtilPrintln("usbUnregister : " + strUnReg);
		m_StateMachine = 0;
	}
	
	/*状态机2-3*/
	public void LockUSB()
	{
		m_StateMachine = 3;
		String strUnlock = m_UsbService.usbLock(m_devId);
		Util.UtilPrintln("usbLock : " + strUnlock);
	}
	
	/*状态机1/2/3-1*/
	public void UnLockUSB()
	{
		m_StateMachine = 1;
		if (0 != m_fd)
		{
			Util.UtilPrintln("usbClose begin");
			String StrClose = m_UsbService.usbClose(m_fd);
			Util.UtilPrintln("usbClose : " + StrClose);
			m_fd = 0;
		}
		if (0 != m_devId)
		{
			Util.UtilPrintln("usbUnlock begin");
			String strLock = m_UsbService.usbUnlock(m_devId);
			Util.UtilPrintln("usbUnlock : " + strLock);
			m_devId = 0;
		}
		Protocol.getInstance().ClearGatewarMac();
		
		return;
	}
	
	/*状态机2/3-1*/
	public synchronized void PullUSB(int devId)
	{
		Util.UtilPrintln("devId : " + devId + " m_devId : " + m_devId);
		if (m_devId != devId || 0 == devId)
		{
			return;
		}

		UnLockUSB();
		m_ThreadRead.interrupt();
		m_ThreadRead.interrupt();
		m_ThreadRead.interrupt();
		m_ThreadRead.interrupt();
		return;
	}
	
	/*状态机1-2-1/3*/
	public synchronized void InsertUSB(int devId)
	{
		Util.UtilPrintln("devId : " + devId + " m_devId : " + m_devId);
		boolean Flag = true;
		if (null != Protocol.getInstance().GetGatewayMac())
		{
			/*已经有一个网关正在使用*/
			Util.UtilPrintln("GatewayMac ： " + Protocol.getInstance().GetGatewayMac());
			String strUnlock = m_UsbService.usbUnlock(devId);
			Util.UtilPrintln("usbUnlock : " + strUnlock);
			return;
		}
		
		m_devId = devId;

		Util.UtilPrintln("usbOpen begin");
		String StrOpen = m_UsbService.usbOpen(m_devId);
		Util.UtilPrintln("usbOpen" + StrOpen);
		
		JSONObject OpenJson = null;
		int OpenResult = 0;
		int fd = 0;
		try {
			OpenJson = new JSONObject(StrOpen);
			OpenResult = OpenJson.getInt("Result");
			fd = OpenJson.getInt("Handle");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Flag = false;
		}
		Util.UtilPrintln(Flag + " " + OpenResult + " " + fd);
		if (!Flag || 0 != OpenResult || 0 == fd)
		{
			UnLockUSB();
			return;
		}
		
		m_fd = fd;
		
//		// /usr/include/asm-generic/ioctls.h:#define FIONBIO		0x5421
//		//FIONBIO，第三个参数unsigned long,0阻塞,1非阻塞
//		byte[] IoBlock = new byte[4];
//		IoBlock[0] = 0;
//		IoBlock[1] = 0;
//		IoBlock[2] = 0;
//		IoBlock[3] = 1;
//		String StrIostl = m_UsbService.usbIoctl(m_fd, 0x5421, IoBlock);
//		Util.UtilPrintln("usbIoctl :" + StrIostl);
		
		Util.UtilPrintln("usbSetSerial begin");
		String StrSetSerial = m_UsbService.usbSetSerial(m_fd, 38400, 0, 8, 1, false, false);
		Util.UtilPrintln("usbSetSerial : " + StrSetSerial);
		
		int SetSerialResult = 0;
		JSONObject SetSerialJson = null;
		try {
			SetSerialJson = new JSONObject(StrSetSerial);
			SetSerialResult = SetSerialJson.getInt("Result");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Flag = false;
		}
		Util.UtilPrintln("SetSerialResult :" + SetSerialResult);
		if (!Flag || 0 != SetSerialResult)
		{
			Util.UtilPrintln("SetSerialResult Flag :" + Flag);
			UnLockUSB();
			return;
		}
		
		m_StateMachine = 2; /*状态不能设置太早，必须在设置串口参数成功之后*/
		boolean LockFlag = false;
		for (int i = 0; i < 10; i++)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (null != Protocol.getInstance().GetGatewayMac())
			{
				LockFlag = true;
				break;
			}
		}
		Util.UtilPrintln("10s 10s 10s 10s");
		
		if (LockFlag)
		{
			/*果谷设备，锁定*/
			LockUSB();
		}
		else
		{
			/*10s后仍然网关mac仍然没有获取成功，说明不是果谷设备*/
			UnLockUSB();
		}

	}
	
	/*不能设置synchronized，否则状态机无法从2到达3*/
	public int getStateMachine()
	{
		return m_StateMachine;
	}
	
	public boolean ReadChars(byte[] Byte, int offset, int len)
	{
		if (!m_ThreadReadFlag || 1 >= m_StateMachine || 0 == m_fd)
		{
			return false;
		}
	
		Util.UtilPrintln("usbRead in");
		String strRead = m_UsbService.usbRead(m_fd, Byte, offset, len, -1);
		Util.UtilPrintln("usbRead : " + strRead);
		
		int ReadResult = 0;
		JSONObject ReadJson = null;
		try {
			ReadJson = new JSONObject(strRead);
			ReadResult = ReadJson.getInt("Result");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (0 != ReadResult)
		{
			return false;
		}
		return true;
	}


	class m_ThreadRead extends Thread
	{
		m_ThreadRead(String name){
	        super(name);//调用父类带参数的构造方法
	    }
		int nWrite = 0;
	    public void run(){
	        //Util.UtilPrintln(" is saled by "+Thread.currentThread().getName());
	        while (m_ThreadReadFlag)
	        {
	        	if (1 >= m_StateMachine)
	        	{
	        		try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//	        		Util.UtilPrintln("m_StateMachine :" + m_StateMachine);
	        		continue;
	        	}

	        	Util.UtilPrintln("m_StateMachine :" + m_StateMachine);
	        	
				byte[] readbuff = new byte[546];
//				byte[] readdata = new byte[546];
				Util.UtilPrintln("read begin!!!!");
//	        	String strRead = m_UsbService.usbRead(m_fd, readbuff, 0, 4, -1);
//	        	Util.UtilPrintln("usbRead: " + strRead);
				if (!ReadChars(readbuff, 0, 4))
				{
					//读取错误，等等再读
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
	        	
	        	int nLen = (readbuff[2] & 0xff) << 8 | readbuff[3] & 0xff;
	        	if(0x00aa != (readbuff[0] & 0xff) || 0x55 != (readbuff[1] & 0xff) || nLen > 546)
	        	{
	        		Util.UtilPrintln(0xaa + " " + 0x55);
	        		Util.UtilPrintln("No Heda " + (readbuff[0] & 0xff) + " " + (readbuff[1] & 0xff) + " " + nLen);
	        		continue;
	        	}
	        	
	            Util.UtilPrintln("nLen:" + nLen);
//	            String strReaddata = m_UsbService.usbRead(m_fd, readdata, 0, nLen - 4, -1);
//	            Util.UtilPrintln("usbRead: " + strReaddata);
//	            System.arraycopy(readdata, 0, readbuff, 4, nLen - 4);
	            if (!ReadChars(readbuff, 4, nLen - 4))
	            {
					//读取错误，稍微等等再读
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
	        	
	            //打印读取到的消息
	        	String StrHexRead = " ";
				for (int i = 0; i < nLen; i++)
				{
					StrHexRead += Integer.toHexString(readbuff[i] & 0xff);
					StrHexRead += " ";
				}
				Util.UtilPrintln(StrHexRead);
				
				
				/*
	        	boolean readFlag = true;
	        	if (0 == nWrite)
	            {
	                //TRACE("all new\n");
	                readbuff[0] = 0;
	                ReadChar(readbuff, nWrite);
	                //TRACE("readbuff[0]:%u\n", (unsigned int)readbuff[0]);
	                //TRACE("%u\n", PACKAGE_HEDAH);
	                if (0xaa == readbuff[0])
	                {
	                	Util.UtilPrintln("aa");
	                    nWrite = 1;
	                }
	                else
	                {
	                    nWrite = 0;
	                    readFlag = false;
	                }
	            }
	        	if (!readFlag)
	        	{
	        		continue;
	        	}
	            if (1 == nWrite)
	            {
	                ReadChar(readbuff, nWrite);
	                if (0x55 == readbuff[1])
	                {
	                	Util.UtilPrintln("55");
	                    nWrite = 2;
	                }
	                else if (0xaa == readbuff[1])
	                {
	                    nWrite = 1;
	                    readFlag = false;
	                }
	                else
	                {
	                    nWrite = 0;
	                    readFlag = false;
	                }
	            }
	            if (!readFlag)
	        	{
	        		continue;
	        	}
	            ReadChar(readbuff, nWrite);
	            nWrite++;
	            ReadChar(readbuff, nWrite);
	            int nLen = (readbuff[2] & 0xff) << 8 | readbuff[3] & 0xff;
	            //TRACE("nLen:%d\n", nLen);
	            Util.UtilPrintln(nLen);
	            if (nLen >= 546)
	            {
	                nWrite = 0;
	                readFlag = false;
	            }
	            if (!readFlag)
	        	{
	        		continue;
	        	}

	            int i = 0;
	            for (i = 0; i < nLen - 4; i++)
	            {
	                ReadChar(readbuff, 4 + i);
	            }
	            
	            nWrite = 0;
	            */

	            Util.UtilPrintln("read a frame");
	            
	            if (m_ThreadReadFlag)
	            {
	            	ISmartFrame ReadFrame = new ISmartFrame(readbuff);
		            Protocol.getInstance().DealSerialFrame(ReadFrame);
	            }
	        }
	        Util.UtilPrintln("thread serialComm exit");
	    }
	}
	
	
	public List<SmartNode> getNodeStatusList()
	{
		return this.nodeStatusList;
	}
	// 查询某个节点状态,若原不存在，则插入List中，若存在则更新  LZP
	public void addQueryRequest(ISmartFrame frame) {// 地址2个字节 1个字节的类型 1个字节的状态
													// 1个字节的时间
		boolean hasFlag = true;
		SmartNode node = new SmartNode();
		switch (frame.GetDev()) {
		case SmartNode.PROTOCOL_TYPE_COLORLIGHT:
			SmartNode.GetItemFromColorLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_ONELIGNT:
			SmartNode.GetItemFromOneLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_TWOLIGNT:
			SmartNode.GetItemFromTwoLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_THREELIGNT:
			SmartNode.GetItemFromThreeLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_FOURLIGNT:
			SmartNode.GetItemFromFourLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_POWERSOCKET:
			SmartNode.GetItemFromColorLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_CONTROLSOCKET:
			SmartNode.GetItemFromControlSocket(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_GATEWAY:/* 网关不提取节点信息 */
			return;
		default:
			SmartNode.GetItemFromAny(frame, node);
			break;
		}
		if(nodeStatusList.size() > 0){
			for (SmartNode nodeTemp : nodeStatusList) {
				if(Arrays.equals(frame.GetSourceMac(), nodeTemp.getMac())){
					//若存在，更新状态与时间
					nodeTemp.setStatus(frame.GetData()[0]);
					nodeTemp.setTime(System.currentTimeMillis());
					hasFlag = false;
					break;
				}
			}
		}		
		if (hasFlag) {
			// Add into nodeStatusList
			nodeStatusList.add(node);
		}

	}
	
}