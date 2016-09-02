package com.zyw.zhaochuan;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.transition.Explode;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.zyw.zhaochuan.activity.CaptureConnectQRActivity;
import com.zyw.zhaochuan.activity.SessionActivity;
import com.zyw.zhaochuan.activity.SettingActivity;
import com.zyw.zhaochuan.activity.ShareActivity;
import com.zyw.zhaochuan.util.Utils;
import com.zyw.zhaochuan.wifi.WifiAP;

import cn.bmob.v3.Bmob;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private WifiManager wifiManager;
    private WifiAP wifiAP;
    private Button bn_create;
    private  Button bn_connect;
    NavigationView navigationView;
    private boolean isServer;//标记是否是服务端
    public  static  MainActivity thiz;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bn_connect=(Button)findViewById(R.id.main_bn_connect);
        bn_create=(Button)findViewById(R.id.main_bn_create);
        bn_create.setOnClickListener(new ButtonOnClick());
        bn_connect.setOnClickListener(new ButtonOnClick());
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiAP =new WifiAP(wifiManager);
        setupWindowExitAnimation();

        Bmob.initialize(getApplicationContext(),"e0294739d44256d3ebcef804504885b3");
        thiz=this;
    }
    private  void setupWindowExitAnimation()
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Explode explode=new Explode();
            explode.setDuration(2000);
            getWindow().setExitTransition(explode);
        }

    }
    /**
     * 按钮事件
     */
    private final class ButtonOnClick implements View.OnClickListener {
        Intent intent;
        private  boolean isOpen=false;
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                //连接其他
                case R.id.main_bn_connect:
                    //是否打开wifi
                    if(wifiManager.isWifiEnabled()){
                     intent=new Intent(MainActivity.this,CaptureConnectQRActivity.class);
                    startActivity(intent);
                    }
                    else{
                        //Snackbar.make(navigationView,"请先打开并连接WIFI。",Snackbar.LENGTH_SHORT).show();
                        //打开wifi
                        wifiManager.setWifiEnabled(true);
                        intent=new Intent(MainActivity.this,CaptureConnectQRActivity.class);
                        startActivity(intent);
                       // wifiAP.CreateWifiInfo(Utils.getRandomSSID(),Utils.getRandomKey(), WifiAP.WifiCipherType.WIFICIPHER_INVALID);

                    }
                    break;
                //创建连接
                case R.id.main_bn_create:
                    if(wifiManager.isWifiEnabled()){
                        //如果wifi是开着的
                        isServer=true;
                        intent=new Intent(MainActivity.this,SessionActivity.class);
                        intent.setAction(SessionActivity.ACTION_SHOW_QR);
                        intent.putExtra("isServer",isServer);
                        intent.putExtra("local_ip",Utils.getIpAddress(wifiManager));
                        intent.putExtra("isAsAP",false);
                        startActivity(intent);
                    }
                else{
                        //如果wifi是关的,打开热点
                        String ssid=Utils.getRandomSSID();
                        String key=Utils.getRandomKey();
                        wifiAP.startWifiAp(ssid,key);
                        isServer=true;
                        intent=new Intent(MainActivity.this,SessionActivity.class);
                        intent.setAction(SessionActivity.ACTION_SHOW_QR);
                        intent.putExtra("isServer",isServer);
                        intent.putExtra("local_ip",Utils.getIpAddress(wifiManager));
                        intent.putExtra("isAsAP",true);
                        intent.putExtra("ap_ssid",ssid);
                        intent.putExtra("ap_key",key);
                        startActivity(intent);
                        Snackbar.make(navigationView,"WIFI热点已打开。",Snackbar.LENGTH_SHORT).show();
                  }

                    break;
            }
        }
    }

    /**
     * 返回键按下时
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            wifiAP.closeWifiAp();
            super.onBackPressed();

        }
    }



    /**
     * 导航菜单被点击后
     * @param item
     * @return
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id){
//            //设置
//            case R.id.nav_setting:
//                 intent=new Intent(MainActivity.this, SettingActivity.class);
//                startActivity(intent);
//                break;
            //分享
            case R.id.nav_share:
                intent=new Intent(MainActivity.this, ShareActivity.class);
                startActivity(intent);
                break;
            //关于
            case  R.id.nav_about:
                AlertDialog alertDialog=new AlertDialog.Builder(this)
                        .setTitle("赵传")
                        .setMessage(getResources().getString(R.string.about_dialog))
                        .setPositiveButton("我知道了",null)
                        .create();

                alertDialog.show();
                break;

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}

