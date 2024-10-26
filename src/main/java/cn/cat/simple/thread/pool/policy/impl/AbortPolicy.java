package cn.cat.simple.thread.pool.policy.impl;

import cn.cat.simple.thread.pool.core.ThreadPool;
import cn.cat.simple.thread.pool.policy.RejectPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Cat
 * @description 中止策略(JDK默认的拒绝策略)，直接抛出异常
 */
public class AbortPolicy implements RejectPolicy<Runnable> {
    private static final Logger logger = LoggerFactory.getLogger(AbortPolicy.class);

    @Override
    public void reject(ThreadPool pool, Runnable task) {
        logger.warn("拒绝策略触发==中止策略=={}", task);
        throw new RuntimeException("Task " + task.toString() + " rejected from " + pool);
    }
}
