package ru.yandex.direct.traceinterception.entity.traceinterception.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.traceinterception.configuration.TraceInterceptionTest;
import ru.yandex.direct.traceinterception.entity.traceinterception.exception.TraceInterceptionActionException;
import ru.yandex.direct.traceinterception.model.TraceInterception;
import ru.yandex.direct.traceinterception.model.TraceInterceptionAction;
import ru.yandex.direct.traceinterception.model.TraceInterceptionCondition;
import ru.yandex.direct.traceinterception.model.TraceInterceptionStatus;
import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.TraceGuard;
import ru.yandex.direct.tracing.TraceHelper;
import ru.yandex.direct.tracing.TraceLogger;
import ru.yandex.direct.tracing.TraceProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppcdict.tables.TraceInterceptions.TRACE_INTERCEPTIONS;

@TraceInterceptionTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TraceInterceptionsServiceTest {
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private TraceInterceptionsManagingService managingService;
    @Autowired
    private TraceInterceptionsService traceInterceptor;

    private static Callable<Boolean> createTask(TraceHelper traceHelper, String func) {
        return () -> {
            try (TraceGuard g = traceHelper.guard("method", "")) {
                try (TraceProfile p = Trace.current().profile(func, "tags")) {
                    try {
                        Thread.sleep(3_000L);
                        return true;
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            return false;
        };
    }

    private static int getSuccessCount(Collection<Future<Boolean>> futures) {
        return futures.stream().mapToInt(future -> {
            try {
                future.get();
                return 1;
            } catch (Exception e) {
                return 0;
            }
        }).sum();
    }

    private static ThrowableAssert.ThrowingCallable executeWithProfile(TraceHelper traceHelper, String method,
                                                                       String func, String tags) {
        return () -> {
            try {
                try (TraceGuard g = traceHelper.guard(method, "")) {
                    try (TraceProfile p = Trace.current().profile(func, tags)) {
                        assert true;
                    }
                }
            } catch (TraceInterceptionActionException e) {
                throw e;
            } catch (Exception ignored) {
            }
        };
    }

    @Before
    public void before() {
        dslContextProvider.ppcdict().deleteFrom(TRACE_INTERCEPTIONS).execute();
    }

    private TraceHelper createTraceHelper(String service) {
        ThreadPoolTaskScheduler traceScheduler = new ThreadPoolTaskScheduler();
        traceScheduler.setDaemon(true);
        traceScheduler.setThreadNamePrefix("TracingBackgroundT-");
        traceScheduler.setRemoveOnCancelPolicy(true);
        traceScheduler.initialize();

        TraceLogger traceLogger = new TraceLogger((runnable, period, timeUnit) ->
                traceScheduler.scheduleWithFixedDelay(runnable,
                        new Date(System.currentTimeMillis() + timeUnit.toMillis(period)),
                        timeUnit.toMillis(period)));

        return new TraceHelper(service, traceLogger, traceInterceptor);
    }

    @Test
    public void checkSemaphoresAction() {
        int semaphoresNum = 3;
        TraceInterception traceInterception = new TraceInterception()
                .withId(1L)
                .withCondition(new TraceInterceptionCondition()
                        .withFunc("func")
                        .withTags("tags"))
                .withAction(new TraceInterceptionAction()
                        .withSemaphorePermits(semaphoresNum))
                .withStatus(TraceInterceptionStatus.ON);

        managingService.add(traceInterception);

        sleepAfterUpdate();

        int threadsNum = semaphoresNum + 2;
        ExecutorService service = Executors.newFixedThreadPool(threadsNum);

        TraceHelper traceHelper = createTraceHelper("service");

        SoftAssertions softly = new SoftAssertions();
        // повторяем два раза, чтобы проверить, что семафор отпустился
        for (int j = 0; j < 2; ++j) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threadsNum; ++i) {
                futures.add(service.submit(createTask(traceHelper, "func")));
            }

            int result = getSuccessCount(futures);
            softly.assertThat(result)
                    .describedAs("Число потоков успешно взявших семафор на %s проходе", j)
                    .isEqualTo(semaphoresNum);
        }
        softly.assertAll();
    }

    @Test
    public void checkSharedSemaphoresAction() {
        int semaphoresNum = 2;
        TraceInterception traceInterception1 = new TraceInterception()
                .withId(11L)
                .withCondition(new TraceInterceptionCondition().withFunc("func0"))
                .withAction(new TraceInterceptionAction()
                        .withSemaphorePermits(semaphoresNum)
                        .withSemaphoreKey("shared"))
                .withStatus(TraceInterceptionStatus.ON);
        managingService.add(traceInterception1);

        TraceInterception traceInterception2 = new TraceInterception()
                .withId(12L)
                .withCondition(new TraceInterceptionCondition().withFunc("func1"))
                .withAction(new TraceInterceptionAction()
                        .withSemaphorePermits(semaphoresNum)
                        .withSemaphoreKey("shared"))
                .withStatus(TraceInterceptionStatus.ON);
        managingService.add(traceInterception2);

        sleepAfterUpdate();

        int threadsNum = semaphoresNum + 2;
        ExecutorService service = Executors.newFixedThreadPool(threadsNum);

        TraceHelper traceHelper = createTraceHelper("service");

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threadsNum; ++i) {
            futures.add(service.submit(createTask(traceHelper, "func" + i % 2)));
        }

        assertThat(getSuccessCount(futures))
                .as("Число потоков успешно взявших семафор")
                .isEqualTo(semaphoresNum);
    }

    @Test
    public void checkSharedSemaphoresPermit() {
        int semaphoresNumLow = 2;
        int semaphoresNumHigh = 3;
        TraceInterception traceInterception1 = new TraceInterception()
                .withId(21L)
                .withCondition(new TraceInterceptionCondition().withFunc("permit0"))
                .withAction(new TraceInterceptionAction()
                        .withSemaphorePermits(semaphoresNumLow)
                        .withSemaphoreKey("permit"))
                .withStatus(TraceInterceptionStatus.ON);
        managingService.add(traceInterception1);

        TraceInterception traceInterception2 = new TraceInterception()
                .withId(22L)
                .withCondition(new TraceInterceptionCondition().withFunc("permit1"))
                .withAction(new TraceInterceptionAction()
                        .withSemaphorePermits(semaphoresNumHigh)
                        .withSemaphoreKey("permit"))
                .withStatus(TraceInterceptionStatus.ON);
        managingService.add(traceInterception2);

        sleepAfterUpdate();

        int threadsNum = (semaphoresNumHigh + semaphoresNumLow) * 2;
        ExecutorService service = Executors.newFixedThreadPool(threadsNum);

        TraceHelper traceHelper = createTraceHelper("service");

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threadsNum; ++i) {
            futures.add(service.submit(createTask(traceHelper, "permit" + i % 2)));
        }

        assertThat(getSuccessCount(futures))
                .as("Число потоков успешно взявших семафор")
                .isEqualTo(semaphoresNumHigh);
    }

    @Test
    public void checkComplexAction() {
        long sleepTime = 2_000L;
        TraceInterception traceInterception = new TraceInterception()
                .withId(1L)
                .withCondition(new TraceInterceptionCondition()
                        .withFunc("func")
                        .withTags("tags"))
                .withAction(new TraceInterceptionAction()
                        .withSleepDuration(sleepTime)
                        .withExceptionMessage("test exception")
                        .withSemaphorePermits(3))
                .withStatus(TraceInterceptionStatus.ON);
        managingService.add(traceInterception);

        sleepAfterUpdate();

        TraceHelper traceHelper = createTraceHelper("service");
        SoftAssertions softly = new SoftAssertions();

        long start = System.currentTimeMillis();
        softly.assertThatCode(executeWithProfile(traceHelper, "method", "func", "tags"))
                .as("Должно быть исключение от действия")
                .hasMessage("test exception");
        long end = System.currentTimeMillis();

        softly.assertThat(sleepTime)
                .as("Должно быть sleep")
                .isLessThan(end - start);

        softly.assertAll();
    }

    @Test
    public void checkConditionsMatching() {
        managingService.add(new TraceInterception()
                .withCondition(new TraceInterceptionCondition()
                        .withService("service")
                        .withMethod("method")
                        .withFunc("func")
                        .withTags("tags"))
                .withAction(new TraceInterceptionAction()
                        .withExceptionMessage("exception 0"))
                .withStatus(TraceInterceptionStatus.ON));

        managingService.add(new TraceInterception()
                .withCondition(new TraceInterceptionCondition()
                        .withService("service")
                        .withMethod("method")
                        .withFunc("func")
                        .withTags("tags"))
                .withAction(new TraceInterceptionAction()
                        .withExceptionMessage("exception 1"))
                .withStatus(TraceInterceptionStatus.ON));

        managingService.add(new TraceInterception()
                .withCondition(new TraceInterceptionCondition()
                        .withService("service")
                        .withMethod("method")
                        .withFunc("func"))
                .withAction(new TraceInterceptionAction()
                        .withExceptionMessage("exception 2"))
                .withStatus(TraceInterceptionStatus.ON));

        managingService.add(new TraceInterception()
                .withCondition(new TraceInterceptionCondition()
                        .withMethod("method")
                        .withFunc("func"))
                .withAction(new TraceInterceptionAction()
                        .withExceptionMessage("exception 3"))
                .withStatus(TraceInterceptionStatus.ON));

        managingService.add(new TraceInterception()
                .withCondition(new TraceInterceptionCondition()
                        .withService("service")
                        .withMethod("method"))
                .withAction(new TraceInterceptionAction()
                        .withExceptionMessage("exception 4"))
                .withStatus(TraceInterceptionStatus.ON));

        managingService.add(new TraceInterception()
                .withCondition(new TraceInterceptionCondition()
                        .withTags("tags"))
                .withAction(new TraceInterceptionAction()
                        .withExceptionMessage("exception 5"))
                .withStatus(TraceInterceptionStatus.ON));

        sleepAfterUpdate();

        TraceHelper traceHelper = createTraceHelper("service");
        SoftAssertions softly = new SoftAssertions();

        softly.assertThatCode(executeWithProfile(traceHelper, "method", "func", "tags"))
                .as("Исключение при полном совпадении условий")
                .hasMessage("exception 1");

        softly.assertThatCode(executeWithProfile(traceHelper, "method", "func", "abcd"))
                .as("Исключение при любом теге")
                .hasMessage("exception 2");

        softly.assertThatCode(executeWithProfile(traceHelper, "method", "efgh", "abcd"))
                .as("Исключение при любой функции с тегом")
                .hasMessage("exception 4");

        softly.assertThatCode(executeWithProfile(traceHelper, "method1", "func", "tags1"))
                .as("Нет правил")
                .doesNotThrowAnyException();

        traceHelper = createTraceHelper("service_v1");
        softly.assertThatCode(executeWithProfile(traceHelper, "method", "func", "abcd"))
                .as("Исключение при любом сервисе")
                .hasMessage("exception 3");

        softly.assertThatCode(executeWithProfile(traceHelper, "a", "b", "tags"))
                .as("Исключение при совпадении только по тегу")
                .hasMessage("exception 5");

        softly.assertAll();
    }

    // так как сторадж обновляется асинхронно, чуть чуть ждем, пока он обновится, секунды должно с запасом хватить
    private void sleepAfterUpdate() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ignored) {
        }
    }
}
