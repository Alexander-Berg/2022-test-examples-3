package ru.yandex.market.health.configs.logshatter.sanitizer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SnooperSanitizerTest {

    public static final String STRING_WITH_RAW_SECRET = "AgAAAAAHYVLuAAX_m477zavGV0GMlcItT-kpRm1";
    public static final String STRING_WITH_MASKED_SECRET = "AgAAAAAHYVLuAAX_m4XXXXXXXXXXXXXXXXXXXXX";
    private static final Logger log = LogManager.getLogger();

    /**
     * Базовый тест для проверки, что библиотека snooper, обёрнутая в синглотон, работает корректно.
     * Данные для теста взяты отсюда: https://nda.ya.ru/t/Bh1DufSv4k2qsb
     */
    @Test
    void mask() {
        Assertions.assertEquals(STRING_WITH_MASKED_SECRET, SnooperSanitizer.getInstance().mask(STRING_WITH_RAW_SECRET));
    }

    @Test
    void multiThreadedTest() throws InterruptedException {
        int threadsCount = 100;
        ConcurrentHashMap<Integer, String> taskNumResultMap = new ConcurrentHashMap<>();
        CountDownLatch preparedCountDownLatch = new CountDownLatch(threadsCount);
        CountDownLatch finishedCountDownLatch = new CountDownLatch(threadsCount);
        SimultaneouslyRunningThreadsCounter simultaneouslyRunningThreadsCounter =
            new SimultaneouslyRunningThreadsCounter();
        String threadPrefixTemplate = "task %d - ";
        String inputStringTemplate = threadPrefixTemplate + STRING_WITH_RAW_SECRET;
        String resultStringTemplate = threadPrefixTemplate + STRING_WITH_MASKED_SECRET;
        for (int i = 0; i < threadsCount; ++i) {
            new Thread(new SnooperTask(taskNumResultMap, i, String.format(inputStringTemplate, i),
                preparedCountDownLatch, finishedCountDownLatch, simultaneouslyRunningThreadsCounter)).start();
        }
        finishedCountDownLatch.await();
        Assertions.assertEquals(threadsCount, taskNumResultMap.size());
        for (int i = 0; i < threadsCount; ++i) {
            Assertions.assertEquals(String.format(resultStringTemplate, i), taskNumResultMap.get(i));
        }
        log.info("max simultaneously running threads count: " + simultaneouslyRunningThreadsCounter.getMax());
    }

    private static class SnooperTask implements Runnable {

        final ConcurrentHashMap<Integer, String> taskNumResultMap;
        final int taskNum;
        final String inputString;
        final CountDownLatch preparedCountDownLatch;
        final CountDownLatch finishedCountDownLatch;
        final SimultaneouslyRunningThreadsCounter simultaneouslyRunningThreadsCounter;

        private SnooperTask(ConcurrentHashMap<Integer, String> taskNumResultMap, int taskNum, String inputString,
                            CountDownLatch preparedCountDownLatch, CountDownLatch finishedCountDownLatch,
                            SimultaneouslyRunningThreadsCounter simultaneouslyRunningThreadsCounter) {
            this.taskNumResultMap = taskNumResultMap;
            this.taskNum = taskNum;
            this.inputString = inputString;
            this.preparedCountDownLatch = preparedCountDownLatch;
            this.finishedCountDownLatch = finishedCountDownLatch;
            this.simultaneouslyRunningThreadsCounter = simultaneouslyRunningThreadsCounter;
        }

        @Override
        public void run() {
            // сообщаем, что текущий поток готов
            preparedCountDownLatch.countDown();
            try {
                // ждём, когда все потоки будут готовы. Всё для того, чтобы начать бомбить SnooperSanitizer одновременно
                preparedCountDownLatch.await();
            } catch (InterruptedException e) {
                return;
            }
            simultaneouslyRunningThreadsCounter.incCurRunningCount();
            try {
                taskNumResultMap.put(taskNum, SnooperSanitizer.getInstance().mask(inputString));
            } catch (RuntimeException e) {
                log.error(e);
            } finally {
                simultaneouslyRunningThreadsCounter.decCurRunningCount();
                finishedCountDownLatch.countDown();
            }
        }
    }

    private static class SimultaneouslyRunningThreadsCounter {

        int curCount = 0;
        int maxCount = 0;

        synchronized void incCurRunningCount() {
            ++curCount;
            maxCount = Math.max(maxCount, curCount);
        }

        synchronized void decCurRunningCount() {
            --curCount;
        }

        synchronized int getMax() {
            return maxCount;
        }
    }
}
