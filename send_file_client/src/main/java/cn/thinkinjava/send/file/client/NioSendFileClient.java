package cn.thinkinjava.send.file.client;

import cn.thinkinjava.send.file.common.MagicNum;
import cn.thinkinjava.send.file.common.PacketCodec;
import cn.thinkinjava.send.file.common.RpcPacket;
import cn.thinkinjava.send.file.common.SendResult;
import cn.thinkinjava.send.file.common.util.JackSonUtil;
import cn.thinkinjava.send.file.common.util.SendFileNameThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 NIO 的 sendFile client . 线程安全.
 */
public class NioSendFileClient implements SendFileClient {

    private static AtomicLong rpcId = new AtomicLong();
    private static Logger logger = LoggerFactory.getLogger(NioSendFileClient.class);
    private static final int fix_num = 4 + 2 + 8 + 8;
    private static Map<Long, CountDownLatch> requestMap = new ConcurrentHashMap<>();
    private static Map<Long, SendResult> resultMap = new ConcurrentHashMap<>();

    private String host = "localhost";
    private int port = 8082;
    private AtomicBoolean running = new AtomicBoolean();
    private SocketChannel socketChannel;
    private Selector readSelector;

    private ExecutorService execute = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(), new SendFileNameThreadFactory("client_read"));

    @Override
    public void start(String address, int port) throws IOException {
        running.set(true);
        try {

            SocketAddress sad = new InetSocketAddress(host, port);
            socketChannel = SocketChannel.open();
            // 连接
            socketChannel.connect(sad);
            // tcp 优化.
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.FALSE);
            // 非阻塞
            socketChannel.configureBlocking(false);

            processReadKeys();
            logger.info("send file client start success. socket = {}", socketChannel.socket());
        } catch (Exception e) {
            running.set(false);
            throw e;
        }
    }

    private void processReadKeys() {
        execute.execute(new Runnable() {
            @Override
            public void run() {
                while (running.get()) {
                    try {
                        Thread.sleep(10);
                        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                        if (socketChannel.isOpen()) {
                            socketChannel.read(byteBuffer);
                        } else {
                            break;
                        }

                        if (!byteBuffer.hasRemaining()) {
                            continue;
                        }

                        byteBuffer.flip();
                        RpcPacket packet = PacketCodec.decode(byteBuffer);
                        if (packet == null) {
                            continue;
                        }

                        resultMap.put(packet.getId(),
                                JackSonUtil.string2Obj(new String(packet.getContent()), SendResult.class));

                        CountDownLatch countDownLatch = requestMap.get(packet.getId());
                        if (countDownLatch == null) {
                            logger.error("CountDownLatch is null, id = {}", packet.getId());
                            throw new RuntimeException("CountDownLatch is null.");
                        }
                        countDownLatch.countDown();
                        requestMap.remove(packet.getId());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                logger.warn("Client Read Thread exit.");
            }
        });
    }

    @Override
    public void shutdown() {
        try {
            logger.info("shutdown success. socket = {}", socketChannel);
            running.set(false);
            execute.shutdown();
            socketChannel.close();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public SendResult sendFile(File file) throws IOException {
        long id = rpcId.incrementAndGet();
        send0(file, id);
        CountDownLatch latch = new CountDownLatch(1);
        requestMap.put(id, latch);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SendResult sr = resultMap.get(id);
        resultMap.remove(id);
        return sr;
    }

    @Override
    public SendResult sendFileOneWay(File file) throws IOException {
        return send0(file, -1);
    }

    private synchronized SendResult send0(File file, long id) throws IOException {

        if (file == null) {
            throw new RuntimeException("file is empty");
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("file can not be dir.");
        }
        long start = System.currentTimeMillis();
        FileChannel fc = new RandomAccessFile(file, "rw").getChannel();
        try {
            byte[] nameContent = file.getName().getBytes();
            long bodyLength = file.length();
            // 魔数
            // id 8
            // 文件名长度.
            // body 长度.
            // 以上固定 14 字节.
            // 然后写入 文件名称.
            // 再然后写入文件内容.
            ByteBuffer bb = ByteBuffer.allocateDirect((fix_num + nameContent.length));
            bb.putInt(MagicNum.INSTANCE.getNum());
            bb.putLong(id);
            bb.putShort((short) nameContent.length);
            bb.putLong(bodyLength);
            bb.put(nameContent);

            bb.flip();
            // 写入元信息.
            while (bb.hasRemaining()) {
                socketChannel.write(bb);
            }
            // 写入文件内容
            long alreadySend = 0L;
            do {
                // 这里怕网卡写满导致的丢弃.做些累加进行判断.
                alreadySend += fc.transferTo(alreadySend, fc.size() - alreadySend, socketChannel);
            } while (alreadySend < fc.size());
            bb.clear();
            long end = System.currentTimeMillis();
            logger.info("send file {{}} success, size = {}, cost time = {}", file.getName(), file.length(), end - start);
            return new SendResult(true, alreadySend);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw e;
        }finally {
            fc.close();
        }

    }
}
