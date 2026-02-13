package com.xteammors.openclaw.utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {

    private static ThreadUtils threadUtils;

    private ThreadUtils() {
    }

    public static ThreadUtils instance() {
        if (null == threadUtils) {
            threadUtils = new ThreadUtils();
            threadUtils.initExecutor();
        }
        return threadUtils;
    }

    private static final int CORE_POOL_SIZE = 200;
    private static final int MAX_POOL_SIZE = 10000;
    private static final int QUEUE_CAPACITY = 1;
    private static final Long KEEP_ALIVE_TIME = 1L;
    ThreadPoolExecutor executor;

    public void  initExecutor(){
        executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void shutdownExecutor(){
        try {

            if (executor != null){
                executor.shutdown();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public ThreadPoolExecutor  getExecutor(){
        return executor;
    }

}
