
package com.chinatelecom.smartgateway.guogee;

import com.chinatelecom.smartgateway.mangement.AbsSetMsgConfig;

import org.json.*;
import org.osgi.framework.*;

public class GuogeeSetMsgConfig extends AbsSetMsgConfig
{
	public GuogeeSetMsgConfig (BundleContext context) {
		super(context);
	}
	public String SetMsgProcess (String msgContent) {
		
		Util.UtilPrintln("msgContent : " + msgContent);
		
		boolean Flag = true;
		JSONObject jsonObj = null;
		String Event = null;
		String DevType = null;
		String ActionType = null;
		int DevId = 0;
		try {
			jsonObj = new JSONObject(msgContent);
			Event = jsonObj.getString("Event");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Flag = false;
		}
		if (Flag)
		{
			Util.UtilPrintln("Event : " + Event);
			if (Event.equals("USB_DEV_ACTION"))
			{
				try {
					DevType = jsonObj.getString("DevType");
					ActionType = jsonObj.getString("ActionType");
					DevId = jsonObj.getInt("DevId");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Flag = false;
				}
			}
			else
			{
				Flag = false;
			}
		}
		
		if (Flag)
		{
			Util.UtilPrintln("Event : " + Event 
					+ " DevType : " + DevType 
					+ " ActionType : " + ActionType
					+ " DevId : " + DevId);
			if (Event.equals("USB_DEV_ACTION") && DevType.equals("USB_SERIAL"))
			{
				if (ActionType.equals("USB_DEV_INSERT"))
				{
					Util.UtilPrintln("InserUSB");
					SerialComm.getInstance().InsertUSB(DevId);
				}
				else if (ActionType.equals("USB_DEV_PULL"))
				{
					Util.UtilPrintln("PullUSB");
					SerialComm.getInstance().PullUSB(DevId);
				}
			}
		}

		String RetStr  = "{\"Result\":0}";
		Util.UtilPrintln("SetMsgProcess return:" + RetStr);
		return RetStr;
	}
}