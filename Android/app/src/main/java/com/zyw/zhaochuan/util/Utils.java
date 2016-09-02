package com.zyw.zhaochuan.util;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import com.zyw.zhaochuan.entity.FileListItem;
import com.zyw.zhaochuan.interfaces.OnTransProgressChangeListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Created by zyw on 2016/5/5.
 * 工具类
 */
public class Utils {

    public static String[] chars = new String[] { "a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z" };


    /**
     * 复制文件，如果目标存在即覆盖
     * @param source
     * @param target
     * @param listener
     * @return 同一个路径返回false
     * @throws IOException
     */
    public static boolean copyFile(File source, File target, OnTransProgressChangeListener listener) throws IOException
    {
        if(target.toString().equals(source.toString()))
            return false;
        FileInputStream fileInputStream=new FileInputStream(source);
        FileOutputStream fileOutputStream=new FileOutputStream(target);

        byte[] buf=new byte[8192];
        int len=0;
        long readSize=0;//已读文件大小
        long max=source.length();
        while((len=fileInputStream.read(buf))!=-1)
        {
            readSize+=len;
            if(listener!=null)
                listener.onProgressChange(readSize, max);
            fileOutputStream.write(buf, 0,  len);
            fileOutputStream.flush();
        }
        fileInputStream.close();
        fileOutputStream.close();
        return true;
    }




    /**
     * 深度优先遍历
     * 删除文件夹下所有文件和文件夹
     * @param file
     */
    public static void deleteFile(File file)
    {
        if(file.isFile()) {
            file.delete();
            return;
        }
        File[] fs=file.listFiles();

        for (File f:fs)
        {
            if (f.isFile())
            {
                f.delete();
            }
            else{
                deleteFile(f);
                f.delete();
            }

        }
        file.delete();
    }

    /**
     * 生成随机的Key
     * @param len
     */
    public  static  String getFileKey(int len)
    {
        StringBuilder sb=new StringBuilder();
        Random rd=new Random();
        for(int i=0;i<len;i++)
        {
            sb.append(chars[rd.nextInt(chars.length)]);
        }

        return sb.toString();
    }

    /**
     *列表深复制
     * @param list1
     * @param list2
     */
    public static void listCopy(List<FileListItem> list1, List<FileListItem> list2)
    {
        list1.clear();
        for (int i=0;i<list2.size();i++)
        {
            list1.add(list2.get(i).clone());
        }
    }

    /**
     * 生成uuid
     * @return
     */
    private static String generateShortUuid() {
        StringBuffer shortBuffer = new StringBuffer();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            shortBuffer.append(chars[x % 0x3E]);
        }
        return shortBuffer.toString();

    }

    /**
     * 将字符串型ip转成int型ip
     * @param strIp
     * @return
     */
    public static int ip2Int(String strIp){
        String[] ss = strIp.split("\\.");
        if(ss.length != 4){
            return 0;
        }
        byte[] bytes = new byte[ss.length];
        for(int i = 0; i < bytes.length; i++){
            bytes[i] = (byte) Integer.parseInt(ss[i]);
        }
        return byte2Int(bytes);
    }
    /**
     * 将int型ip转成String型ip
     * @param intIp
     * @return
     */
    public static String int2Ip(int intIp){
        byte[] bytes = int2byte(intIp);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 4; i++){
            sb.append(bytes[i] & 0xFF);
            if(i < 3){
                sb.append(".");
            }
        }
        return sb.toString();
    }

    private static byte[] int2byte(int i) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (0xff & i);
        bytes[1] = (byte) ((0xff00 & i) >> 8);
        bytes[2] = (byte) ((0xff0000 & i) >> 16);
        bytes[3] = (byte) ((0xff000000 & i) >> 24);
        return bytes;
    }
    private static int byte2Int(byte[] bytes) {
        int n = bytes[0] & 0xFF;
        n |= ((bytes[1] << 8) & 0xFF00);
        n |= ((bytes[2] << 16) & 0xFF0000);
        n |= ((bytes[3] << 24) & 0xFF000000);
        return n;
    }

    /**
     * 获取GPRS数据时的ip
     * @return
     */
    public static String getIpAddress(WifiManager wifiManager) {
       if(wifiManager!=null)
       {
           if(wifiManager.isWifiEnabled()) {
               return int2Ip(wifiManager.getConnectionInfo().getIpAddress());
           }else
           {
               return  int2Ip(wifiManager.getDhcpInfo().serverAddress);
           }
       }
        return null;
    }

    /**
     * 返回随机ssid
     * @return
     */
    public static String getRandomSSID()
    {

     return generateShortUuid();
    }

    /***
     *格式化时间
     * @param time
     * @return
     */
    public static String getFormatedDate(long time)
    {
        //String nowStr;
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date curDate = new Date(time);//获取当前时间
        return  sdf.format(curDate);//转换时间格式

    }

    public  static  void writeLogToSdcard(String log)
    {
        try {
            FileWriter fw =new FileWriter("/sdcard/zhaochuan.txt",true);
            fw.write("\n\n");
            fw.write(getFormatedDate(System.currentTimeMillis())+"\n");
            fw.write(log);
            if(fw!=null)
                fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成随机密码
     * @return
     */
    public static String getRandomKey()
    {

        return generateShortUuid();
    }
}
