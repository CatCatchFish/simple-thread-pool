package cn.cat.simple.thread.pool.core;

import cn.cat.simple.thread.pool.policy.RejectPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class WorkQueue<T> {
    private static final Logger logger = LoggerFactory.getLogger(WorkQueue.class);
    // 双端队列
    private Deque<T> deque = new ArrayDeque<>();
    // 队列容量
    private int size;
    // 可重入锁
    private ReentrantLock lock = new ReentrantLock();

    // 队列满的条件
    private Condition fullCondition = lock.newCondition();
    // 队列可用条件
    private Condition emptyCondition = lock.newCondition();

    public WorkQueue(int size) {
        this.size = size;
    }

    // 添加任务，阻塞添加
    public void put(T task) {
        // 1.上锁
        lock.lock();
        try {
            // 2.检查队列是否已满
            while (size == deque.size()) {
                // 标志队列已满，等待消费者消费
                fullCondition.await();
            }
            logger.info("任务添加成功: " + task);
            deque.addLast(task);
            emptyCondition.signal();
        } catch (InterruptedException e) {
            logger.error("任务添加失败", e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    // 获取任务 阻塞获取
    public T take() {
        // 1.上锁
        lock.lock();
        try {
            // 2.首先检查队列是否存在元素
            while (deque.isEmpty()) {
                // 等待队列为空，等待生产者生产
                emptyCondition.await();
            } // 3.拿取元素
            T task = deque.removeFirst();
            logger.info("线程{}任务拿取成功", Thread.currentThread());
            // 4.唤醒挂起的生产者
            fullCondition.signal();
            return task;
        } catch (InterruptedException e) {
            logger.error("take task error", e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    // 非堵塞添加任务
    public Boolean offer(T task) {
        // 1.上锁
        lock.lock();
        try {
            if (size == deque.size()) {
                return false;
            }
            // 3.将任务存入队列中
            deque.addLast(task);
            logger.info("等待队列====》任务添加成功");
            // 4.唤醒挂起的消费者
            emptyCondition.signal();
            return true;
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    // 带超时时间阻塞添加
    public boolean offer(T task, long timeout, TimeUnit unit) throws InterruptedException {
        // 1.上锁
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout); // 转为毫秒
            // 2.首先检查队列是否满了
            while (size == deque.size()) {
                try {
                    if (nanos <= 0) {
                        return false;
                    }
                    // 超时等待
                    logger.info("等待加入任务队列 {} ...", task);
                    nanos = fullCondition.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 3.加入队列
            deque.addLast(task);
            logger.info("任务添加成功:{}", task);
            // 4.唤醒挂起的消费者
            emptyCondition.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    // 带超时时间阻塞获取
    public T poll(long timeout, TimeUnit unit) {
        // 1.上锁
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout); // 转为毫秒
            // 2.首先检查队列是否存在元素
            while (deque.isEmpty()) {
                try {
                    // 2.1超时判断，返回值是剩余时间
                    if (nanos <= 0) {
                        return null;
                    }
                    // 2.2超时等待
                    logger.debug("{}线程等待获取任务", Thread.currentThread());
                    nanos = emptyCondition.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 3.拿取元素
            T task = deque.removeFirst();
            logger.info("线程{}任务拿取成功", Thread.currentThread());
            // 4.唤醒挂起的生产者
            fullCondition.signal();
            return task;
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }
}
