package com.zero.agent;
import android.accessibilityservice.AccessibilityService; import android.view.accessibility.AccessibilityEvent; import android.view.accessibility.AccessibilityNodeInfo; import java.util.List;
public class AccessibilityHelper extends AccessibilityService {
    public static AccessibilityHelper instance;
    public void onServiceConnected() { instance = this; }
    public void onAccessibilityEvent(AccessibilityEvent event) {}
    public void onInterrupt() {}
    public void onDestroy() { instance = null; super.onDestroy(); }
    public void clickText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow(); if (root == null) return;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo n : nodes) { AccessibilityNodeInfo parent = n; while (parent != null) { if (parent.isClickable()) { parent.performAction(AccessibilityNodeInfo.ACTION_CLICK); return; } parent = parent.getParent(); } }
    }
}