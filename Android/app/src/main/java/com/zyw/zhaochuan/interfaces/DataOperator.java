package com.zyw.zhaochuan.interfaces;

/**
 * Created by zyw on 2016/5/27.
 */
import java.io.File;
import java.io.IOException;

/**
 * 数据操作接口，TcpService实现它
 */
public interface DataOperator {

    /**
     * 给远程发送获取目录命令
     * @param path
     * @throws IOException
     */
    public void sendGetContentMsg(String path) throws IOException;

    /**
     * 发送获取文件命令，远端收到该命令就给本地发送文件
     * @throws IOException
     */
    public void sendGetFileMsg(String remoteFile) throws IOException;

    /**
     * 给远程发送发送文件命令，对方收到这个命令就可以知道它自己收到该存文件在哪了
     * @param path
     * @throws IOException
     */
    public void sendSendFileMsg(String path) throws IOException;
    /**
     * 给远程发送重命名命令
     * @param path 原文件的完整路径
     * @param newPath 新文件的路径
     * @throws IOException
     */
    public void sendRenameFileMsg(String path,String newPath) throws IOException;
    /**
     * 给远程发送删除文件命令
     * @param path
     * @throws IOException
     */
    public void sendDeleteFileMsg(String path) throws IOException;
    /**
     * 给远程发送目录
     * @throws IOException
     */
    public void sendContent() throws IOException;
    /**
     * 从流中发送文件到本地
     * @param file
     * @throws IOException
     */
    public void saveFile(File file) throws IOException;
    /**
     * 发送文件到远程路径
     * @param file 本地文件
     * @throws IOException
     */
    public void sendFile(File file) throws IOException;

    /**
     * 发送复制文件命令，同端之间
     * @param sourceFile 要复制的文件
     * @param targetFile 目标文件
     */
    public void sendCopyFileMsg(String sourceFile, String targetFile);

    /**
     * 从流中读取数据
     * @return
     * @throws IOException
     */
    public String readCommand() throws IOException;

    /**
     * 通知客户端已连接
     */
    public  void noticeClientConnected();

    /**
     * 发送断开连接命令
     */
    public void sendDisconnectMsg();

    /**
     * 发送连接请求
     */
    public void sendConnectedMsg() throws IOException;

}
