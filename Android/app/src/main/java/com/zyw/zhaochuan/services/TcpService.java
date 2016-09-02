package com.zyw.zhaochuan.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.zyw.zhaochuan.ThisApplication;
import com.zyw.zhaochuan.activity.SessionActivity;
import com.zyw.zhaochuan.interfaces.DataOperator;
import com.zyw.zhaochuan.interfaces.OnTransProgressChangeListener;
import com.zyw.zhaochuan.util.FileNameSort;
import com.zyw.zhaochuan.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * 该类是tcp服务类,Tcp短连接，用作服务端
 * 发送和接受数据都可以在此类内完成
 * 当发送文件时只负责发送，不管对方怎么收，也不管对方存放在哪
 * @author zyw
 *
 */
public  class TcpService extends Service implements DataOperator {

    /**包头**/
    public final byte DATA_TYPE_BYTE=0;
    public final byte DATA_TYPE_CHAR=1;

    /**请求类型**/
    public static final String REQUEST_TYPE_GET_CONTENT="get_content";//获取目录
    public static final String REQUEST_TYPE_SEND_FILE="send_file";//发送文件命令，给远程存储要存储的路径
    public static final String REQUEST_TYPE_RENAME_FILE="rename_file";//重命名文件
    public static final String REQUEST_TYPE_DELETE_FILE="delete_file";//删除文件
    public static final String REQUEST_TYPE_GET_FILE="get_file";//请求获取远端文件
    public static final String REQUEST_TYPE_CONNECTED="connect_server";//请求连接
    public static final String REQUEST_TYPE_DISCONNECT="disconnect";//请求断开连接
    public static final String REQUEST_TYPE_COPY_FILE="copy_file";//收到这个命令，就将本地文件复制到本地的另一个位置
    /**应答类型**/
    public static final String RESPONSE_TYPE_CONTENT="resp_list";//表示收到目录，收到这个命令就更新自己的远程文件列表

    /**通知**/
    public static final String NOTICE_TYPE_CLIENT_CONTENTED="notice_client_connected";//通知目标Activity，客户端已连接
    public static final String NOTICE_TYPE_UPDATE_REMOTE_LIST="notice_update_remote_list";//通知目标Activity，更新列表
    public static  final String NOTICE_TYPE_GETTED_MSG="notice_update_progress";//通知目标Activity，收到目录，测试用
    public static  final String NOTICE_TYPE_FILE_CLOSE_ACTIVITY="notice_close_activity";//通知目标Activity，关闭界面
    /**结束标志**/
    public static final int END_OF_STREAM =-1;
    /**全局变量声明**/
    //是否提供服务
    public boolean isServer=true;
    //远程文件存储目录，指示当收到文件时，远程该存放文件在哪里，当用户发送sendSendFileMsg命令时，这个变量就被设置
    private  String saveFilePath;
    //对方获取的目录
    private  String remoteRequestRoot;
    private Socket socket;
    private static  final String  SD_PATH= Environment.getExternalStorageDirectory().getAbsolutePath();
    //标记是启动为服务端
    public static  final  int START_ACTION_LISTEN=0;

    private SimpleBinder simpleBinder;

    private  Intent intent;

    //输入输出流放在全局就不用传来传去的了
    private   OutputStream out;
    private  InputStream in;

    private SocketThread  serverThread;

    private  String TAG="TcpService";
    private IntentFilter intentFilter;

    private String remoteHost;
    private  int remotePort;

    private  final int BUFFER_SIZE=8192;
    private OnTransProgressChangeListener onTransProgressChangeListener;

    public void setOnTransProgressChangeListener(OnTransProgressChangeListener onTransProgressChangeListener)
    {
        this.onTransProgressChangeListener=onTransProgressChangeListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        simpleBinder=new SimpleBinder();
    }

    /**
     * 如果是非绑定方式
     * onCreate->onStartCommand->onDestory
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 如果是绑定方式执行,他的执行顺序如下
     * onCreate->onBind->onUnBind->onDestroy
     * @param intent
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        TAG+=",Server";
        this.intent=intent;
        //启动为服务端
        if(intent.getIntExtra("start_action",START_ACTION_LISTEN)==START_ACTION_LISTEN)
        {
            try {
                String remoteIp=intent.getStringExtra("remote_ip");
                if(remoteIp!=null) {
                    remoteHost = intent.getStringExtra("remote_ip");
                }
                remotePort=intent.getIntExtra("remote_port", ((ThisApplication)getApplication()).getAppPort());
                tcpListen(remotePort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return simpleBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        SessionActivity.tcpService.sendDisconnectMsg();
        close();
        return true;
    }

    public   class SimpleBinder extends Binder {
        /**
         * 获取 Service 实例
         * @return
         */
        public TcpService getService(){
            return TcpService.this;
        }
    }


    public String getRemoteRequestRoot() {
        return remoteRequestRoot;
    }
    public void setRemoteRequestRoot(String remoteRequestRoot) {
        this.remoteRequestRoot = remoteRequestRoot;
    }

    public String getSaveFilePath() {
        return saveFilePath;
    }
    public void setSaveFilePath(String saveFilePath) {
        this.saveFilePath = saveFilePath;
    }

    /**
     * 服务器监听
     * @param port
     * @throws IOException
     */
    public void tcpListen(int port) throws IOException
    {
        serverThread=new SocketServerThread(port);
        serverThread.start();
    }

    /*
    * ************************************************************
     * Socket父类
     ************************************************************
     */
    public abstract class SocketThread extends Thread
    {
        /**
         * 关闭连接资源
         * @throws IOException
         */
        public abstract void close() throws IOException;
        /**
         * 命令执行者
         * 执行各种收到的命令
         * @throws IOException
         */
        public void runCmd() throws IOException {
            String cmd=readCommand();

            //收到目录，测试用=================================
            Intent it=new Intent(NOTICE_TYPE_GETTED_MSG);
            it.putExtra("json","收到的消息：\n"+cmd);
            sendBroadcast(it);

            JSONObject json=null;
            try {
                json=new JSONObject(cmd);
            } catch (Exception e) {
                e.printStackTrace();
                return;//Json格式出错还搞毛啊，直接返回
            }
            //收到获取目录的命令
            if(cmd.contains(REQUEST_TYPE_GET_CONTENT))
            {
                if(json!=null) {
                    try {
                        setRemoteRequestRoot(json.getString("path"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sendContent();
                }

            }
            //收到重命名的命令
            else if(cmd.contains(REQUEST_TYPE_RENAME_FILE))
            {

                if(json!=null) {
                    try {
                        File file=new File(json.getString("path"));
                        file.renameTo(new File(json.getString("new_path")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sendContent();
                }
            }
            //收到删除文件命令
            else if(cmd.contains(REQUEST_TYPE_DELETE_FILE))
            {
                File f;
                try {
                    f=new File(json.getString("path"));
                    Utils.deleteFile(f);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendContent();
            }
            //收到目录这个命令就更新自己的远程文件列表
            else if(cmd.contains(RESPONSE_TYPE_CONTENT))
            {
                Intent intent=new Intent(NOTICE_TYPE_UPDATE_REMOTE_LIST);
                intent.putExtra("json",cmd);
                sendBroadcast(intent);
            }
            //收到要接受文件的命令
            else if(cmd.contains(REQUEST_TYPE_SEND_FILE))
            {

                try {
                    setSaveFilePath(json.getString("path"));
                    System.out.println(json.getString("path"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            //对方要获取文件.就给他发文件
            else if(cmd.contains(REQUEST_TYPE_GET_FILE))
            {
                try {
                    sendFile(new File(json.getString("path")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            //收到复制文件的命令，同端之间
            else if(cmd.contains(REQUEST_TYPE_COPY_FILE))
            {

                if(json!=null) {
                    try {
                        File file=new File(json.getString("path"));
                        File newFile=new File(json.getString("new_path"));

                        Utils.copyFile(file,newFile,null);//同端之间复制文件

                    } catch (Exception e) {
                        System.out.println("远端试图执行复制文件失败："+e.toString());
                        e.printStackTrace();
                    }
                    sendContent();
                }
            }
            //客户端连接上
            else if(cmd.contains(REQUEST_TYPE_CONNECTED))
            {
//                String ip=socket.getInetAddress().getHostAddress();
//                remoteHost= ip.toString();
                try {
                    String remoteIP=json.getString("local_ip");//将对方的ip存为自己的ip
                    remoteHost=remoteIP;
                    noticeClientConnected();//给界面发送通知
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            //断开连接
            else if(cmd.contains(REQUEST_TYPE_DISCONNECT))
            {
                Intent intent=new Intent(NOTICE_TYPE_FILE_CLOSE_ACTIVITY);
                sendBroadcast(intent);
                isServer=false;
            }
        }
    }


    /**************************************************************
     * 关闭服务
     **************************************************************/
    public void close()
    {
        isServer=false;

        if(serverThread!=null)
            try {
                serverThread.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        if(out!=null)
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if(in!=null)
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        stopSelf();
    }


    /*
    * ********************************************************************
     *服务器的线程
     * *******************************************************************
     * */
    private  class SocketServerThread extends SocketThread {
        private ServerSocket serverSocket;
        private  boolean isCanCreateThread=true;//标记是否可以创建线程，巧妙解决多任务传输
        private int port;
        public SocketServerThread(int port){
            // TODO Auto-generated constructor stub
            this.port=port;
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub

            try {
                serverSocket=new ServerSocket(port);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
                while(isServer)
                {

                    try {
                        isCanCreateThread=false;//标记不能创建新线程
                        socket=serverSocket.accept();
                        isCanCreateThread=true;//这个线程有连接进入了，不等待了，另外的一个线程可以创建线程了
                        out=socket.getOutputStream();
                        in=socket.getInputStream();
                        byte dataType=-1;
                        DataInputStream dataInputStream=new DataInputStream(in);
                        dataType=dataInputStream.readByte();
                        if(dataType==DATA_TYPE_CHAR){
                            //字符流
                            runCmd();
                        }else if(dataType==DATA_TYPE_BYTE) {
                            saveFile(new File(getSaveFilePath()));
                        }
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }/*
                    if(!isCanCreateThread)
                        continue;//如果有一个线程在等待，那么就不创建新线程
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                isCanCreateThread=false;//标记不能创建新线程
                                socket=serverSocket.accept();
                                isCanCreateThread=true;//这个线程有连接进入了，不等待了，另外的一个线程可以创建线程了
                                out=socket.getOutputStream();
                                in=socket.getInputStream();
                                byte dataType=-1;
                                DataInputStream dataInputStream=new DataInputStream(in);
                                dataType=dataInputStream.readByte();
                                if(dataType==DATA_TYPE_CHAR){
                                    //字符流
                                    runCmd();
                                }else if(dataType==DATA_TYPE_BYTE) {
                                    saveFile(new File(getSaveFilePath()));
                                }
                            }catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }).start();*/
                 }
        }

        public void close() throws IOException {
            isServer=false;
            if(serverSocket!=null)
                serverSocket.close();
            if(socket!=null)
                socket.close();
        }
    }


    /**
     * 显示通知栏
     * @param current
     * @param max
     */
    public  void updateUI(int current,long max)
    {
        onTransProgressChangeListener.onProgressChange(current,max);
    }

    /**
     * 发送文本消息
     * @param head 文本头
     * @param msg 文本消息
     */
    private synchronized   void sendTextMsg(final byte head, final String msg)
    {
        //测试-----------------------------------------------------
        Intent it=new Intent(NOTICE_TYPE_GETTED_MSG);
        it.putExtra("json","发送的消息：\n"+msg);
        sendBroadcast(it);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket= null;
                OutputStream outputStream=null;
                DataOutputStream dataOutputStream=null;
                try {
                    socket = new Socket(remoteHost, remotePort);
                    outputStream=socket.getOutputStream();
                    dataOutputStream=new DataOutputStream(outputStream);
                    dataOutputStream.writeByte(head);//发送头
                    dataOutputStream.write(msg.getBytes());//发送内容体
                    dataOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    if(dataOutputStream!=null) {
                        try {
                            dataOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if(socket!=null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }).start();
    }


    /**
     * 重命名
     *
     * 发送的内容：
     * {
     * "command":"rename_file",
     * "path":"远程目录"
     * }
     *
     * @param path 原文件的路径
     * @param  newPath 新的文件的路径
     * @throws IOException
     */
    @Override
    public void sendRenameFileMsg( String path,String newPath) throws IOException {
        // TODO Auto-generated method stub
        final String msg=String.format("{\"command\":\"%s\",\"path\":\"%s\",\"new_path\":\"%s\"}", REQUEST_TYPE_RENAME_FILE,path,newPath);
        sendTextMsg(DATA_TYPE_CHAR,msg);
    }


    /**
     * 删除文件
     *
     * 发送的内容：
     * {
     * "command":"delete_file",
     * "path":"远程目录"
     * }
     *
     * @param path
     * @throws IOException
     */
    @Override
    public  void sendDeleteFileMsg( String path) throws IOException {
        // TODO Auto-generated method stub
        final String msg=String.format("{\"command\":\"%s\",\"path\":\"%s\"}", REQUEST_TYPE_DELETE_FILE,path);
        sendTextMsg(DATA_TYPE_CHAR,msg);
    }
    /**
     * 给远程发送发送文件命令，对方收到这个命令就可以知道它自己该存文件在哪了
     *
     * 发送的内容：
     * {
     * "command":"send_file",
     * "path":"远程文件"
     * }
     * @throws IOException
     */
    @Override
    public  void sendSendFileMsg(String path) throws IOException {
        // TODO Auto-generated method stub
        final String msg=String.format("{\"command\":\"%s\",\"path\":\"%s\"}", REQUEST_TYPE_SEND_FILE,path);

        sendTextMsg(DATA_TYPE_CHAR,msg);
    }

    /**
     * 发送文件
     * @param file 要发送的本地文件
     * @throws IOException
     */
    @Override
    public synchronized void sendFile(final File file) throws IOException{
        // TODO Auto-generated method stub
        final FileInputStream fileInputStream=new FileInputStream(file);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket= null;
                DataOutputStream dataOutputStream = null;
                try {
                    socket = new Socket(remoteHost, remotePort);
                    out=socket.getOutputStream();
                     dataOutputStream=new DataOutputStream(out);
                    byte[] buf=new byte[BUFFER_SIZE];
                    int count=0;
                    int sentSize=0;//已发送的字节数
                    long fileSize=file.length();
                    dataOutputStream.write(DATA_TYPE_BYTE);//发送头
                    dataOutputStream.writeLong(fileSize);//文件大小
                    while((count=fileInputStream.read(buf))!=END_OF_STREAM)//发送内容体
                    {
                        sentSize+=count;
                        updateUI(sentSize,fileSize);
                        dataOutputStream.write(buf,0,count);
                        dataOutputStream.flush();
                    }
                    fileInputStream.close();//关闭读取文件流
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {

                    if(dataOutputStream!=null) {
                        try {
                            dataOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if(socket!=null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }

            }
        }).start();
    }

    /**
     * 发送复制文件命令，同端之间
     * @param sourceFile 要复制的文件
     * @param targetFile 目标文件
     */
    @Override
    public void sendCopyFileMsg(String sourceFile, String targetFile)
    {
        final String msg=String.format("{\"command\":\"%s\",\"path\":\"%s\",\"new_path\":\"%s\"}", REQUEST_TYPE_COPY_FILE,sourceFile,targetFile);
        sendTextMsg(DATA_TYPE_CHAR,msg);
    }

    /**
     * 从流中读取并保存成文件
     * @param file 保存的地方
     */
    @Override
    public synchronized  void saveFile( File file) throws IOException {
        // TODO Auto-generated method stub
        FileOutputStream fileOutputStream=new FileOutputStream(file);
        System.out.println("saveFile:"+file.toString());
        byte[] buf=new byte[BUFFER_SIZE];
        int len=0;
        DataInputStream dataInputStream=new DataInputStream(in);
        long fileSize=dataInputStream.readLong();//读取文件大小
        int size=0;
        while ((len=dataInputStream.read(buf))!= END_OF_STREAM) {
            size+=len;
            updateUI(size,fileSize);
            fileOutputStream.write(buf, 0, len);
            fileOutputStream.flush();
        }//in不用关
        fileOutputStream.close();//关闭文件流
        sendContent();
    }



    /**
     * 读取流中的命令，不读头
     * @throws
     */
    @Override
    public synchronized  String readCommand() throws IOException {
        //TODO Auto-generated method stub
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(in));
        int ch=-1;
        StringBuilder sb=new StringBuilder();
        while ((ch=bufferedReader.read())!=END_OF_STREAM) {
            sb.append((char) ch);
        }
        return sb.toString();

    }

    /**
     * 通知各组件，客户端已连接
     */
    @Override
    public   void noticeClientConnected() {
        Intent intent=new Intent();
        intent.setAction(NOTICE_TYPE_CLIENT_CONTENTED);
        sendBroadcast(intent);
        //给客户端发目录
    }

    /**
     * 发送断开连接命令
     */
    @Override
    public void sendDisconnectMsg() {
        final String msg=String.format("{\"command\":\"%s\"}", REQUEST_TYPE_DISCONNECT);
        sendTextMsg(DATA_TYPE_CHAR,msg);

    }


    @Override
    /**
     * 发送请求连接请求
     * remote_ip，对方的ip。对方收到这个命令后，就会把自己的remote_ip存入local_ip
     * local_ip，本地的ip。对方收到这个命令后，就会把自己的local_ip存入remote_ip(未定)
     */
    public void sendConnectedMsg() throws IOException{
        WifiManager wifiManager=(WifiManager) getSystemService(Context.WIFI_SERVICE);
        final String msg=String.format("{\"command\":\"%s\",\"remote_ip\":\"%s\",\"local_ip\":\"%s\"}", REQUEST_TYPE_CONNECTED,remoteHost, Utils.getIpAddress(wifiManager));
        sendTextMsg(DATA_TYPE_CHAR,msg);
    }

    /**
     * 获取目录
     *
     * 发送的内容：
     * {
     * "command":"get_content",
     * "path":"远程目录"
     * }
     *
     * @param path
     * @throws IOException
     */
    @Override
    public  void sendGetContentMsg( String path) throws IOException {
        // TODO Auto-generated method stub

        final String msg=String.format("{\"command\":\"%s\",\"path\":\"%s\"}", REQUEST_TYPE_GET_CONTENT,path);
        sendTextMsg(DATA_TYPE_CHAR,msg);
    }

    /**
     * 发送目录
     * 获取自己的目录并发过去
     *如果收到这个命令就更新自己的远程文件列表
     */
    @Override
    public synchronized  void sendContent() throws IOException {
        // TODO Auto-generated method stub
        final StringBuilder msg=new StringBuilder();
        System.out.println("请求的目录："+getRemoteRequestRoot());
        File[] files=new File(getRemoteRequestRoot()).listFiles();
        Arrays.sort(files,new FileNameSort());
        msg.append(String.format("{\"command\":\"%s\",\"root\":\"%s\",\"files\":["
                ,RESPONSE_TYPE_CONTENT
                ,getRemoteRequestRoot()));

        for(File f:files)
        {
            msg.append(String.format("{\"name\":\"%s\",\"length\":%s,\"isFile\":%s,\"lastModified\":%s},"
                    ,f.getName()
                    ,f.length()
                    ,f.isFile()
                    ,f.lastModified()));
        }
        msg.append("]}");
        String newMsg=msg.toString().replace(",]","]");//替换掉
        sendTextMsg(DATA_TYPE_CHAR,newMsg);
    }
    /**
     * 发送获取文件命令，远端收到该命令就给本地流中写数据
     * 问题：本地给对方发送此命令，对方给我发送文件，本地应该存在哪。
     * {
     * "command":"get_file",
     * "path":""  --要获取的远程文件
     * }
     * @throws IOException
     */
    @Override
    public  void sendGetFileMsg(String remoteFile) throws IOException {
        // TODO Auto-generated method stub
        final String msg=String.format("{\"command\":\"%s\",\"path\":\"%s\"}", REQUEST_TYPE_GET_FILE,remoteFile);
        sendTextMsg(DATA_TYPE_CHAR,msg);
    }


}