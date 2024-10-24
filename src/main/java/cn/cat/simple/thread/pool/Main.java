package cn.cat.simple.thread.pool;

import cn.cat.simple.thread.pool.core.ThreadPool;
import cn.cat.simple.thread.pool.core.WorkQueue;
import cn.cat.simple.thread.pool.factory.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 创建配置类
        Configuration configuration = new Configuration(2, 3, 5L, TimeUnit.SECONDS,
                (queue, task) -> {
                    logger.info("任务{}被丢弃", task);
                });

        // 初始化线程池
        ThreadPool threadPool = new ThreadPool(configuration, new WorkQueue<>(5));

        for (int i = 0; i < 4; i++) {
            threadPool.execute(() -> {
                System.out.println("执行任务------->当前执行线程为" + Thread.currentThread().toString());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
