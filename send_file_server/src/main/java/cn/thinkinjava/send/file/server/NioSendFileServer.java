package cn.thinkinjava.send.file.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 NIO 的 sendFile 服务端.
 */
public class NioSendFileServer implements SendFileServer {

    private int rnext = 0;
    private int wnext = 0;

    private static Logger logger = LoggerFactory.getLogger(NioSendFileServer.class);

    private KernelReadProcessor[] worker = new KernelReadProcessor[4];
    private KernelWriteProcessor[] writers = new KernelWriteProcessor[4];
    private KernelAcceptProcessor acceptProcessor;
    private AtomicBoolean running = new AtomicBoolean();
    private ServerSocketChannel serverSocketChannel;

    private KernelReadProcessor nextKernelReadProcessor() {
        return worker[Math.abs(++rnext) % worker.length];
    }

    private KernelWriteProcessor nextKernelWriteProcessor() {
        return writers[Math.abs(++wnext) % writers.length];
    }

    @Override
    public void start(String address, int port, String baseDir) throws IOException {
        try {
            if (running.get()) {
                throw new RuntimeException("send file server already running.");
            }
            running.set(true);

            for (int i = 0; i < worker.length; i++) {
                worker[i] = new KernelReadProcessor(baseDir, Selector.open());
                worker[i].start();
            }

            for (int i = 0; i < writers.length; i++) {
                writers[i] = new KernelWriteProcessor(Selector.open());
                writers[i].start();
            }

            doStart(address, port);
        } catch (Exception e) {
            running.set(false);
            throw e;
        }
    }


    private void doStart(String address, int port) throws IOException {
        SelectionKey key = null;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(address, port));
        Selector acceptSelector = Selector.open();

        acceptProcessor = new KernelAcceptProcessor(acceptSelector);

        acceptProcessor.register(serverSocketChannel);

        logger.info("send file server start success, server info = {}", serverSocketChannel.socket());

        while (running.get()) {
            try {
                acceptSelector.select();
                Iterator<SelectionKey> selectedKeys = acceptSelector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    key = selectedKeys.next();
                    selectedKeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);

                        nextKernelWriteProcessor().register(socketChannel);

                        nextKernelReadProcessor().register(socketChannel);

                        logger.info("accept a new socket {}", socketChannel.socket());
                    }
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                if (key != null) {
                    try {
                        key.channel().close();
                        key.cancel();
                    } catch (IOException ex) {
                        logger.warn(ex.getMessage(), ex);
                    }
                }
            }
        }
    }

    @Override
    public void shutdown() {
        for (int i = 0; i < worker.length; i++) {
            worker[i].stop();
            writers[i].stop();
        }
        try {
            logger.info("send file server stop success. server socket = {}", serverSocketChannel.socket());
            serverSocketChannel.close();
        } catch (IOException e) {
            // ignore.
        }
        running.set(false);
    }
}
