package cn.cat.simple.thread.pool.factory;

/**
 * @author Cat
 * @description 创建线程的工厂接口
 */
public interface ThreadFactory {
    Thread newThread(Runnable r);
}
