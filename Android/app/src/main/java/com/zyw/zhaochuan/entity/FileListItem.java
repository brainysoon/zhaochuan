package com.zyw.zhaochuan.entity;

import android.graphics.Bitmap;

/**
 * Created by zyw on 2016/5/18.
 */
public class FileListItem implements Cloneable {
    public Bitmap getIcon() {
        return icon;
    }

    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(String modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public FileListItem(Bitmap icon, String fileName, String modifyTime, String fileSize) {
        this.icon = icon;
        this.fileName = fileName;
        this.modifyTime = modifyTime;
        this.fileSize = fileSize;
    }

    /**
     * 深复制
     * @return
     */
    @Override
    public FileListItem clone() {
        FileListItem o = null;
        try {
            o = (FileListItem) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return o;
    }

    private Bitmap icon;
    private String fileName;
    private String modifyTime;
    private  String fileSize;

}
