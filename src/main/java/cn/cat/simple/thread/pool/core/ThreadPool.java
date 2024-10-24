package cn.cat.simple.thread.pool.core;

import cn.cat.simple.thread.pool.factory.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPool.class);
    private Configuration configuration;
    /**
     * 任务等待队列
     */
    private WorkQueue<Runnable> workQueue;
    /**
     * 正在运行的工作线程集合
     */
    private final Set<Worker> workerSet = new HashSet<>();
    /**
     * 核心线程数
     */
    private int corePoolSize;
    /**
     * 最大线程数
     */
    private int maximumPoolSize;
    /**
     * 最大等待时间（也就是线程的最大空闲时间)
     */
    private Long keepAliveTime;
    /**
     * 等待时间单位
     */
    private TimeUnit timeUnit;

    public ThreadPool(Configuration configuration, WorkQueue<Runnable> workQueue) {
        this.configuration = configuration;
        this.corePoolSize = configuration.getCorePoolSize();
        this.maximumPoolSize = configuration.getMaximumPoolSize();
        this.keepAliveTime = configuration.getKeepAliveTime();
        this.timeUnit = configuration.getTimeUnit();
        this.workQueue = workQueue;
    }

    class Worker extends Thread {
        // 任务
        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            logger.info("工作线程{}开始运行", Thread.currentThread());

            // 1。首先消费当前任务，消费完再去任务队列取，while循环实现线程复用
            while (task != null || (task = workQueue.poll(keepAliveTime, timeUnit)) != null) {
                try {
                    task.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 执行完后清除任务
                    task = null;
                }
            }
            // 2.跳出循环，说明取任务超过了最大等待时间，线程歇菜休息吧
            synchronized (workerSet) {
                workerSet.remove(this);
            }
            logger.info("线程{}超过最大空闲时间没有获取到任务，已被回收", Thread.currentThread());
        }
    }

    public void execute(Runnable task) {
        synchronized (workerSet) {
            //1 判断当前运行的工作线程数是否小于核心线程数
            if (workerSet.size() < corePoolSize) {
                //2.1 创建工作线程
                Worker worker = new Worker(task);
                //2.2 加入到工作线程集合
                workerSet.add(worker);
                //2.3 启动工作线程
                worker.start();
            } else {
                logger.info("队列已满，任务{}进入等待队列", task);
                // 尝试将任务放入等待队列
                workQueue.put(task);
            }
        }
    }
}
