package com.zyw.zhaochuan.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.zyw.zhaochuan.util.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by zyw on 2016/5/14.
 * 该类用于管理Wifi  Ap
 */
public class WifiAP {

    private  WifiManager mWifiManager;
    public WifiAP(WifiManager wifiManager)
    {
        this.mWifiManager=wifiManager;
    }
    private Context mContext = null;
    private String mSSID = "";
    private String mPasswd = "";

        //定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
        public enum WifiCipherType
        {
            WIFICIPHER_WEP,WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
        }

    public static final String TAG = "WifiApAdmin";


    public  void closeWifiAp() {
        closeWifiAp(mWifiManager);
    }

    public void startWifiAp(String ssid, String passwd) {
        mSSID = ssid;
        mPasswd = passwd;

        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }

        setWifiApEnabled(true);

    }


    private  void closeWifiAp(WifiManager wifiManager) {
        if (isWifiApEnabled()) {
            try {
                Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);

                WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);

                Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method2.invoke(wifiManager, config, false);
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private  boolean isWifiApEnabled() {
        try {
            Method method = mWifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(mWifiManager);

        } catch (NoSuchMethodException e) {
            // TODO Auto-ge nerated catch block
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }



    /**
     * 打开wifi功能
     *
     * @return
     */
    private boolean openWifi()
        {
            boolean bRet = true;
            if (!mWifiManager.isWifiEnabled())
            {
                bRet = mWifiManager.setWifiEnabled(true);
            }
            return bRet;
        }


    /**
     * 设置wifi热点打开状态
     * @param enabled
     * @return
     */
    private boolean setWifiApEnabled(boolean enabled) {
        if (enabled) { // disable WiFi in any case
            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
            mWifiManager.setWifiEnabled(false);
        }
        try {
            //热点的配置类
            WifiConfiguration apConfig = new WifiConfiguration();
            //配置热点的名称(可以在名字后面加点随机数什么的)
            apConfig.SSID = mSSID;

            //配置热点的密码
            apConfig.preSharedKey=mPasswd;
            //必须设置在这里
            apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            //通过反射调用设置热点
            Method method = mWifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            //返回热点打开状态
            return (Boolean) method.invoke(mWifiManager, apConfig, enabled);
        } catch (Exception e) {
            return false;
        }
    }
}
