package com.zyw.zhaochuan.parser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by zyw on 2016/5/5.
 * 二维码的实体
 * json数据的生成和解析
 */
public class ConnectQRBodyParser {

    public ConnectQRBodyParser(String json) throws JSONException {
        this.json=json;
            JSONObject jsonObject=new JSONObject(json);
            this.ip=jsonObject.optString("ip");
            this.ssid = jsonObject.optString("ssid");
            this.key = jsonObject.optString("key");
            this.username = jsonObject.optString("username");
            this.isAsAP = jsonObject.optBoolean("isAsAP");
            this.isPC=jsonObject.optBoolean("isPC");
    }

    public ConnectQRBodyParser(String ip, String ssid, String key, String user, boolean isAsAP, boolean isPC) {
        this.ip = ip;
        this.ssid = ssid;
        this.key = key;
        this.username = user;
        this.isAsAP = isAsAP;
        this.isPC=isPC;
        this.json=String.format("{\"ip\":\"%s\",\"ssid\":\"%s\",\"key\":\"%s\",\"username\":\"%s\",\"isAsAP\":%s,\"isPC\":%s}",ip,ssid,key, username, isAsAP,isPC);

    }


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public boolean isAsAP() {
        return isAsAP;
    }

    public void setAsAP(boolean asAP) {
        isAsAP = asAP;
    }

    @Override
    public String toString() {
        return json;
    }
    public boolean isPC() {
        return isPC;
    }

    public void setPC(boolean PC) {
        isPC = PC;
    }
    private  String ip;
    private String ssid;
    private  String key;
    private String username;
    private String json;
    private boolean isPC;
    private boolean isAsAP;
}
