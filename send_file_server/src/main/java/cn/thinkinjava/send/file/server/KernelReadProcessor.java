package cn.thinkinjava.send.file.server;

import cn.thinkinjava.send.file.common.MagicNum;
import cn.thinkinjava.send.file.common.RpcPacket;
import cn.thinkinjava.send.file.common.SendResult;
import cn.thinkinjava.send.file.common.util.JackSonUtil;
import cn.thinkinjava.send.file.common.util.MemoryAllocator;
import cn.thinkinjava.send.file.common.util.SendFileNameThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 读事件处理器.
 */
class KernelReadProcessor implements Processor {

    private static Logger logger = LoggerFactory.getLogger(KernelReadProcessor.class);
    // 魔数. 4
    // id. 8
    // 文件名长度. 2
    // body 长度. 8
    private static final int FIX_LENGTH = 22;

    private AtomicBoolean running = new AtomicBoolean();
    private boolean force;
    private Set<SelectionKey> keys;
    private String baseDir;
    private Selector readSelector;
    private Map<SocketChannel, FileEntry> map = new ConcurrentHashMap<>();

    private ExecutorService execute = new ThreadPoolExecutor(1, 1, 60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1), new SendFileNameThreadFactory("R"));

    public KernelReadProcessor(String baseDir, Selector readSelector) throws IOException {
        this(false, baseDir, readSelector);
        if (baseDir != null) {
            File file = new File(baseDir);
            if (!file.exists()) {
                file.mkdirs();
            }
        } else {
            throw new RuntimeException("baseDir can not be null.");
        }
    }

    public KernelReadProcessor(boolean force, String baseDir, Selector readSelector) {
        this.force = force;
        this.baseDir = baseDir + "/";
        this.readSelector = readSelector;
    }

    @Override
    public void start() throws IOException {
        fireRead();
    }

    @Override
    public void register(SelectableChannel channel) throws ClosedChannelException {
        channel.register(readSelector, SelectionKey.OP_READ);
    }


    public void fireRead() throws IOException {
        running.set(true);

        execute.execute(() -> {
            try {
                while (running.get()) {
                    // TODO fix JDK Nio bug
                    readSelector.select(1000);
                    keys = readSelector.selectedKeys();
                    doRead();
                }
            } catch (IOException e) {
                logger.info(e.getMessage(), e);
            }
        });


    }

    private void doRead() throws IOException {
        Iterator<SelectionKey> iterator = keys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            // must remove. cpu 100%
            iterator.remove();
            SocketChannel socketChannel = (SocketChannel) key.channel();
            FileEntry fe = map.get(socketChannel);
            if (fe == null) {
                fe = new FileEntry();
                map.put(socketChannel, fe);
            }
            // 魔数. 4
            // id. 8
            // 文件名长度. 2
            // body 长度. 8
            if (fe.bodyLength == 0 && fe.nameLength == 0 && socketChannel.isOpen()) {

                ByteBuffer metaBuffer = MemoryAllocator.allocate(FIX_LENGTH);
                try {
                    int result = socketChannel.read(metaBuffer);
                    if (result == -1) {
                        // 表示客户端主动关闭连接.
                        socketChannel.socket().close();
                        key.cancel();
                        logger.warn("client close connection. result is -1, socket = {}", socketChannel.socket());
                    }
                    metaBuffer.flip();
                    if (!metaBuffer.hasRemaining()) {
                        // 无数据.
                        return;
                    }
                    int magic_num = metaBuffer.getInt();
                    if (magic_num != MagicNum.INSTANCE.getNum()) {
                        // 表示魔数,应该关闭.(可能导致 close_wait)
                        logger.warn("not match magic num, close socket.");
                        socketChannel.close();
                        return;
                    }
                    fe.id = metaBuffer.getLong();
                    fe.nameLength = metaBuffer.getShort();
                    fe.bodyLength = metaBuffer.getLong();
                } finally {
                    MemoryAllocator.recycle(metaBuffer);
                }
            } else {
                try {
                    long start = System.currentTimeMillis();
                    ByteBuffer nameBuffer = ByteBuffer.allocateDirect(fe.nameLength);
                    socketChannel.read(nameBuffer);
                    nameBuffer.flip();
                    byte[] nameArr = new byte[fe.nameLength];
                    nameBuffer.get(nameArr);
                    String fileName = new String(nameArr);
                    FileChannel fc = new RandomAccessFile(new File(baseDir + fileName), "rw").getChannel();
                    long alreadyWrite = 0;

                    try {
                        while (alreadyWrite < fe.bodyLength) {
                            alreadyWrite += fc.transferFrom(socketChannel, alreadyWrite, fe.bodyLength - alreadyWrite);
                        }
                        if (force) {
                            // 防止宕机采用
                            fc.force(true);
                        }
                    } finally {
                        fc.close();
                    }

                    long end = System.currentTimeMillis();
                    logger.info("send file transfer over, file size = {}, cost time = {}, id = {}", alreadyWrite, end - start, fe.id);
                    if (fe.id != -1) {

                        SendResult sr = new SendResult(true, alreadyWrite);
                        sr.setFileAddr(baseDir + fileName);

                        ReplyPacket packet = new ReplyPacket(socketChannel, new RpcPacket(fe.id, JackSonUtil.obj2ByteArray(sr)));

                        KernelWriteProcessor.queue.add(packet);
                    }

                } catch (ClosedChannelException e) {
                    logger.warn("client close connection.");
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
                fe.bodyLength = 0;
                fe.nameLength = 0;
                fe.id = -1;

            }

        }
    }


    @Override
    public void stop() {
        running.set(false);
        execute.shutdown();
    }
}
