package com.zyw.zhaochuan.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.zyw.zhaochuan.R;
import com.zyw.zhaochuan.fragment.ShareFileListFragment;

/**
 * Created by zyw on 2016/7/20.
 */
public class ShareActivity extends AppCompatActivity {
    public static final String NOTICE_BACKKEY_PRESS = "notice_back_key_press";
    public static ShareActivity thiz;
    private  FragmentTransaction transaction;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        thiz=this;
        transaction=getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frag_container,new ShareFileListFragment());
        transaction.commit();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.share_menu, menu);
        return true;
    }
    @Override
    public void onBackPressed() {
        //这里目的为了通知fragment，activity已经按下返回键，让她们自己处理
        Intent intent=new Intent(NOTICE_BACKKEY_PRESS);
        sendBroadcast(intent);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

      final  int id=item.getItemId();
        //按下左上角的返回键
        if(id==android.R.id.home)
        {
            finish();
        }
        //下载
        else if(id==R.id.share_download)
        {
            Intent intent=new Intent(this,CaptureShareQRActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}
