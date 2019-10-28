package cn.thinkinjava.send.file.server;


import org.junit.Test;

import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;

class SendFileServerTest {

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 3; i++) {
            System.out.println(SelectorProvider.provider().openSelector());
        }
    }

    @Test
    public void start() {

    }


    void stop() {
    }
}