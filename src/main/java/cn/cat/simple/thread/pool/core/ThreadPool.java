package cn.cat.simple.thread.pool.core;

import cn.cat.simple.thread.pool.factory.Configuration;
import cn.cat.simple.thread.pool.factory.ThreadFactory;
import cn.cat.simple.thread.pool.policy.RejectPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final int corePoolSize;
    /**
     * 最大线程数
     */
    private int maximumPoolSize;
    /**
     * 最大等待时间（也就是线程的最大空闲时间)
     */
    private final Long keepAliveTime;
    /**
     * 等待时间单位
     */
    private final TimeUnit timeUnit;

    /**
     * 任务拒绝策略
     */
    private final RejectPolicy<Runnable> rejectPolicy;
    /**
     * 线程工厂
     */
    private ThreadFactory threadFactory;
    /**
     * 当前线程中的线程数
     */
    private final AtomicInteger threadTotalNums = new AtomicInteger(0);
    /**
     * 是否允许核心线程被回收
     */
    private boolean allowCoreThreadTimeOut = false;

    public ThreadPool(Configuration configuration, WorkQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        this.configuration = configuration;
        this.corePoolSize = configuration.getCorePoolSize();
        this.maximumPoolSize = configuration.getMaximumPoolSize();
        this.keepAliveTime = configuration.getKeepAliveTime();
        this.timeUnit = configuration.getTimeUnit();
        this.rejectPolicy = configuration.getRejectPolicy();
        this.workQueue = workQueue;
        this.threadFactory = threadFactory;
    }

    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    class Worker extends Thread {
        // 任务
        private Runnable firstTask;
        // 线程
        private Thread thread;

        public Worker(Runnable task) {
            this.firstTask = task;
            this.thread = threadFactory.newThread(this);
        }

        @Override
        public void run() {
            logger.info("工作线程{}开始运行", Thread.currentThread());

            // 1。首先消费当前任务，消费完再去任务队列取，while循环实现线程复用
            while (firstTask != null || (firstTask = getTask()) != null) {
                try {
                    firstTask.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 执行完后清除任务
                    firstTask = null;
                }
            }
            // 2.跳出循环，说明取任务超过了最大等待时间，线程歇菜休息吧
            synchronized (workerSet) {
                workerSet.remove(this);
                threadTotalNums.decrementAndGet();
            }
            logger.info("工作线程====》线程{}已被回收，当前线程数:{}", Thread.currentThread(), threadTotalNums.get());
        }
    }

    private void reject(Runnable task) {
        rejectPolicy.reject(workQueue, task);
    }


    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("任务不能为空");
        }
        // 当前线程数小于核心线程数，直接创建工作线程
        if (threadTotalNums.get() < corePoolSize) {
            if (addWorker(task, true)) {
                return;
            }
        }

        // 2.大于核心线程数
        if (workQueue.offer(task)) {
            return;
        } else if (!addWorker(task, false)) {
            // 3.队列满了，尝试创建非核心线程，如果失败就触发拒绝策略
            reject(task);
        }
    }

    /**
     * @param firstTask 线程第一次执行的任务
     * @param isCore    是否为核心线程
     * @return Boolean 是否添加成功
     * @description 添加工作线程
     */
    public Boolean addWorker(Runnable firstTask, Boolean isCore) {
        if (firstTask == null) {
            throw new NullPointerException("");
        }
        // 1. TODO 进行线程池生命周期的判断，某些情况下不允许添加线程

        // 2. 根据当前线程池和isCore条件判断是否需要创建
        int wc = threadTotalNums.get();
        if (wc >= (isCore ? corePoolSize : maximumPoolSize)) {
            return false;
        }
        // 3. 创建工作线程
        Worker worker = new Worker(firstTask);
        Thread t = worker.thread;
        if (t != null) {
            synchronized (workerSet) {
                workerSet.add(worker);
                threadTotalNums.incrementAndGet();
            }
            t.start();
            return true;
        }
        return false;
    }

    public Runnable getTask() {
        //我们使用一个变量来记录上次循环获取任务是否超时
        boolean preIsTimeOut = false;
        while (true) {
            // 线程池当前线程数量
            int wc = threadTotalNums.get();
            // 1.是否进行核心线程回收
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            // 2.是据情况调整线程数量，一下是返回null的情况：
            // 2.1 wc > 最大线程数 && 任务队列为空且存在工作线程
            // 2.2 timed && 任务队列为空且存在工作线程
            if ((wc > maximumPoolSize || (timed && preIsTimeOut)) && (wc > 1 || workQueue.isEmpty())) {
                return null;
            }

            // 3.根据timed这个条件来选择是超时堵塞
            Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();

            if (r != null) {
                return r;
            }
            preIsTimeOut = true;
        }
    }
}
