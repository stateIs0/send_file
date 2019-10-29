package cn.thinkinjava.send.file.server;

import java.io.IOException;

/**
 * sendFile server .
 */
public interface SendFileServer {

    /**
     * 启动 sendFile 服务器.
     *
     * @param address 服务器地址.
     * @param port    端口.
     * @param baseDir 存储的目录. 不存在时会自动创建.
     * @throws IOException
     */
    void start(String address, int port, String baseDir) throws IOException;

    /**
     * 停止服务器.
     */
    void shutdown();


}
