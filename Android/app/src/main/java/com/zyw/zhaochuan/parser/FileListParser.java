package com.zyw.zhaochuan.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;

/**
 * Created by zyw on 2016/6/6.
 */
public class FileListParser {

    private  String json;
    private  String cmd;
    private  String root;
    private  RFile[]  files;
    private JSONObject obj;
    public FileListParser(String json) {
        this.json = json;
        try {
            obj=new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public String getCmd() {
        try {
            cmd=obj.getString("command");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return cmd;
    }
    public String getRoot() {
        try {
            cmd=obj.getString("root");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return root;
    }

    public RFile[] getFiles() {
        try {
            JSONArray jsonArray=obj.getJSONArray("files");
            int len=jsonArray.length();
            files=new RFile[len];
            for(int i=0;i<len;i++)
            {
                JSONObject tempFile=jsonArray.getJSONObject(i);

                    files[i] = new RFile(obj.getString("root")
                            , tempFile.getString("name")
                            , tempFile.getInt("length")
                            , tempFile.getBoolean("isFile")
                            , jsonArray.getJSONObject(i).getLong("lastModified"));
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        return files;
    }

    /**
     * 远程文件结构
     */
   public  class RFile
    {
        private String path;//文件路径
        private String name;//文件名
        private  long length;//文件长度
        private boolean isFile;//是否是文件
        private long lastModified;//最后修改时间
        public RFile(String path,String name, long length, boolean isFile,long lastModified) {
            this.name = name;
            this.path = path;
            this.length = length;
            this.isFile = isFile;
            this.lastModified=lastModified;
        }

        public String getAbsolutePath()
        {
            return path+File.separator+name;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public long getLength() {
            return length;
        }

        public boolean isFile() {
            return isFile;
        }

        public long getLastModified() {
            return lastModified;
        }
    }

}
