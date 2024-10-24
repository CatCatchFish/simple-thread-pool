package cn.cat.simple.thread.pool.factory;

import cn.cat.simple.thread.pool.policy.RejectPolicy;

import java.util.concurrent.TimeUnit;

public class Configuration {
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

    private RejectPolicy<Runnable> rejectPolicy;

    public Configuration(int corePoolSize, int maximumPoolSize, Long keepAliveTime, TimeUnit timeUnit, RejectPolicy<Runnable> rejectPolicy) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.rejectPolicy = rejectPolicy;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public Long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public RejectPolicy<Runnable> getRejectPolicy() {
        return rejectPolicy;
    }

    public void setRejectPolicy(RejectPolicy<Runnable> rejectPolicy) {
        this.rejectPolicy = rejectPolicy;
    }
}
