package cn.cat.simple.thread.pool;

import cn.cat.simple.thread.pool.core.ThreadPool;
import cn.cat.simple.thread.pool.core.WorkQueue;
import cn.cat.simple.thread.pool.factory.Configuration;
import cn.cat.simple.thread.pool.factory.DefaultThreadFactory;
import cn.cat.simple.thread.pool.factory.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 创建配置类
        Configuration configuration = new Configuration(2, 5, 5L, TimeUnit.SECONDS,
                (queue, task) -> {
                    logger.info("拒绝策略====》拒绝策略触发，直接丢弃当前任务");
                });

        ThreadFactory threadFactory = new DefaultThreadFactory();
        // 初始化线程池
        ThreadPool threadPool = new ThreadPool(configuration, new WorkQueue<>(5), threadFactory);
        threadPool.setAllowCoreThreadTimeOut(false);

        for (int i = 0; i < 15; i++) {
            int finalI = i;
            threadPool.execute(() -> {
                logger.info("执行任务{}------->当前执行线程为{}", finalI, Thread.currentThread().toString());
            });
        }
        // 1.直接关闭线程池 任务不会全部执行完毕
        // threadPool.shutdownNow();
        // 2.等待线程池中任务执行完毕
        threadPool.shutdown();
    }
}
