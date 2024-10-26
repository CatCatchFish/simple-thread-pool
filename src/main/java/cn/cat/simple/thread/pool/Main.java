package cn.cat.simple.thread.pool;

import cn.cat.simple.thread.pool.core.ThreadPool;
import cn.cat.simple.thread.pool.core.WorkQueue;
import cn.cat.simple.thread.pool.factory.Configuration;
import cn.cat.simple.thread.pool.factory.DefaultThreadFactory;
import cn.cat.simple.thread.pool.factory.ThreadFactory;
import cn.cat.simple.thread.pool.policy.impl.DiscardOldestPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 创建配置类
        Configuration configuration = new Configuration(
                2,
                5,
                5L,
                TimeUnit.SECONDS,
                new DiscardOldestPolicy()
        );

        ThreadFactory threadFactory = new DefaultThreadFactory();
        // 初始化线程池
        ThreadPool threadPool = new ThreadPool(configuration, new WorkQueue<>(5), threadFactory);

        threadPool.setAllowCoreThreadTimeOut(true);
        for (int i = 0; i < 15; i++) {
            threadPool.execute(() -> {
                logger.info("执行任务------->当前执行线程为" + Thread.currentThread().toString());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
