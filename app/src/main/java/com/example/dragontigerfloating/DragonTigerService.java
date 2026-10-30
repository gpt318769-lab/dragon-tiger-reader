package com.example.dragontigerfloating;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

public class DragonTigerService extends AccessibilityService {
    private static final String TAG = "DragonTigerService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            Log.d(TAG, "Nababasa ang aktibong window sa screen.");
            rootNode.recycle();
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Na-interrupt ang service.");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Matagumpay na kumonekta ang Accessibility Service!");
    }
}
