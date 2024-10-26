package cn.cat.simple.thread.pool.policy.impl;

import cn.cat.simple.thread.pool.core.ThreadPool;
import cn.cat.simple.thread.pool.policy.RejectPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Cat
 * @description 丢弃旧任务策略
 */
public class DiscardOldestPolicy implements RejectPolicy<Runnable> {
    private static final Logger logger = LoggerFactory.getLogger(DiscardOldestPolicy.class);

    @Override
    public void reject(ThreadPool pool, Runnable task) {
        logger.info("拒绝策略触发==丢弃旧任务策略=={}", task);
        pool.getWorkQueue().poll();
        pool.execute(task);
    }
}
