package cn.thinkinjava.send.file.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.*;

/**
 * accept 事件处理器.
 */
class KernelAcceptProcessor implements Processor {

    private static Logger logger = LoggerFactory.getLogger(KernelAcceptProcessor.class);

    private Selector selector;

    public KernelAcceptProcessor(Selector selector) {
        this.selector = selector;
    }

    public void accept(ServerSocketChannel serverChannel) throws IOException {
        SocketChannel socketChannel = serverChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        logger.info("accept a new socket {}", socketChannel.socket());
    }

    @Override
    public void start() {

    }

    @Override
    public void register(SelectableChannel channel) throws ClosedChannelException {
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void stop() {
        //noop
    }
}
