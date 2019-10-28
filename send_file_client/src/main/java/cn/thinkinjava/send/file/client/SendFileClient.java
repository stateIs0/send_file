package cn.thinkinjava.send.file.client;

import cn.thinkinjava.send.file.common.SendResult;

import java.io.File;
import java.io.IOException;

public interface SendFileClient {

    /**
     * 启动客户端.包括连接服务器.
     *
     * @param address
     * @param port
     * @throws IOException 可能连不上.
     */
    void start(String address, int port) throws IOException;

    /**
     * 关闭客户端.会通知服务器.
     */
    void shutdown();

    /**
     * 使用 sendfile 多路复用同步发送文件到目标服务器. 且等待服务器回应成功.
     *
     * @param file 不能为空,不能是目录.
     * @return 发送结果 {@link SendResult}
     * @throws IOException 可能发生 io 异常.
     */
    SendResult sendFile(File file) throws IOException;

    /**
     * 使用 sendfile 多路复用发送文件到目标服务器.
     * 注意: 只发送到网卡.不接受回应.
     * 优点: 速度快.
     * 缺点: 可能导致文件丢失.因为没有 ack.
     *
     * @param file 不能为空,不能是目录.
     * @return 发送结果 {@link SendResult}
     * @throws IOException 可能发生 io 异常.
     */
    SendResult sendFileOneWay(File file) throws IOException;
}
