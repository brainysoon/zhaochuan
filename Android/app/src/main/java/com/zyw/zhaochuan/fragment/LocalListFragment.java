package com.zyw.zhaochuan.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.zyw.zhaochuan.R;
import com.zyw.zhaochuan.ThisApplication;
import com.zyw.zhaochuan.activity.SessionActivity;
import com.zyw.zhaochuan.adapter.FileListAdapter;
import com.zyw.zhaochuan.entity.FileListItem;
import com.zyw.zhaochuan.interfaces.FileListInterface;
import com.zyw.zhaochuan.interfaces.OnTransProgressChangeListener;
import com.zyw.zhaochuan.util.FileNameSort;
import com.zyw.zhaochuan.util.IntentTool;
import com.zyw.zhaochuan.util.Utils;
import com.zyw.zhaochuan.view.RecycleViewDivider;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by zyw on 2016/5/18.
 * 属于ShareActivity
 */
public class LocalListFragment extends Fragment implements FileListInterface,OnTransProgressChangeListener{
    private RecyclerView recyclerView;
    private Context context;
    private FileListAdapter fileListAdapter;
    private List<FileListItem> fileListItems;
    final String SD_PATH= Environment.getExternalStorageDirectory().getAbsolutePath();
    private File[] files;
    private  View rootView =null;
    public static File curPath;//当前显示的目录，一定是目录
    private Toast toast;
     static List<FileListItem> fileListItemsCache;
    final String TAG="LocalListFragment";
    public static File willSendFilePath;
    private  FloatingActionButton pasteFloatButton;
    private ProgressDialog copyProgressDialog;
    private  PackageManager pm = null;
    private SessionActivity rootAct;
    private  ThisApplication application;
    public static String NOTICE_LOAD_LIST="notice_load_list";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=getContext();
       // Log.v(TAG,"被创建");
        if(rootView ==null) {
            rootView = inflater.inflate(R.layout.file_list_layout, null);
            recyclerView = (RecyclerView) rootView.findViewById(R.id.filelist_reclyclerview);
            pasteFloatButton=(FloatingActionButton)rootView.findViewById(R.id.float_button_paste);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            //添加分割线
            recyclerView.addItemDecoration(new RecycleViewDivider(context, LinearLayoutManager.VERTICAL));
            fileListItems = new ArrayList<FileListItem>();
            curPath=new File(SD_PATH);
            //loadList前先加载资源
            pm=context.getPackageManager();

            rootAct=SessionActivity.thiz;
            loadList(curPath,true);
            fileListAdapter = new FileListAdapter(context, fileListItems);
            //列表项单击事件
            fileListAdapter.setOnItemClickListener(new RecyclerViewEvents());
            //列表长按事件
            fileListAdapter.setOnItemLongClickListener(new RecyclerViewEvents());
            recyclerView.setAdapter(fileListAdapter);

            //按下粘贴按钮
            pasteFloatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //判断文件是否从本地复制
                    if(!application.isCopyFromLocal()) {
                        if (RemoteListFragment.willSendFilePath != null) {
                            try {
                                SessionActivity.tcpService.sendGetFileMsg(RemoteListFragment.willSendFilePath.getAbsolutePath());
                                SessionActivity.tcpService.setSaveFilePath(curPath.getAbsolutePath() + File.separator + RemoteListFragment.willSendFilePath.getName());
                            } catch (IOException e) {
                                showToast("粘贴失败");
                                e.printStackTrace();
                            }
                        }
                    }else{
                        //从本地进行复制，开启线程复制
                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                try {
                                    boolean notSame=Utils.copyFile(willSendFilePath,new File(curPath+File.separator+willSendFilePath.getName()),LocalListFragment.this);
                                    application.setCopyFromLocal(false);//复制了
                                    if(!notSame){
                                        showToast("文件复制失败");
                                    }
                                } catch (IOException e) {
                                    rootAct.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showToast("文件复制失败");
                                        }
                                    });
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
            });
            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(rootAct.NOTICE_BACKKEY_PRESS);
            intentFilter.addAction(NOTICE_LOAD_LIST);//更新本地列表
            rootAct.registerReceiver(broadcastReceiver,intentFilter);
            application=(ThisApplication)rootAct.getApplication();
            application.setCopyFromLocal(false);//初始设置为没有复制
            copyProgressDialog =new ProgressDialog(rootAct);
            copyProgressDialog.setTitle("文件复制中...");
            copyProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            copyProgressDialog.setCancelable(false);
            copyProgressDialog.setMax(100);
        }

        rootAct.getSupportActionBar().show();//显示ActionBar
        rootAct.getSupportActionBar().setTitle(getShortPath(curPath.toString()));
        SessionActivity.isLocalFragment=true;
        return rootView;
    }

    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action=intent.getAction();
            if(action.equals(rootAct.NOTICE_BACKKEY_PRESS)){
                goBack();
            }else if(action.equals(NOTICE_LOAD_LIST))
            {
                loadList(curPath,true);
                fileListAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * 复制本地文件的进度
     * @param current
     * @param max
     */
    @Override
    public void onProgressChange(final long current,final long max) {
        rootAct.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                copyProgressDialog.setProgress((int)(((double)current/max)*100));
                copyProgressDialog.show();
                if(current>=max) {
                    copyProgressDialog.hide();
                    loadList(curPath,false);
                    fileListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private class RecyclerViewEvents implements FileListAdapter.OnRecyclerViewItemClickListener,FileListAdapter.OnRecyclerViewItemLongClickListener
    {
        /**
         * 列表单击事件
         * @param view
         * @param pos
         */
        @Override
        public void onItemClick(View view, int pos) {

            if (pos == 0 && !curPath.toString().equals("/"))
            {
                goBack();
            }
            //点击文件夹时,这里要注意数组的范围
            else if ((pos == 0 && curPath.toString().equals("/")) || files[pos-1].isDirectory())
            {
                try
                {
                    fileListItemsCache=new ArrayList<FileListItem>();
                    //深复制
                    Utils.listCopy(fileListItemsCache,fileListItems);
                    //对根目录的处理
                    if (curPath.toString().equals("/"))
                    {
                        curPath =files[pos];
                    }
                    else
                    {
                        curPath =files[pos-1];
                    }

                    loadList(curPath,true);
                    fileListAdapter.notifyDataSetChanged();
                }
                catch (Exception e)
                {
                    //发生异常时，当前路径不变，并重新载入列表
                    showToast("抱歉，发生了错误！" + "\n" + e.toString());
                    goBack();
                }
            }

            //点击文件时
            else if (files[pos-1].isFile())
            {
               Intent intent= IntentTool.openFile(files[pos-1].getAbsolutePath());
                startActivity(intent);
            }
        }

        /**
         * 列表长按事件
         * @param view
         * @param pos
         */
        @Override
        public void onItemLongClick(View view, final int pos) {

            boolean isFileItem=false;
            File selectedPath=null;
            if (pos == 0 && !curPath.toString().equals("/"))
            {
                return;
            }
            else if ((pos == 0 && curPath.toString().equals("/")) || files[pos-1].isDirectory()) //是目录
            {
                if (curPath.toString().equals("/"))
                {
                    selectedPath =files[pos];
                }
                else
                {
                    selectedPath =files[pos-1];
                }
                isFileItem=false;
            }
            else if (files[pos-1].isFile())//文件。
            {
                selectedPath=files[pos-1];
                isFileItem=true;
            }
            final File finalSelectedPath = selectedPath;
            //根据长按的项目，判段生成数组
            final String[] menuArr= isFileItem? getResources().getStringArray(R.array.local_context_menu_item_file):getResources().getStringArray(R.array.local_context_menu_item_folder);
            final boolean finalIsFileItem = isFileItem;
            Dialog dialog = new AlertDialog.Builder(rootAct)
                    .setItems(menuArr, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (menuArr[which]) {// /Java7新特性，switch判断字符串
                                case "发送":
                                    //如果是文件，它是发送
                                    try {
                                        //空判断
                                        String remoteFilePath=RemoteListFragment.curPath==null?application.getFileRoot():RemoteListFragment.curPath.getAbsolutePath();
                                        //通知对方该存文件在哪
                                        SessionActivity.tcpService.sendSendFileMsg(remoteFilePath+File.separator+finalSelectedPath.getName());
                                        //通知对方给自己发的目录
                                        SessionActivity.tcpService.setRemoteRequestRoot(finalSelectedPath.getPath());
                                        //发送文件
                                        SessionActivity.tcpService.sendFile(finalSelectedPath);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;

                                case "复制":
                                    //复制文件
                                    //复制本地的了，远程的路径清空
                                    RemoteListFragment.willSendFilePath=null;
                                    willSendFilePath= finalSelectedPath;
                                    Toast.makeText(SessionActivity.thiz,willSendFilePath.getAbsolutePath(),Toast.LENGTH_LONG).show();
                                    application.setCopyFromLocal(true);//标记已复制，是本地复制
                                    break;
                                case "重命名":
                                    //重命名
                                    rename(finalSelectedPath);
                                    break;

                                case "删除":
                                    //删除文件或文件夹
                                    Utils.deleteFile(finalSelectedPath);
                                    loadList(curPath,false);
                                    fileListAdapter.notifyDataSetChanged();
                                    break;
                            }
                        }
                    })
                    .create();
            dialog.show();
        }

    }

    /**
     * 重命名
     */
    private void rename(final File oldName)
    {
        final EditText editText=new EditText(context);
        editText.setText(oldName.getName());
        AlertDialog alertDialog=new AlertDialog.Builder(context)
                .setTitle("重命名")
                .setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text=editText.getText().toString();
                        if(!text.equals(""))
                        {
                            oldName.renameTo(new File(oldName.getParent()+File.separator+text));
                            loadList(curPath,false);
                            fileListAdapter.notifyDataSetChanged();
                        }else
                        {
                            showToast("重命名失败");
                        }
                    }
                })
                .create();
        alertDialog.show();
    }


    /**
     * 返回上一级目录，有ui操作
     */
    public void goBack() {
        try
        {

            if(curPath.getAbsolutePath().equals("/"))
                return;
            curPath = curPath.getParentFile();
            //缓存都不为空时才能使用缓存
            if (fileListItemsCache!=null)
            {
                //深复制
                Utils.listCopy(fileListItems,fileListItemsCache);
                //使用后清空缓存
                fileListItemsCache = null;

                fileListAdapter.notifyDataSetChanged();

                files=curPath.listFiles();
                Arrays.sort(files,new FileNameSort());
            }
            //没有缓存就重新获取列表
            else
            {
                loadList(curPath,true);

                fileListAdapter.notifyDataSetChanged();
            }

            rootAct.getSupportActionBar().setTitle(getShortPath(curPath.toString()));
        }
        catch (Exception e)
        {
            showToast("抱歉，发生了错误！" + "\n" + e.toString());
        }
    }

    /**
     * 当销毁视图时发生，切换标签卡就会发生，在这里不宜解注广播
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ViewGroup) rootView.getParent()).removeView(rootView);
//        rootAct.unregisterReceiver(broadcastReceiver);
        Log.v(TAG,"摧毁");
    }

    /**
     * 显示toast
     * @param msg
     */
    public  void showToast(CharSequence msg)
    {
        if(toast==null)
        {
            toast=Toast.makeText(getContext(),msg,Toast.LENGTH_SHORT);
        }else
        {
            toast.setText(msg);
        }
        toast.show();
    }

    /**
     * 加载列表，有ui操作
     * @param path
     */
    @Override
    public void loadList(final File path,boolean isToTop) {
        fileListItems.clear();
        files =path.listFiles();
        Arrays.sort(files,new FileNameSort());

        if(!curPath.getAbsolutePath().equals("/"))
        {
            //添加返回上级目录项
            fileListItems.add(new FileListItem(application.folderBmp
                    , ".."
                    ,""
                    ,""));
        }
        for(int i = 0; i< files.length; i++)
        {
            String tempName=files[i].getName().toLowerCase();
            if(files[i].isDirectory()) {
                fileListItems.add(new FileListItem(application.folderBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,""));
            }
            else if(tempName.matches(".*.jpg|.*.png|.*.bmp|.*.gif$")) {
                fileListItems.add(new FileListItem(application.imageFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            } else if(tempName.matches(".*.mp4|.*.3gp|.*.flv|.*.wmv|.*.rmvb|.*.avi$")) {
                fileListItems.add(new FileListItem(application.videoFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.mp3|.*.amr|.*.ape|.*.flac|.*.ogg$")) {
                fileListItems.add(new FileListItem(application.audioFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.zip|.*.rar|.*.7z$")) {
                fileListItems.add(new FileListItem(application.achieveFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.xls|.*.xlsx$")) {
                fileListItems.add(new FileListItem(application.excelFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.doc|.*.docx$")) {
                fileListItems.add(new FileListItem(application.wordFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.ppt|.*.pptx$")) {
                fileListItems.add(new FileListItem(application.pptFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.apk$")) {
                Drawable icon = null;
                PackageInfo info = pm.getPackageArchiveInfo(files[i].getAbsolutePath(),
                        PackageManager.GET_ACTIVITIES);
                if (info != null) {
                    ApplicationInfo appInfo = info.applicationInfo;
                    appInfo.sourceDir = files[i].getAbsolutePath();
                    appInfo.publicSourceDir = files[i].getAbsolutePath();
                    try {
                        icon= appInfo.loadIcon(pm);
                    } catch (OutOfMemoryError e) {
                        Log.e("ApkIconLoader", e.toString());
                    }
                }
                Bitmap apkIcon=null;

                if(icon==null)
                {
                    apkIcon=application.apkFileBmp;
                }else {
                    apkIcon = ((BitmapDrawable) icon).getBitmap();
                }
                fileListItems.add(new FileListItem(apkIcon
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }
            else
            {
                fileListItems.add(new FileListItem(application.commonFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }
        }

//        copyProgressDialog.hide();
        rootAct.getSupportActionBar().setTitle(getShortPath(curPath.toString()));
        if(isToTop)
        {
            recyclerView.scrollToPosition(0);//跳到第一项显示
        }
    }

    /**
     * 获取缩略路径
     *
     * @param path
     * @return
     */
    public static String getShortPath (String path)
    {
        int n=0,p=0;
        for (int i=0;i < path.length();i++)
            if (path.charAt(i) == '/')
                ++n;

        if (n >= 3)
        {
            for (int i=0;i < path.length();i++)
            {
                if (path.charAt(i) == '/')
                    ++p;
                if (p == n - 1)
                {
                    String newPath = "..." + path.substring(i, path.length());
                    return newPath;
                }
            }
        }

        return path;
    }

    /***
     *格式化时间
     * @param time
     * @return
     */
    public String getFormatedDate(long time)
    {
        //String nowStr;
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date curDate = new Date(time);//获取当前时间
        return  sdf.format(curDate);//转换时间格式

    }

    /**
     * 格式化文件大小
     * @param fileS
     * @return
     */
    public static  String formetFileSize (long fileS)
    {
        //转换文件大小
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024)
        {
            fileSizeString = df.format((double) fileS) + "b";
            if (fileS == 0)
                fileSizeString = fileS + ".0b";
        }
        else if (fileS < 1048576)
        {
            fileSizeString = df.format((double) fileS / 1024) + "Kb";
        }
        else if (fileS < 1073741824)
        {
            fileSizeString = df.format((double) fileS / 1048576) + "Mb";
        }
        else
        {
            fileSizeString = df.format((double) fileS / 1073741824) + "Gb";
        }
        return fileSizeString;
    }
}
