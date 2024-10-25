package cn.cat.simple.thread.pool.factory;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadFactory implements ThreadFactory {
    /**
     * 原子序号，为每个线程池分配一个序号
     */
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    /**
     * 线程组
     */
    private final ThreadGroup group;
    /**
     * 每个线程来获取一个随机序号
     */
    private static final AtomicInteger threadNumber = new AtomicInteger(1);
    /**
     * 线程名称前缀，便于日志分辨
     */
    private final String namePrefix;

    public DefaultThreadFactory() {
        this.group = Thread.currentThread().getThreadGroup();
        this.namePrefix = "pool-" + poolNumber.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        // 检查并设置线程为用户线程
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        // 为线程设置默认的优先级
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}
