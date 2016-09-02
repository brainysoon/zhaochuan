package com.zyw.zhaochuan.activity;

import java.io.IOException;
import java.util.Vector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.zyw.zhaochuan.R;
import com.zyw.zhaochuan.ThisApplication;
import com.zyw.zhaochuan.camera.CameraManager;
import com.zyw.zhaochuan.decoding.CaptureActivityHandler;
import com.zyw.zhaochuan.decoding.InactivityTimer;
import com.zyw.zhaochuan.interfaces.ConnectCallback;
import com.zyw.zhaochuan.parser.ConnectQRBodyParser;
import com.zyw.zhaochuan.util.Utils;
import com.zyw.zhaochuan.view.ViewfinderView;
import com.zyw.zhaochuan.wifi.WifiAutoConnectManager;
import com.zyw.zhaochuan.wifi.WifiConnector;

import org.json.JSONException;

/**
 * @author zyw
 *扫描连接二维码界面
 */
public class CaptureConnectQRActivity extends Activity implements Callback,ConnectCallback
{
	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;
    private  ProgressDialog progressDialog;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.capture_layout);
		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();


		if (hasSurface)
		{
			initCamera(surfaceHolder);
		}
		else
		{
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		decodeFormats = null;
		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL)
		{
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (handler != null)
		{
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy()
	{
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	private void initCamera(SurfaceHolder surfaceHolder)
	{
		try
		{
			CameraManager.get().openDriver(surfaceHolder);
		}
		catch (IOException ioe)
		{
			return;
		}
		catch (RuntimeException e)
		{
			return;
		}
		if (handler == null)
		{
			handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		if (!hasSurface)
		{
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		hasSurface = false;

	}

	public ViewfinderView getViewfinderView()
	{
		return viewfinderView;
	}

	public Handler getHandler()
	{
		return handler;
	}

	public void drawViewfinder()
	{
		viewfinderView.drawViewfinder();

	}

	/**
	 * 读取二维码成功时触发
	 * @param obj
	 * @param barcode
     */
	public void handleDecode(final Result obj, Bitmap barcode)
	{
		inactivityTimer.onActivity();
		playBeepSoundAndVibrate();//震动
		String text=obj.getText();
        ConnectQRBodyParser qrBodyParser = null;
        try {
            qrBodyParser = new ConnectQRBodyParser(text);
			//根据平台设置根目录
			String root=qrBodyParser.isPC()?"/":"/sdcard";
			((ThisApplication)getApplication()).setFileRoot(root);

			//==================================================================
			if(!qrBodyParser.isAsAP()) {
				//如果扫到对方不是Ap
				Intent intentAct = new Intent(CaptureConnectQRActivity.this, SessionActivity.class);
				intentAct.setAction(SessionActivity.ACTION_SHOW_SESSION);
				intentAct.putExtra("remote_ip", qrBodyParser.getIp());//远程ip
				intentAct.putExtra("isServer", false);
				startActivity(intentAct);
				finish();
			}else {

				//如果扫到对方是Ap,连接WIfI，并获取Ip-----------------------------------------------
				final ConnectQRBodyParser finalQrBodyParser = qrBodyParser;

				WifiManager wifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
				WifiAutoConnectManager wifiAutoConnectManager=new WifiAutoConnectManager(wifiManager);
				wifiAutoConnectManager.connect(finalQrBodyParser.getSsid(),qrBodyParser.getKey(), WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);

                progressDialog=ProgressDialog.show(this,"","正在连接中,请稍后...",false);
                ConnectThread connectThread=new ConnectThread(this,finalQrBodyParser,wifiManager);
                connectThread.start();

			}
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.qr_error),Toast.LENGTH_LONG).show();
			onPause();
			onResume();//刷新
            e.printStackTrace();
        }
	}

    @Override
    public void onConnectComplete(WifiManager wifiManager,boolean isSuccess) {
        //判断是否成功
        if(isSuccess) {
            final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            Utils.writeLogToSdcard(Utils.int2Ip(dhcpInfo.gateway));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intentAct = new Intent(CaptureConnectQRActivity.this, SessionActivity.class);
                    intentAct.setAction(SessionActivity.ACTION_SHOW_SESSION);
                    intentAct.putExtra("remote_ip", Utils.int2Ip(dhcpInfo.gateway));
                    intentAct.putExtra("isServer", false);
                    startActivity(intentAct);
                    finish();
                }
            });
        }else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),"连接失败，请重试。",Toast.LENGTH_LONG).show();
                    onPause();
                    onResume();//刷新
                }
            });
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.hide();
            }
        });

    }

    private  class ConnectThread extends Thread
	{
		private  ConnectCallback callback;
		private  WifiManager wifiManager;
		private ConnectQRBodyParser parser;
		public ConnectThread(ConnectCallback callback,ConnectQRBodyParser parser,WifiManager wifiManager)
		{
			this.callback=callback;
			this.parser=parser;
			this.wifiManager=wifiManager;
		}
		@Override
		public void run() {
			int i=0;
			boolean isSuccess=false;//标记是否连上了
            //
            System.out.println(wifiManager.getConnectionInfo().getSSID()+","+parser.getSsid());
            if(wifiManager.getConnectionInfo().getSSID().contains(parser.getSsid()))
            {
                callback.onConnectComplete(wifiManager,true);
                return ;
            }
			while (i++<=300)
			{
				String ssid=wifiManager.getConnectionInfo().getSSID();
				if(ssid!=null) {
					//判断ssid和网关ip是否正确。
					if(ssid.contains(parser.getSsid())&&wifiManager.getDhcpInfo().gateway!=0)
					{
						isSuccess=true;
						break;
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
            callback.onConnectComplete(wifiManager,isSuccess);
		}
	}

	private void initBeepSound()
	{
		if (playBeep && mediaPlayer == null)
		{
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
			try
			{
				mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			}
			catch (IOException e)
			{
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate()
	{
		if (playBeep && mediaPlayer != null)
		{
			mediaPlayer.start();
		}
		if (vibrate)
		{
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener()
	{
		public void onCompletion(MediaPlayer mediaPlayer)
		{
			mediaPlayer.seekTo(0);
		}
	};

}