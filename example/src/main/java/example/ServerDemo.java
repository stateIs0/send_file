package example;

import cn.thinkinjava.send.file.server.NioSendFileServer;

import java.io.IOException;

public class ServerDemo {

    public static void main(String[] args) throws IOException {
        NioSendFileServer nioSendFileServer = new NioSendFileServer();
        nioSendFileServer.start("localhost", 8083, "/Users/cxs/send_file_copy_dir");
    }
}
