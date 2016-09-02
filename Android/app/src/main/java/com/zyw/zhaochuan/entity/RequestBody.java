package com.zyw.zhaochuan.entity;

/**
 * Created by zyw on 2016/5/30.
 */
public class RequestBody {
    public RequestBody(String cmd, String path) {
        this.cmd = cmd;
        this.path = path;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private String cmd;
    private String path;
}
