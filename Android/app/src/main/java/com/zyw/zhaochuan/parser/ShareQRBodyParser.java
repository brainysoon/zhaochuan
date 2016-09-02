package com.zyw.zhaochuan.parser;


import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by zyw on 2016/7/24.
 */
public class ShareQRBodyParser  {

    public ShareQRBodyParser(String url, String key,String fileName) throws JSONException {
        this.url = url;
        this.key = key;
        this.obj=new JSONObject();
        obj.put("url", url);
        obj.put("key",key);
        obj.put("fileName",fileName);
        json=obj.toString();
    }

    public  ShareQRBodyParser(String json) throws JSONException {
        this.json=json;
        this.obj=new JSONObject(json);
        this.url =obj.getString("url");
        this.key=obj.getString("key");
        this.fileName=obj.getString("fileName");
    }


    @Override
    public String toString() {
        return  json;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    private  String url;
    private  String key;
    private  String json;

    public String getFileName() {
        return fileName;
    }

    private  String fileName;
    private JSONObject obj;
}
