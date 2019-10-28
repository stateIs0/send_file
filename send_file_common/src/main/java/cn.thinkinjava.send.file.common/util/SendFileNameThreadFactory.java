package cn.thinkinjava.send.file.common.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程 factory..
 */
public class SendFileNameThreadFactory implements ThreadFactory {

    private static AtomicInteger poolNumber = new AtomicInteger();

    private AtomicInteger threadNumber = new AtomicInteger();
    private String name;

    public SendFileNameThreadFactory(String name) {
        poolNumber.incrementAndGet();
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        SecurityManager s = System.getSecurityManager();
        ThreadGroup group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        String namePrefix = "sendfile_" + name + "_" + poolNumber.get() + "-";
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
