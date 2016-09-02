package com.zyw.zhaochuan.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.zyw.zhaochuan.parser.FileListParser;
import com.zyw.zhaochuan.services.TcpService;
import com.zyw.zhaochuan.view.RecycleViewDivider;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 此类是显示远程的目录的Fragment
 * 属于SessionActivity
 * 深复制是为了缓存
 * Created by zyw on 2016/5/18.
 */
public class RemoteListFragment extends Fragment {
    private RecyclerView recyclerView;
    private FloatingActionButton pasteFloatButton;
    private Context context;
    private FileListAdapter fileListAdapter;
    private List<FileListItem> fileListItems;
    private ProgressDialog progressDialog;
    private  View rootView =null;
    public  static File curPath=null;
    private Toast toast;
   private FileListParser.RFile[] files;
    final String TAG="RemoteListFragment";
    public static FileListParser.RFile willSendFilePath;
    private SessionActivity rootAct;
    private  ThisApplication application;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=getContext();
        if(rootView ==null) {
            rootAct=SessionActivity.thiz;//放在这里
            application=(ThisApplication)rootAct.getApplication();
            //根据平台设置根目录
            curPath=new File(application.getFileRoot());
            rootView = inflater.inflate(R.layout.file_list_layout, null);
            recyclerView = (RecyclerView) rootView.findViewById(R.id.filelist_reclyclerview);
            pasteFloatButton=(FloatingActionButton)rootView.findViewById(R.id.float_button_paste);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            //添加分割线
            recyclerView.addItemDecoration(new RecycleViewDivider(context, LinearLayoutManager.VERTICAL));
            fileListItems = new ArrayList<FileListItem>();

            fileListAdapter = new FileListAdapter(context, fileListItems);
            //列表项单击事件
            fileListAdapter.setOnItemClickListener(new RecyclerViewEvents());
            //列表长按事件
            fileListAdapter.setOnItemLongClickListener(new RecyclerViewEvents());
            recyclerView.setAdapter(fileListAdapter);
            recyclerView.setFocusable(true);//这个和下面的这个命令必须要设置了，才能监听back事件。
            recyclerView.setFocusableInTouchMode(true);
            recyclerView.setOnKeyListener(backlistener);


            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(TcpService.NOTICE_TYPE_UPDATE_REMOTE_LIST);//注册收到更新列表通知
            intentFilter.addAction(rootAct.NOTICE_BACKKEY_PRESS);//收到返回键的消息
            SessionActivity.thiz.registerReceiver(broadcastReceiver,intentFilter);

            progressDialog=ProgressDialog.show(SessionActivity.thiz,"",getResources().getString(R.string.progress_bar_getting_list));
            progressDialog.setCancelable(true);
            //发送获取目录消息
            sendGetContentMsg(curPath.toString());

            //粘贴按钮
            pasteFloatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        //发送发送文件命令，传进去一个远程的目录+文件名
                        //本地到远程
                        if(application.isCopyFromLocal()) {
                            if (LocalListFragment.willSendFilePath != null) {
                                SessionActivity.tcpService.sendSendFileMsg(curPath.toString() + File.separator + LocalListFragment.willSendFilePath.getName());
                                SessionActivity.tcpService.sendFile(LocalListFragment.willSendFilePath);
                                LocalListFragment.willSendFilePath = null;//粘贴了就为空
                               // 此句不能加;sendGetContentMsg(curPath.getAbsolutePath());
                            }
                        }
                        //远程到远程
                        else
                        {
                            if(RemoteListFragment.willSendFilePath!=null)
                            {
                                SessionActivity.tcpService.sendCopyFileMsg(RemoteListFragment.willSendFilePath.getAbsolutePath()
                                        ,curPath.getAbsolutePath()+File.separator+RemoteListFragment.willSendFilePath.getName());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(SessionActivity.thiz,"粘贴失败",Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        rootAct.getSupportActionBar().setTitle(getShortPath(curPath.toString()));
        SessionActivity.isLocalFragment=false;
        return rootView;
    }



    private class RecyclerViewEvents implements FileListAdapter.OnRecyclerViewItemClickListener,FileListAdapter.OnRecyclerViewItemLongClickListener {

        /**
         * 列表单击事件
         * @param view
         * @param pos
         */
        @Override
        public void onItemClick(View view, int pos) {
            if (pos == 0 && !(curPath.toString().equals("/")))
            {
                goBack();
            }
            //点击文件夹时,这里要注意数组的范围
            else if ((pos == 0 && curPath.toString().equals("/")) || !files[pos-1].isFile())
            {
                try
                {
                    //对根目录的处理,为/时是电脑根目录
                    if (curPath.toString().equals("/"))
                    {
                        curPath =new File(files[pos].getAbsolutePath());
                    }
                    else
                    {
                        curPath =new File(files[pos-1].getAbsolutePath());
                    }

                    progressDialog=ProgressDialog.show(SessionActivity.thiz,"",getResources().getString(R.string.progress_bar_getting_list));
                    progressDialog.setCancelable(true);
                    sendGetContentMsg(curPath.getAbsolutePath());
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
            }

        }

        /**
         * 列表长按事件
         * @param view
         * @param pos
         */
        @Override
        public void onItemLongClick(View view, int pos) {
            FileListParser.RFile selectedPath=null;
             boolean isFileItem=false;
            if (pos == 0 && (!curPath.toString().equals("/")||!curPath.equals("")))
            {
                    return;
            }
            else if ((pos == 0 && (curPath.toString().equals("/")||curPath.toString().equals(""))) ||!files[pos-1].isFile()) {
                    selectedPath = files[pos-1];
                    isFileItem=false;
            }else if(files[pos-1].isFile()){
                    selectedPath = files[pos - 1];
                    isFileItem=true;
            }
                final FileListParser.RFile finalSelectedPath = selectedPath;
                final String[] menuArr= isFileItem? getResources().getStringArray(R.array.remote_context_menu_item_file):getResources().getStringArray(R.array.remote_context_menu_item_folder);
                Dialog dialog = new AlertDialog.Builder(rootAct)
                        .setItems(menuArr, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (menuArr[which]) {
                                    case "传输":
                                        try {
                                            SessionActivity.tcpService.sendGetFileMsg(finalSelectedPath.getAbsolutePath());
                                            SessionActivity.tcpService.setSaveFilePath(LocalListFragment.curPath.getAbsolutePath() + File.separator + finalSelectedPath.getName());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    case "复制":
                                            //复制,复制远程的了，本地的路径就被清空
                                            LocalListFragment.willSendFilePath = null;
                                            willSendFilePath = finalSelectedPath;
                                            Toast.makeText(SessionActivity.thiz, willSendFilePath.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                            application.setCopyFromLocal(false);//标记不是本地复制
                                        break;
                                    case "重命名":
                                        //重命名
                                        final EditText editText=new EditText(context);
                                        editText.setText(finalSelectedPath.getName());
                                        AlertDialog alertDialog=new AlertDialog.Builder(context)
                                                .setTitle("重命名")
                                                .setView(editText)
                                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        String text=editText.getText().toString();
                                                        if(!text.equals(""))
                                                        {
                                                            try {
                                                                SessionActivity.thiz.tcpService.sendRenameFileMsg(finalSelectedPath.getAbsolutePath(),new File(finalSelectedPath.getAbsolutePath()).getParent()+File.separator+text);
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }else
                                                        {
                                                            showToast("重命名失败");
                                                        }
                                                    }
                                                })
                                                .create();
                                        alertDialog.show();
                                        break;
                                    //删除
                                    case "删除":
                                        try {
                                            SessionActivity.thiz.tcpService.sendDeleteFileMsg(finalSelectedPath.getAbsolutePath());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                }
                            }
                        })
                        .create();
                dialog.show();

        }

    }

    /**
     * 向服务发送获取文件消息
     * @param path
     */
    private  void sendGetContentMsg(String path)
    {
       // 发送获取目录消息
        try {
            SessionActivity.tcpService.sendGetContentMsg(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 广播接收者，这里主要是为了更新UI(列表)
     */
    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                 if(intent.getAction().equals(TcpService.NOTICE_TYPE_UPDATE_REMOTE_LIST))
            {
                //当收到更新列表的通知
                String json=intent.getStringExtra("json");
                FileListParser fileListParser=new FileListParser(json);
                files=fileListParser.getFiles();
               loadList(null,false);
                fileListAdapter.notifyDataSetChanged();
                if(progressDialog!=null)
                progressDialog.hide();
            }else if(intent.getAction().equals(rootAct.NOTICE_BACKKEY_PRESS)){
                goBack();
            }


        }
    };

    /**
     * 返回上一级目录，有ui操作
     */
    public void goBack() {
        try
        {
            if(curPath.getAbsolutePath().equals("/"))
                return;
            progressDialog=ProgressDialog.show(SessionActivity.thiz,"",getResources().getString(R.string.progress_bar_getting_list));
            progressDialog.setCancelable(true);
            curPath=curPath.getParentFile();
            sendGetContentMsg(curPath.getAbsolutePath());

            SessionActivity.thiz.getSupportActionBar().setTitle(getShortPath(curPath.toString()));
        }
        catch (Exception e)
        {
            showToast("抱歉，发生了错误！" + "\n" + e.toString());
        }
    }

    /**
     * 当销毁视图时发生
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ViewGroup) rootView.getParent()).removeView(rootView);
        //rootAct.unregisterReceiver(broadcastReceiver);
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

    private View.OnKeyListener backlistener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int code, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                if (code == KeyEvent.KEYCODE_BACK) {  //表示按返回键 时的操作
                }
            }
            return false;
        }
    };
    /**
     * 加载列表，有ui操作
     * @param paths
     */
    public void loadList(FileListParser.RFile[] paths,boolean isToTop) {

        fileListItems.clear();//清空，必要

        if(!(curPath.toString().equals("/")||curPath.toString().equals("")))
        {
            //添加返回上级目录项
            fileListItems.add(new FileListItem(application.remoteFolderBmp
                    , ".."
                    ,""
                    ,""));
        }
        for(int i = 0; i< files.length; i++)
        {
            String tempName=files[i].getName().toLowerCase();
            if(!files[i].isFile()) {
                fileListItems.add(new FileListItem(application.remoteFolderBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,""));
            }
            else if(tempName.matches(".*.jpg|.*.png|.*.bmp|.*.gif$")) {
                fileListItems.add(new FileListItem(application.imageFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,formetFileSize(files[i].getLength())));
            } else if(tempName.matches(".*.mp4|.*.3gp|.*.flv|.*.wmv|.*.rmvb|.*.avi$")) {
                fileListItems.add(new FileListItem(application.videoFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,formetFileSize(files[i].getLength())));
            }else if(tempName.matches(".*.mp3|.*.amr|.*.ape|.*.flac|.*.ogg$")) {
                fileListItems.add(new FileListItem(application.audioFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,formetFileSize(files[i].getLength())));
            }else if(tempName.matches(".*.zip|.*.rar|.*.7z$")) {
                fileListItems.add(new FileListItem(application.achieveFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,formetFileSize(files[i].getLength())));
            }else if(tempName.matches(".*.xls|.*.xlsx$")) {
                fileListItems.add(new FileListItem(application.excelFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,formetFileSize(files[i].getLength())));
            }else if(tempName.matches(".*.doc|.*.docx$")) {
                fileListItems.add(new FileListItem(application.wordFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,formetFileSize(files[i].getLength())));
            }else if(tempName.matches(".*.ppt|.*.pptx$")) {
                fileListItems.add(new FileListItem(application.pptFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,formetFileSize(files[i].getLength())));
            }else if(tempName.matches(".*.apk$")) {
                fileListItems.add(new FileListItem(application.apkFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,formetFileSize(files[i].getLength())));
            }
            else
            {
                fileListItems.add(new FileListItem(application.commonFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].getLastModified())
                        ,formetFileSize(files[i].getLength())));
            }
        }

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

