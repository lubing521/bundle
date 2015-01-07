package com.chinatelecom.smartgateway.guogee;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.*;


public class Protocol
{
	private boolean m_RemoteFlag;
	private int m_CtrlIP;
	private short m_CtrlPort;
	private int m_DataIP;
	private short m_DataPort;
	private byte[] m_GatewayMac;
	private ThreadKeepAlive m_ThreadAlive;
	private boolean m_ThreadAliveFlag;
	
	private volatile List<SmartNode> nodeStatusList;
	private Iterator<SmartNode> m_NodeIterator;
	
	private static Protocol instance; 
	private Protocol()
	{
		m_RemoteFlag = false;
		setCtrlIP(0);
		setCtrlPort((short) 0);
		setDataIP(0);
		setDataPort((short) 0);
		m_GatewayMac = null;
		m_ThreadAlive = null;
		m_ThreadAliveFlag = false;

		nodeStatusList = new ArrayList<SmartNode>();
		m_NodeIterator = nodeStatusList.iterator();
	}
	public static Protocol getInstance()
	{
		if (instance == null) { 
			instance = new Protocol();
		}
		return instance;  
	}
	
	public synchronized boolean start(BundleContext Context)
	{
		Util.UtilPrintln("protocol start() begin");
		boolean Flag = true;
		
		Util.UtilPrintln("protocol start() end : " + Flag);
		return Flag;
	}
	
	public synchronized boolean startThread()
	{
		Util.UtilPrintln("protocol startThread() begin");
		boolean Flag = true;
		m_ThreadAliveFlag = true;
		m_ThreadAlive = new ThreadKeepAlive("keepalive");
		m_ThreadAlive.start();
		Util.UtilPrintln("protocol startThread() end : " + Flag);
		return Flag;
	}
	
	public synchronized void stop()
	{
		Util.UtilPrintln("protocol stop() begin");
		ClearGatewarMac();
		m_ThreadAliveFlag = false;
		m_ThreadAlive.interrupt();
		m_ThreadAlive = null;
		Util.UtilPrintln("protocol stop() end");
	}
	
	/*启动线程：心跳线程*/
	class ThreadKeepAlive extends Thread
	{
		ThreadKeepAlive(String name){
	        super(name);//调用父类带参数的构造方法
	    }
	    public void run(){
	        //Util.UtilPrintln(" is saled by "+Thread.currentThread().getName());
	        while (m_ThreadAliveFlag)
	        {
	        	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	
	        	if (!m_ThreadAliveFlag || 1 >= SerialComm.getInstance().getStateMachine())
	        	{
	        		m_GatewayMac = null;
	        		m_RemoteFlag = false;
	        	
	        		continue;
	        	}
	        	
	        	if (m_ThreadAliveFlag && null == m_GatewayMac && 2 <= SerialComm.getInstance().getStateMachine())
	        	{
	        		ISmartFrame GetGateWayFrame = PackGetGatewayMac();
	        		SerialComm.getInstance().write(GetGateWayFrame.GetStrData(), GetGateWayFrame.GetSize());
	        		
	        		continue;
	        	}
	        	Util.UtilPrintln("keep alive GatewayMac success");
	        	while (m_ThreadAliveFlag && null != m_GatewayMac)
	        	{
	        		if (!m_RemoteFlag)
	        		{
	        			ISmartFrame GetRemoveSerFrame = PackGetRemoveSer();
	        			
	        			boolean flag = true;
	        			InetAddress address = null;
						try {
							address = InetAddress.getByName("regsrv1.guogee.com");
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							flag = false;
						} 
						if (flag)
						{
		        			DatagramPacket SendPacket = new DatagramPacket(GetRemoveSerFrame.GetStrData(),
		        					GetRemoveSerFrame.GetSize(),
		        					address,
		        					3001);
		        			NetComm.getInstance().send(SendPacket);
						}
	        		}
	        		else
	        		{
	        			Util.UtilPrintln("Remote server success");
	        			boolean flag = true;
	        			ISmartFrame KeepAliveFrame = PackKeepAlive();
	        			DatagramPacket SendPacket = null;
						try {
							SendPacket = new DatagramPacket(KeepAliveFrame.GetStrData(), 
									KeepAliveFrame.GetSize(),
									InetAddress.getByAddress( Util.Int2Byte(getCtrlIP()) ),
									getCtrlPort());
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							flag = false;
						}
						if (flag)
						{
							NetComm.getInstance().send(SendPacket);
						}
	        		}
	        		try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					senRequestOfPoint();
	        	}
	        }
	        Util.UtilPrintln("thread KeepAlive exit");
	    }
	}
	
	public synchronized void ClearGatewarMac()
	{
		m_GatewayMac = null;
		ClearRemoteSer();
	}
	
	public void ClearRemoteSer()
	{
		m_RemoteFlag = false;
		setCtrlIP(0);
		setCtrlPort((short) 0);
		setDataIP(0);
		setDataPort((short) 0);
	}
	
	public synchronized byte[] GetGatewayMac()
	{
		return m_GatewayMac;
	}
	
	public void DealNetFrame(ISmartFrame NetFrame)
	{
		if (!NetFrame.CheckError())
		{
			return;
		}
		Util.UtilPrintln("dev:" + NetFrame.GetDev() + " ver:" + NetFrame.GetVer() + " fun:" +  (NetFrame.GetFun() & 0xff));
		if (FilterKeepAliveRet(NetFrame))
		{
			return;
		}
		else if (FilterCtlServerGet(NetFrame))
		{
			return;
		}
		else if (FilterQueryNode(NetFrame))
		{
			return;
		}

//		byte[] tmp = NetFrame.GetStrData();
//		String StrHex = " ";
//		for (int i = 0; i < NetFrame.GetSize(); i++)
//		{
//			StrHex += Integer.toHexString(tmp[i] & 0xff);
//			StrHex += " ";
//		}
//		Util.UtilPrintln(StrHex);

		
		
		SerialComm.getInstance().write(NetFrame.GetStrData(), NetFrame.GetSize());
		return;
	}
	
	public void DealSerialFrame(ISmartFrame SerialFrame)
	{
		Util.UtilPrintln("DealSerialFrame");
		if (!SerialFrame.CheckError())
		{
			return;
		}
		if (FilterGatewayMac(SerialFrame))
		{
			return;
		}
		
		//更新节点列表
		addQueryRequest(SerialFrame);
		
		if (0 == SerialFrame.GetIP())
		{
			Util.UtilPrintln("SerialFrame.GetIP :" + SerialFrame.GetIP());
			return;
		}
		try {
//			Util.UtilPrintln("SendPacket begin");
			Util.UtilPrintln("IP:" + Util.longToIP(SerialFrame.GetIP()));
			Util.UtilPrintln("Port:" + SerialFrame.GetPort());
			if (0x01020304 == SerialFrame.GetIP())
			{
				//1.2.3.4，该IP为查询节点预设IP，不处理
				return;
			}
			DatagramPacket SendPacket = new DatagramPacket(SerialFrame.GetStrData(),
					SerialFrame.GetSize(),
					InetAddress.getByAddress( Util.Int2Byte(SerialFrame.GetIP()) ),
					SerialFrame.GetPort());
//			Util.UtilPrintln("SendPacket OK");
			NetComm.getInstance().send(SendPacket);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public boolean FilterCtlServerGet(ISmartFrame NetFrame)
	{
		if (0x00 != (NetFrame.GetDev() & 0xff) || 0x02 != (NetFrame.GetFun() & 0xff))
		{
			return false;
		}
		byte[] NetFrameData = NetFrame.GetData();
		setCtrlIP(Util.Byte2Int(NetFrameData, 0));
		setCtrlPort(Util.Byte2Short(NetFrameData, 4));
		setDataIP(Util.Byte2Int(NetFrameData, 6));
		setDataPort(Util.Byte2Short(NetFrameData, 10));
		m_RemoteFlag = true;
		Util.UtilPrintln("filte get romote server ");
		return true;
	}
	
	public boolean FilterKeepAliveRet(ISmartFrame NetFrame)
	{
		if (0x00 != (NetFrame.GetDev() & 0xff) || 0x04 != (NetFrame.GetFun() & 0xff))
		{
			return false;
		}
		Util.UtilPrintln("filte keep alive ret");
		return true;
	}
	
	public boolean FilterQueryNode(ISmartFrame NetFrame)
	{
		//如果指令为查询所有节点状态
		//LZP
		if((NetFrame.GetDev() == SmartNode.PROTOCOL_TYPE_GATEWAY 
				|| NetFrame.GetDev() == SmartNode.PROTOCOL_TYPE_GATEWAY_SECOND)
				&& NetFrame.GetFun() == 0x08)
		{
			//若是返回节点状态包		
			byte[] queryByte = new byte[46 + 400];
			//将数据重新封装
			byte[] getSourceByte = NetFrame.GetStrData();
			System.arraycopy(getSourceByte, 0, queryByte, 0, 2);//头	
//					byte[] queryByteLength = Util.Short2Byte((short)(0x2E + length*5));
			byte[] queryByteLength = Util.Short2Byte((short)(46 + 400));
			queryByte[2] = queryByteLength[0];//长度
			queryByte[3] = queryByteLength[1];//长度
			System.arraycopy(NetFrame.GetSourceMac(), 0, queryByte, 4, NetFrame.GetSourceMac().length);//源Mac
//					System.arraycopy(new byte[]{0x0,0x12,0x4B,0x0,0x3,(byte)0x9F,(byte)0xBE,(byte)0xC9}, 0, queryByte, 12, 8);//目标Mac
			System.arraycopy(getSourceByte, 20, queryByte, 20, 21);//从网络源IP复制到版本号位置
			queryByte[41] = 0x09;//返回指令
			byte[] dataLength = Util.Short2Byte((short)(400));//CRC
			queryByte[42] = dataLength[0];
			queryByte[43] = dataLength[1];
			System.arraycopy(getSourceByte, 44, queryByte, 44, 2);
//			List<SmartNode>  nodeStatusList = SerialComm.getInstance().getNodeStatusList();
			int length = nodeStatusList.size();
			for (int i = 0; i < length; i++) {
				queryByte[46 + i*5] = nodeStatusList.get(i).getShortMac()[0];
				queryByte[46 + i*5 + 1] = nodeStatusList.get(i).getShortMac()[1];
				queryByte[46 + i*5 + 2] = nodeStatusList.get(i).getType();
				queryByte[46 + i*5 + 3] = nodeStatusList.get(i).getStatus();
				long time = (int)((System.currentTimeMillis() - nodeStatusList.get(i).getTime())/1000);
				if ((time & 0xffffff) > 255)
				{
					time = 255;
				}
				queryByte[46 + i*5 + 4] = (byte)time;
				Util.UtilPrintln("curtime: " + System.currentTimeMillis());
				Util.UtilPrintln("time: " 
						+ Integer.toHexString(queryByte[46 + i*5] & 0xff)
						+ " "
						+ Integer.toHexString(queryByte[46 + i*5 + 1] & 0xff)
						+ " "
						+ nodeStatusList.get(i).getTime());
			}
//					if(length > 0)
//						System.arraycopy(data, 0, queryByte, 46, length * 5);
//					String sss = "";
//					for (int i = 0; i < queryByte.length; i++) {
//						sss += Integer.toHexString(queryByte[i] & 0xff) + " ";
//					}
			ISmartFrame QueryNodeRet = new ISmartFrame(queryByte);
			Util.UtilPrintln("IP:" + Util.longToIP(QueryNodeRet.GetIP()));
			Util.UtilPrintln("Port:" + QueryNodeRet.GetPort());
			DatagramPacket SendPacket;
			try {
				SendPacket = new DatagramPacket(QueryNodeRet.GetStrData(), 
						46 + 400,
						InetAddress.getByAddress( Util.Int2Byte(QueryNodeRet.GetIP()) ),
						QueryNodeRet.GetPort());
				NetComm.getInstance().send(SendPacket);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return true;
		}
		return false;
	}
	
	public boolean FilterGatewayMac(ISmartFrame NetFrame)
	{
		if (0x00 != (NetFrame.GetDev() & 0xff) || 0xfe != (NetFrame.GetFun() & 0xff))
		{
			return false;
		}
		if (null != m_GatewayMac)
		{
			return false;
		}
		else
		{
			m_GatewayMac = new byte[8];
			System.arraycopy(NetFrame.GetSourceMac(), 0, m_GatewayMac, 0, 8);
		}
		
		String StrHexRead = " ";
		for (int i = 0; i < 8; i++)
		{
			StrHexRead += Integer.toHexString(m_GatewayMac[i] & 0xff);
			StrHexRead += " ";
		}
		Util.UtilPrintln("GateMac : " + StrHexRead);
		
		Util.UtilPrintln("filte gatemac success");
		return true;
	}
	
	public ISmartFrame PackGetGatewayMac()
	{
		ISmartFrame GetGatewayMac = new ISmartFrame();
		GetGatewayMac.FillAA55();
		GetGatewayMac.SetFrameLength((short) 46);
		GetGatewayMac.SetDev((byte) 0x00);
		GetGatewayMac.SetVer((byte) 0x01);
		GetGatewayMac.SetFun((byte) 0xff);
		GetGatewayMac.FillCRC();
		
		Util.UtilPrintln("pack getgatemac");
		return GetGatewayMac;
	}
	
	public ISmartFrame PackKeepAlive()
	{
		ISmartFrame KeepAlive = new ISmartFrame();
		KeepAlive.FillAA55();
		KeepAlive.SetFrameLength((short) 46);
		if (null != m_GatewayMac)
		{
			KeepAlive.SetSourceMac(m_GatewayMac);
		}
		KeepAlive.SetDev((byte) 0x00);
		KeepAlive.SetVer((byte) 0x00);
		KeepAlive.SetFun((byte) 0x03);
		KeepAlive.FillCRC();
		
		return KeepAlive;
	}
	public ISmartFrame PackGetRemoveSer()
	{
		ISmartFrame GetRemoveSer = new ISmartFrame();
		GetRemoveSer.FillAA55();
		GetRemoveSer.SetFrameLength((short) 46);
		if (null != m_GatewayMac)
		{
			GetRemoveSer.SetSourceMac(m_GatewayMac);
		}
		GetRemoveSer.SetDev((byte) 0x00);
		GetRemoveSer.SetVer((byte) 0x01);
		GetRemoveSer.SetFun((byte) 0x01);
		GetRemoveSer.FillCRC();
		
		return GetRemoveSer;
	}
	
	public ISmartFrame PackQueryNode(SmartNode node)
	{
		ISmartFrame QueryNodeFrame = new ISmartFrame();
		QueryNodeFrame.FillAA55();
		QueryNodeFrame.SetFrameLength((short) 46);
		QueryNodeFrame.SetDev(node.getType());
		QueryNodeFrame.SetVer((byte) 1);
		QueryNodeFrame.SetFun((byte) 0xFF);
		if (null != m_GatewayMac)
		{
			QueryNodeFrame.SetSourceMac(m_GatewayMac);
		}

		QueryNodeFrame.SetTargetMac(node.getMac());
		QueryNodeFrame.FillIP(0x01020304);
		QueryNodeFrame.FillCRC();
		return QueryNodeFrame;
	}

	private void senRequestOfPoint()
	{
		if (m_NodeIterator.hasNext())
		{
			SmartNode node = (SmartNode) m_NodeIterator.next();
			ISmartFrame QueryNodeFrame = PackQueryNode(node);
			SerialComm.getInstance().write(QueryNodeFrame.GetStrData(), QueryNodeFrame.GetSize());
		}
		else
		{
			m_NodeIterator = nodeStatusList.iterator();
		}

		return;
	}
	
	public int getCtrlIP() {
		return m_CtrlIP;
	}
	public void setCtrlIP(int ctrlIP) {
		m_CtrlIP = ctrlIP;
	}
	public int getCtrlPort() {
		return (m_CtrlPort & 0x0000ffff);
	}
	public void setCtrlPort(short ctrlPort) {
		m_CtrlPort = ctrlPort;
	}
	public int getDataIP() {
		return m_DataIP;
	}
	public void setDataIP(int dataIP) {
		m_DataIP = dataIP;
	}
	public int getDataPort() {
		return (m_DataPort & 0x0000ffff);
	}
	public void setDataPort(short dataPort) {
		m_DataPort = dataPort;
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

