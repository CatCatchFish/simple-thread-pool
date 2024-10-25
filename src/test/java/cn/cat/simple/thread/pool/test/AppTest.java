package cn.cat.simple.thread.pool.test;

import cn.cat.simple.thread.pool.core.ThreadPool;
import cn.cat.simple.thread.pool.core.WorkQueue;
import cn.cat.simple.thread.pool.factory.Configuration;
import cn.cat.simple.thread.pool.factory.DefaultThreadFactory;
import cn.cat.simple.thread.pool.factory.ThreadFactory;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AppTest {
    private static final Logger logger = LoggerFactory.getLogger(AppTest.class);
    private ThreadPool threadPool;

    @Before
    public void def() {
        Configuration configuration = new Configuration(2, 3, 5L, TimeUnit.SECONDS,
                (queue, task) -> {
                    logger.info("任务{}被丢弃", task);
                });
        ThreadFactory threadFactory = new DefaultThreadFactory();
        // 初始化线程池
        threadPool = new ThreadPool(configuration, new WorkQueue<>(5), threadFactory);
    }

    @Test
    public void test() throws InterruptedException {
        // 创建配置对象

        for (int i = 0; i < 10; i++) {
            threadPool.execute(() -> {
                System.out.println("执行任务------->当前执行线程为" + Thread.currentThread().toString());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        new CountDownLatch(1).await();
    }
}
