/*
 * Copyright (c) 2017. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package ru.yandex.market.checkout.common.ratelimit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public final class RateLimitCheckerHelper {

    private static final long MAX_WAIT_MS = 90_000L;

    private RateLimitCheckerHelper() {
    }

    public static void awaitEmptyQueue(ExecutorService executorService) {
        if (!(executorService instanceof ThreadPoolExecutor)) {
            throw new IllegalStateException("executorService is not ThreadPoolExecutor");
        }

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;

        long start = System.currentTimeMillis();
        while (threadPoolExecutor.getActiveCount() != 0
                && (start + MAX_WAIT_MS > System.currentTimeMillis())
        ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
