package com.zyw.zhaochuan.entity;

import java.io.Serializable;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BmobFile;

/**
 * Created by zyw on 2016/7/24.
 */
public class FileSessionList extends BmobObject implements Serializable {


    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public void setFile(BmobFile file) {
        this.file = file;
    }

    public BmobFile getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getFileKey() {
        return fileKey;
    }

    private BmobFile file;
    private  String fileName;
    private  String fileSize;
    private  String fileKey;

}
