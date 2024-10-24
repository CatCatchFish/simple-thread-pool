package cn.cat.simple.thread.pool.test;

import cn.cat.simple.thread.pool.core.ThreadPool;
import cn.cat.simple.thread.pool.core.WorkQueue;
import cn.cat.simple.thread.pool.factory.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class AppTest {
    private ThreadPool threadPool;

    @Before
    public void def() {
        Configuration configuration = new Configuration(2, 3, 5L, TimeUnit.SECONDS);
        threadPool = new ThreadPool(configuration, new WorkQueue<>(5));
    }

    @Test
    public void test() {
        // 创建配置对象

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
