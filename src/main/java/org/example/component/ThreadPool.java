package org.example.component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    static final ExecutorService SYS_POOL = new ThreadPoolExecutor(
            7,
            7,
            1,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());
    static final ExecutorService CONTAINER_POOL = new ThreadPoolExecutor(
            7,
            7,
            1,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());

    public static ExecutorService getSys() {
        return SYS_POOL;
    }

    public static ExecutorService getContainer() {
        return CONTAINER_POOL;
    }
}
