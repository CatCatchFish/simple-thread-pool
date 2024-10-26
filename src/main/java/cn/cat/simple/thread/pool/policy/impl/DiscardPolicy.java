package cn.cat.simple.thread.pool.policy.impl;

import cn.cat.simple.thread.pool.core.ThreadPool;
import cn.cat.simple.thread.pool.policy.RejectPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Cat
 * @description 丢弃任务的策略
 */
public class DiscardPolicy implements RejectPolicy<Runnable> {
    private static final Logger logger = LoggerFactory.getLogger(DiscardPolicy.class);

    @Override
    public void reject(ThreadPool pool, Runnable task) {
        // do nothing
        logger.info("拒绝策略触发==丢弃策略=={}", task);
    }
}
