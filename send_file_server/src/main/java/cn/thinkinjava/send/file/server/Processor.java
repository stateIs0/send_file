package cn.thinkinjava.send.file.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;

/**
 * 各种 IO 处理器.
 */
public interface Processor {

    void start() throws IOException;

    /**
     * io 事件注册.
     * @param channel 通道.
     * @throws ClosedChannelException
     */
    void register(SelectableChannel channel) throws ClosedChannelException;

    void stop();

}
