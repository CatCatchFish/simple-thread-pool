package cn.cat.simple.thread.pool.policy;

import cn.cat.simple.thread.pool.core.ThreadPool;

/**
 * 拒绝策略接口
 *
 * @param <T>
 */
public interface RejectPolicy<T> {
    /**
     * 拒绝策略
     *
     * @param pool  线程池
     * @param task  被拒绝的任务
     */
    void reject(ThreadPool pool, T task);
}
