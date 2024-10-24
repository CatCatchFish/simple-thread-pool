package cn.cat.simple.thread.pool.policy;

import cn.cat.simple.thread.pool.core.WorkQueue;

/**
 * 拒绝策略接口
 *
 * @param <T>
 */
public interface RejectPolicy<T> {
    /**
     * 拒绝策略
     *
     * @param queue 当前任务队列
     * @param task  被拒绝的任务
     */
    void reject(WorkQueue<T> queue, T task);
}
