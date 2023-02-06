package ru.yandex.market.health.configs.logshatter.useragent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.errors.ErrorsParserTest;

import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 2019-03-21
 */
public class UatraitsUserAgentDetectorTest {
    private static final Logger log = LogManager.getLogger();

    @Test
    @Disabled
    public void testConcurrency() throws Exception {
        UserAgentDetector detector = new UatraitsUserAgentDetector();
        ErrorsParserTest errorsParserTest = new ErrorsParserTest();
        int count = 1_000_000;
        int threads = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        Stopwatch stopwatch = Stopwatch.createStarted();

        for (int i = 0; i < count; i++) {
            executorService.submit(() -> errorsParserTest.detectDesktopBrowser(detector));
            executorService.submit(() -> errorsParserTest.detectTouchBrowser(detector));
        }
        log.info("{} tasks scheduled in {}", count, stopwatch.toString());
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.MINUTES);
        log.info("{} threads processed {} tasks in {}", threads, count, stopwatch.toString());

    }

    @Test
    @Disabled
    public void testConcurrency2() throws InterruptedException {
        UatraitsUserAgentDetector detector = new UatraitsUserAgentDetector();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            threads.add(new Thread(() -> detector.detect("")));
        }
        for (Thread thread : threads) {
            thread.start();
        }

        Thread.sleep(300_000);

        boolean someThreadsWereAlive = false;
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                someThreadsWereAlive = true;
                System.out.println("\n" + thread.getName());
                for (StackTraceElement stackTraceElement : thread.getStackTrace()) {
                    System.out.println(stackTraceElement);
                }
            }
        }

        assertFalse(someThreadsWereAlive);
    }
}
