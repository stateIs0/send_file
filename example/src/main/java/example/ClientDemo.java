package example;

import cn.thinkinjava.send.file.client.NioSendFileClient;
import cn.thinkinjava.send.file.client.SendFileClient;
import cn.thinkinjava.send.file.common.SendResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientDemo {
    static int total = 0;

    static List<File> list = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        File files = new File("/Users/cxs/doc/百度云");
        File file2 = new File("/Users/cxs/doc/经典书籍");
        addFile(files);
        addFile(file2);

        for (int i = 0; i < 3; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SendFileClient sendFileClient = new NioSendFileClient();
                    long start = 0L;
                    try {
                        sendFileClient.start("localhost", 8083);
                        start = System.currentTimeMillis();
                        int single = 0;
                        for (File file : list) {
                            if (file.isDirectory()) {
                                continue;
                            }
                            SendResult sr = sendFileClient.sendFile(file);
                            System.out.println("single = " + ++single);
                            System.out.println("total = " + ++total);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendFileClient.shutdown();
                    } finally {
                        long end = System.currentTimeMillis();
                        System.err.println("single cost = " + (end - start));
                        sendFileClient.shutdown();
                    }
                }
            }).start();


        }

    }

    public static void addFile(File file) {
        if (file.isDirectory()) {
            for (File listFile : file.listFiles()) {
                addFile(listFile);
            }
        } else {
            list.add(file);
        }
    }
}
