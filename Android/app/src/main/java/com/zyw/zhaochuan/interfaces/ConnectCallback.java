package com.zyw.zhaochuan.interfaces;

import android.net.wifi.WifiManager;

/**
 * Created by zyw on 2016/8/23.
 */
public interface ConnectCallback {
    void  onConnectComplete(WifiManager wifiManager,boolean isSuccess);
}
