package cn.cat.simple.thread.pool.policy.impl;

import cn.cat.simple.thread.pool.core.ThreadPool;
import cn.cat.simple.thread.pool.policy.RejectPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Cat
 * @description 调用者执行策略
 */
public class CallerRunsPolicy implements RejectPolicy<Runnable> {
    private static final Logger logger = LoggerFactory.getLogger(CallerRunsPolicy.class);

    @Override
    public void reject(ThreadPool pool, Runnable task) {
        if (!Thread.currentThread().isInterrupted()) {
            logger.info("拒绝策略触发==调用者执行策略=={}", task);
            task.run();
        }
    }
}
