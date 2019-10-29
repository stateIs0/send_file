package cn.thinkinjava.send.file.server;

import cn.thinkinjava.send.file.common.PacketCodec;
import cn.thinkinjava.send.file.common.RpcPacket;
import cn.thinkinjava.send.file.common.util.SendFileNameThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 写事件处理器.
 */
class KernelWriteProcessor implements Processor {

    private static Logger logger = LoggerFactory.getLogger(KernelWriteProcessor.class);

    public static LinkedBlockingQueue<ReplyPacket> queue = new LinkedBlockingQueue<>();

    private AtomicBoolean running = new AtomicBoolean();
    private ExecutorService execute = new ThreadPoolExecutor(1, 1, 60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1), new SendFileNameThreadFactory("W"));



    @Override
    public void start() {
        running.set(true);
        execute.execute(new Runnable() {
            @Override
            public void run() {
                while (running.get()) {
                    try {
                        ReplyPacket packet = queue.take();

                        SocketChannel socketChannel = packet.getSocketChannel();
                        RpcPacket rpcPacket = packet.getRpcPacket();

                        ByteBuffer byteBuffer = PacketCodec.encode(rpcPacket);
                        byteBuffer.flip();

                        socketChannel.write(byteBuffer);
                        logger.info("write success RpcPacket = {}", rpcPacket);
                        byteBuffer.clear();

                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void register(SelectableChannel channel) throws ClosedChannelException {
    }

    @Override
    public void stop() {
        running.set(false);
        execute.shutdown();
    }
}
