package ru.yandex.market.mbo.tms.billing;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.BillingCounter;
import ru.yandex.market.mbo.billing.BillingException;
import ru.yandex.market.mbo.billing.BillingLogCleaner;
import ru.yandex.market.mbo.billing.BillingSessionManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.tms.billing.BillingJobDateUtils.calendarToday;
import static ru.yandex.market.mbo.tms.billing.BillingJobDateUtils.calendarYesterday;

/**
 * Несколько тестов, проверяющих как два независимых запуска биллинга будут "общаться" друг с другом через отсечки
 * в базе. Под общением подразумевается контроль запуска при наличии уже бегущей джобы или запуск очистки биллинга
 * при неудачном завершении предыдущего. Более детально логика принятия решений - биллить или нет, чистить или
 * нет и т.д. - тестируется в {@link BillingLoaderJobExecutorTest}.
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class BillingTwoJobsTest {

    private static final long ALLOWED_LAG_MINS = 3;
    private static final long UPDATE_PERIOD_SEC = 2;
    private static final long WAIT_SESSION_UPDATE_SEC = 60000;

    @Mock
    private BillingCounter billingCounter1;
    @Mock
    private BillingCounter billingCounter2;
    @Mock
    private BillingLogCleaner cleaner1;
    @Mock
    private BillingLogCleaner cleaner2;
    @Mock
    private BillingSessionManager sessionManager;

    private BillingSessionMarksMock billingSessionMarks;
    private MessageWriterMock messageWriterMock;
    private BillingLoaderJobExecutor executor1;
    private BillingLoaderJobExecutor executor2;
    private AtomicBoolean cleanHappened1;
    private AtomicBoolean billHappened1;
    private AtomicBoolean cleanHappened2;
    private AtomicBoolean billHappened2;

    @Before
    public void setup() {
        billingSessionMarks = new BillingSessionMarksMock();
        messageWriterMock = new MessageWriterMock();
        executor1 = initExecutor(billingCounter1, cleaner1);
        executor2 = initExecutor(billingCounter2, cleaner2);
        cleanHappened1 = new AtomicBoolean(false);
        billHappened1 = new AtomicBoolean(false);
        cleanHappened2 = new AtomicBoolean(false);
        billHappened2 = new AtomicBoolean(false);
        when(sessionManager.getBillingPeriod()).thenReturn(new Pair<>(calendarYesterday(), calendarToday()));
    }

    @Test
    public void testSimpleSequential() {
        initCleanersBehaviourForYesterday();
        initDefaultBillingCountersBehaviour();

        executor1.doRealJob(null);
        executor2.doRealJob(null);
        assertTrue(!cleared(executor1));
        assertTrue(billed(executor1));
        assertTrue(!cleared(executor2));
        assertTrue(!billed(executor2));
    }

    @Test
    public void testFirstFailsGracefullySecondBills() {
        initCleanersBehaviourForYesterday();
        initCustomCounterBehaviour(executor1, () -> {
            throw new BillingException("Expected billing exception");
        });
        initCustomCounterBehaviour(executor2, () -> billHappened2.set(true));

        executor1.doRealJob(null); // Джоба №1 "мягко" упадёт, поймав исключение и закрыв сессию.
        executor2.doRealJob(null); // Эта же всё исправит, вызвав Clean & bill
        assertTrue(!cleared(executor1));
        assertTrue(!billed(executor1));
        assertTrue(cleared(executor2));
        assertTrue(billed(executor2));
    }

    @Test
    public void testFirstHangsSecondWaitsAndFails() {
        initCleanersBehaviourForYesterday();
        initCustomCounterBehaviour(executor1, () -> {
            throw new RuntimeException("Unexpected other test exception"); // делаем эпичный фейл
        });
        initCustomCounterBehaviour(executor2, () -> billHappened2.set(true));

        try { // В итоге первая джоба падает вообще без отсечек о завершении.
            executor1.doRealJob(null);
            fail("Job should've crashed.");
        } catch (RuntimeException e) {
            assertEquals("Unexpected other test exception", e.getMessage());
        }

        // Запускаем вторую следом. Она не увидит отметки о завершении, и поэтому будет ждать некоторый таймаут
        // с последнего апдейта/старта упавшей джобы №1.
        executor2.doRealJob(null);
        // Т.к. вызов произошёл сразу, таймаут ещё не превысился, и у джобы №2 ещё есть надежда, что первая очухается.
        // Эксепшенов не будет, просто игнор.
        assertTrue(!cleared(executor1));
        assertTrue(!billed(executor1));
        assertTrue(!cleared(executor2));
        assertTrue(!billed(executor2));

        sleepSec(1);

        executor2.setWaitForSessionUpdateSec(0); // чтобы не ждать толпу времени, имитируем нулевой таймаут.
        try { // Теперь таймаут ожидания первой джобы превышен, работает она или нет - непонятно. Бросаем исключение.
            executor2.doRealJob(null);
            fail("Job should've crashed.");
        } catch (Exception badStateException) {
            assertTrue(badStateException instanceof IllegalStateException);
        }
        assertTrue(!cleared(executor1));
        assertTrue(!billed(executor1));
        assertTrue(!cleared(executor2));
        assertTrue(!billed(executor2));
    }

    @Test
    public void testAliveKeeperThreadReusable() {
        // Тестик на отлов ошибок типа RejectedExecutionException и подобного
        initCleanersBehaviourForYesterday();
        initCustomCounterBehaviour(executor2, () -> {
            throw new BillingException("Expected billing exception");
        });
        executor2.setUpdateMarkStorePeriodSec(1);
        executor2.doRealJob(null);
        sleepSec(1);
        executor2.doRealJob(null);
        assertTrue(cleared(executor2)); // Т.к. первый раз упали с ошибкой, он попытается почиститься.
        assertTrue(!billed(executor2));
    }

    private BillingLoaderJobExecutor initExecutor(BillingCounter counter, BillingLogCleaner cleaner) {
        BillingLoaderJobExecutor ex = new BillingLoaderJobExecutor();
        ex.setBillingSessionMarks(billingSessionMarks);
        ex.setBillingCounter(counter);
        ex.setBillingSessionManager(sessionManager);
        ex.setCleaner(cleaner);
        ex.setUpdateMarkStorePeriodSec(UPDATE_PERIOD_SEC);
        ex.setWaitForSessionUpdateSec(WAIT_SESSION_UPDATE_SEC);
        ex.setMessageWriter(messageWriterMock);
        return ex;
    }

    private void initCleanersBehaviourForYesterday() {
        // Первый cleaner даже мокать не будем, он по сценарию не должен срабатывать.

        doAnswer(nothing -> {
            cleanHappened2.set(true);
            return null;
        }).when(cleaner2).clean();

        when(cleaner2.getBillingPeriod()).thenReturn(new Pair<>(calendarYesterday(), calendarToday()));
    }

    private void initCustomCounterBehaviour(BillingLoaderJobExecutor executor, Runnable func) {
        BillingCounter what = executor == executor1 ? billingCounter1 : billingCounter2;
        doAnswer(nothing -> {
            func.run();
            return null;
        }).when(what).loadBilling(any());
    }

    private void initDefaultBillingCountersBehaviour() {
        initCustomCounterBehaviour(executor1, () -> {
            sleepSec(UPDATE_PERIOD_SEC);
            billHappened1.set(true);
            sleepSec(UPDATE_PERIOD_SEC);
        });

        initCustomCounterBehaviour(executor2, () -> {
            sleepSec(UPDATE_PERIOD_SEC);
            billHappened2.set(true);
            sleepSec(UPDATE_PERIOD_SEC);
        });
    }

    private boolean billed(BillingLoaderJobExecutor executor) {
        AtomicBoolean what = executor == executor1 ? billHappened1 : billHappened2;
        boolean success = what.get();
        if (!success) {
            return false;
        }
        Optional<BillingSessionMarks.SessionResult> maybeEnd = billingSessionMarks.loadSessionEnd();
        if (!maybeEnd.isPresent()) {
            return false;
        }
        success = maybeEnd.get().isSuccess();
        if (!success) {
            return false;
        }
        // Проверим свежую отсечку по времени. Т.к. тест может внезапно затупить по желанию левой пятки протуберанца
        // на солнце, сделаем небольшой допустимый временной лаг между текущим временем и свежей отсечкой.
        Instant now = Instant.now();
        Instant start = billingSessionMarks.loadSessionStart();
        Instant end = maybeEnd.get().getTime();
        long minutesFromStart = Math.abs(ChronoUnit.MINUTES.between(start, now));
        long minutesFromEnd = Math.abs(ChronoUnit.MINUTES.between(end, now));
        return minutesFromStart <= ALLOWED_LAG_MINS && minutesFromEnd <= ALLOWED_LAG_MINS;
    }

    private boolean cleared(BillingLoaderJobExecutor executor) {
        return (executor == executor1 ? cleanHappened1 : cleanHappened2).get();
    }

    private void sleepSec(long sec) {
        try {
            Thread.sleep(sec * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException("Concurrent execution error: ", e);
        }
    }
}
