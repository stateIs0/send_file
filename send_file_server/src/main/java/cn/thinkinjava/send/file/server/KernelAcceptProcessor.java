package cn.thinkinjava.send.file.server;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * accept 事件处理器.
 */
class KernelAcceptProcessor implements Processor {

    private Selector selector;

    public KernelAcceptProcessor(Selector selector) {
        this.selector = selector;
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
