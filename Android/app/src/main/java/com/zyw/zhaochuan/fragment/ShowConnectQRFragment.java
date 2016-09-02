package com.zyw.zhaochuan.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zyw.zhaochuan.R;
import com.zyw.zhaochuan.ThisApplication;
import com.zyw.zhaochuan.activity.SessionActivity;
import com.zyw.zhaochuan.parser.ConnectQRBodyParser;
import com.zyw.zhaochuan.util.QRMaker;

/**
 * Created by zyw on 2016/6/4.
 * 属于SessionActivity
 */
public class ShowConnectQRFragment extends Fragment {
    private View rootView;
    ImageView qrImageView;
    TextView  ipTextView;
    Intent intent;
    private  SessionActivity rootAct;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootAct=SessionActivity.thiz;
        intent=rootAct.getIntent();
        rootView=inflater.inflate(R.layout.show_qr_layout,container,false);
        qrImageView=(ImageView) rootView.findViewById(R.id.show_qr_iv);
        ipTextView=(TextView)rootView.findViewById(R.id.show_ip_tv);
        boolean isStartFromAp=intent.getBooleanExtra("isAsAP",false);//标记是否作为热点
        String ssid=intent.getStringExtra("ap_ssid");//从MainActivity获取的ssid和key
        String key=intent.getStringExtra("ap_key");

        ((ThisApplication)rootAct.getApplication()).setFileRoot("/sdcard");
        if(isStartFromAp)
        {
            //开启为热点
            ConnectQRBodyParser qrBodyParser = new ConnectQRBodyParser("0.0.0.0", ssid, key, "",true,false);//开启为热点，此处ip设空
            //生成二维码
            qrImageView.setImageBitmap(QRMaker.createQRImage(qrBodyParser.toString()));

        }else {
            ConnectQRBodyParser qrBodyParser = new ConnectQRBodyParser(intent.getStringExtra("local_ip"),"", "", "",false,false);
            //生成二维码
            qrImageView.setImageBitmap(QRMaker.createQRImage(qrBodyParser.toString()));
            ipTextView.setText(intent.getStringExtra("local_ip"));
        }
        SessionActivity.thiz.getSupportActionBar().hide();//隐藏ActionBar，为了隐藏菜单
        //为了接受返回键的广播
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(SessionActivity.NOTICE_BACKKEY_PRESS);
        rootAct.registerReceiver(broadcastReceiver,intentFilter);
        return rootView;
    }

    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(rootAct.NOTICE_BACKKEY_PRESS))
            {
                rootAct.finish();
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(broadcastReceiver!=null)
        rootAct.unregisterReceiver(broadcastReceiver);
    }
}
