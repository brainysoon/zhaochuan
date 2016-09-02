package com.zyw.zhaochuan.activity;

import android.graphics.Bitmap;

import com.google.zxing.Result;
import com.zyw.zhaochuan.interfaces.CaptureCompleteCallback;
import com.zyw.zhaochuan.parser.ShareQRBodyParser;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zyw on 2016/7/24.
 * 扫描分享文件的fragment
 */
public class CaptureShareQRActivity extends CaptureConnectQRActivity   {


    private static CaptureCompleteCallback callback;

    public  static  void setCaptureCompleteCallback(CaptureCompleteCallback captureCompleteCallback)
    {
        callback=captureCompleteCallback;
    }


    @Override
    public void handleDecode(Result obj, Bitmap barcode) {
        try {
            ShareQRBodyParser shareQRBodyParser=new ShareQRBodyParser(obj.getText());
           // Intent intent=new Intent();
            Map<String,String> map=new HashMap<String,String>();
            map.put("url",shareQRBodyParser.getUrl());
            map.put("key",shareQRBodyParser.getKey());
            map.put("fileName",shareQRBodyParser.getFileName());
            callback.onCaptureComplete(map);
            finish();
//            sendBroadcast(intent);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
