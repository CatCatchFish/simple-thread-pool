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

    /**
     * 线程池状态常量
     */
    private static final int RUNNING = 1;
    // 不接受新任务，但继续处理已有任务
    private static final int SHUTDOWN = 2;
    // 表示不接受新任务
    private static final int STOP = 3;
    // 所有任务已经中止，且工作线程数量为0
    private static final int TIDYING = 4;
    // 中止状态
    private static final int TERMINATED = 5;

    /**
     * 线程池当前状态
     */
    private final AtomicInteger state = new AtomicInteger(RUNNING);

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

    class Worker implements Runnable {
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

            // 1.首先消费当前任务，消费完再去任务队列取，while循环实现线程复用
            while (firstTask != null || (firstTask = getTask()) != null) {
                try {
                    firstTask.run();
                } catch (Exception e) {
                    logger.error("执行任务{}时发生了异常{}", firstTask.toString(), e.getMessage());
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
            tryTerminate();
        }
    }

    private void reject(Runnable task) {
        rejectPolicy.reject(workQueue, task);
    }

    public boolean isShutdown() {
        // 大于等于SHUTDOWN状态，表示线程池已经关闭 拒绝新任务
        return state.get() >= SHUTDOWN;
    }

    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("任务不能为空");
        }

        // 1.线程池是否关闭
        if (isShutdown()) {
            reject(task);
        }

        // 2.当前线程数小于核心线程数，直接创建工作线程
        if (threadTotalNums.get() < corePoolSize) {
            if (addWorker(task, true)) {
                return;
            }
        }

        // 3.1大于核心线程数
        if (workQueue.offer(task)) {
            return;
        } else if (!addWorker(task, false)) {
            // 3.2队列满了，尝试创建非核心线程，如果失败就触发拒绝策略
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
        // 1. 进行线程池生命周期的判断
        final int c = state.get();
        if (c > RUNNING && !(c == SHUTDOWN && !workQueue.isEmpty())) {
            return false;
        }

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
                threadTotalNums.getAndIncrement();
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
            // 1.线程池状态判断，如果为关闭状态并且任务队列为空，就返回null
            int rs = state.get();
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                return null;
            }

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
            Runnable r = null;
            try {
                r = timed ?
                        workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                        workQueue.take();
                if (r != null)
                    return r;
                // 获取任务超时了，将preIsTimeOut设为true，下次可以执行回收
                preIsTimeOut = true;
            } catch (InterruptedException e) {
                return null;
            }
        }
    }

    public void shutdown() {
        if (state.compareAndSet(RUNNING, SHUTDOWN)) {
            logger.info("线程池开始关闭");
            // 尝试转换到TERMINATED状态
            tryTerminate();
        }
    }

    public void shutdownNow() {
        if (state.compareAndSet(RUNNING, STOP)) {
            logger.info("线程池开始立即关闭");
            try {
                synchronized (workerSet) { // 保证线程安全 完全关闭线程池
                    for (Worker worker : workerSet) {
                        worker.thread.interrupt();
                    }
                }
            } finally {
                state.set(TIDYING);
                transitionToTerminated();
                logger.info("线程池已立即关闭");
            }
        }
    }

    private void tryTerminate() {
        if ((state.get() == SHUTDOWN || state.get() == STOP) && workQueue.isEmpty() && workerSet.isEmpty()) {
            if (state.compareAndSet(SHUTDOWN, TIDYING) || state.compareAndSet(STOP, TIDYING)) {
                // 在此处执行清理工作
                transitionToTerminated();
                logger.info("线程池已终止");
            }
        }
    }

    private void transitionToTerminated() {
        state.set(TERMINATED);
        // 这里可以通知等待线程池终止的线程
    }
}
