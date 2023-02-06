package ru.yandex.market.mbo.tms.billing;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.BillingCounter;
import ru.yandex.market.mbo.billing.BillingLogCleaner;
import ru.yandex.market.mbo.billing.BillingSessionManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.tms.billing.BillingJobDateUtils.calendarDayAfterTomorrow;
import static ru.yandex.market.mbo.tms.billing.BillingJobDateUtils.calendarToday;
import static ru.yandex.market.mbo.tms.billing.BillingJobDateUtils.calendarTomorrow;
import static ru.yandex.market.mbo.tms.billing.BillingJobDateUtils.calendarYesterday;
import static ru.yandex.market.mbo.tms.billing.BillingJobDateUtils.today;
import static ru.yandex.market.mbo.tms.billing.BillingJobDateUtils.yesterday;

/**
 * Тест проверяет, как будет вести себя механизм ретраев джобы биллинга в различных ситуациях.
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class BillingLoaderJobExecutorTest {

    private static final long ALLOWED_LAG_MINS = 3;
    private static final long UPDATE_PERIOD_SEC = 2;
    private static final long WAIT_SESSION_UPDATE_SEC = 5;

    @Mock
    private BillingCounter billingCounter;
    @Mock
    private BillingLogCleaner cleaner;
    @Mock
    private BillingSessionManager sessionManager;
    private BillingSessionMarksMock billingSessionMarks;
    private MessageWriterMock messageWriterMock;

    private BillingLoaderJobExecutor executor;
    private AtomicBoolean cleanHappened;
    private AtomicBoolean billHappened;

    @Before
    public void setup() {
        billingSessionMarks = new BillingSessionMarksMock();
        messageWriterMock = new MessageWriterMock();
        executor = new BillingLoaderJobExecutor();
        initExecutor(executor);

        cleanHappened = new AtomicBoolean(false);
        billHappened = new AtomicBoolean(false);
        initDefaultBillingCounterBehaviour();
    }

    @Test
    public void testNoSessionInfo() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        executor.doRealJob(null);
        assertTrue(billed());
        assertTrue(!cleared());
    }

    @Test
    public void testYesterdayBilledOk() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(yesterday("13:30"));
        billingSessionMarks.storeSessionEnd(yesterday("14:15"), true);
        executor.doRealJob(null);
        assertTrue(billed());
        assertTrue(!cleared());
    }

    @Test
    public void testTodayBilledOkIgnored() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(today("00:01")); // не запускать тест около получночи :)
        billingSessionMarks.storeSessionEnd(today("00:02"), true);
        executor.doRealJob(null);
        assertTrue(!billed());
        assertTrue(!cleared());
    }

    @Test
    public void testBilledLongAgoError() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(yesterday().minus(1, ChronoUnit.DAYS));
        billingSessionMarks.storeSessionEnd(yesterday().minus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES), true);
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!billed());
        assertTrue(!cleared());
    }

    @Test
    public void testTodayBilledBadRetried() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(today("00:01"));
        billingSessionMarks.storeSessionEnd(today("00:02"), false);
        executor.doRealJob(null);
        assertTrue(cleared());
        assertTrue(billed());
    }

    @Test
    public void testYesterdayBilledBadError() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(yesterday("14:30"));
        billingSessionMarks.storeSessionEnd(yesterday("14:32"), false);
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!billed());
        assertTrue(!cleared());
    }

    @Test
    public void testLonPastCleanPeriodNotAllowed() {
        initCleanerBehaviourLongPast();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(today("00:01"));
        billingSessionMarks.storeSessionEnd(today("00:02"), false);
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testTodayCleanPeriodNotAllowed() {
        initCleanerBehaviourForToday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(today("00:01"));
        billingSessionMarks.storeSessionEnd(today("00:02"), false);
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testFutureCleanPeriodNotAllowed() {
        initCleanerBehaviourForTomorrow();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(today("00:01"));
        billingSessionMarks.storeSessionEnd(today("00:02"), false);
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testLastSessionStartedNotEndedIgnored() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionEnd(yesterday("00:15"), true);
        // Отметка о старте в пределах допустимой нормы. Возможно джоба ещё идёт и мы просто ничего не делаем.
        billingSessionMarks.storeSessionStart(Instant.now().minusSeconds(WAIT_SESSION_UPDATE_SEC - 1));
        executor.doRealJob(null);
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testLastSessionStartedNotEndedError() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionEnd(yesterday("00:15"), true);
        // Отметка о старте НЕ в пределах допустимой нормы. Возможно джоба зависла или упала по ораклу - тут эхепшон.
        billingSessionMarks.storeSessionStart(Instant.now().minusSeconds(WAIT_SESSION_UPDATE_SEC + 1));
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testLastSessionUpdatedNotEndedIgnored() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(yesterday("00:15"));
        // Отметка о прогрессе в пределах допустимой нормы. Возможно джоба ещё идёт и мы просто ничего не делаем.
        billingSessionMarks.storeSessionUpdate(Instant.now().minusSeconds(WAIT_SESSION_UPDATE_SEC - 1));
        executor.doRealJob(null);
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testLastSessionUpdatedNotEndedError() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(yesterday("00:15"));
        // Отметка о прогрессе НЕ в пределах допустимой нормы. Возможно джоба зависла или упала по ораклу - тут эхепшон.
        billingSessionMarks.storeSessionUpdate(Instant.now().minusSeconds(WAIT_SESSION_UPDATE_SEC + 1));
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testFutureSessionDatesErrors() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionStart(today("00:01").plus(1, ChronoUnit.DAYS));
        billingSessionMarks.storeSessionEnd(today("00:02").plus(1, ChronoUnit.DAYS), true);
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testNoStartDateError() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodYesterday();
        billingSessionMarks.storeSessionEnd(yesterday("14:45"), true);
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testTodaysBillingIntervalError() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodToday();
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    @Test
    public void testTomorrowsBillingIntervalError() {
        initCleanerBehaviourForYesterday();
        initSessionPeriodTomorrow();
        try {
            executor.doRealJob(null);
            fail("No exception is thrown");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalStateException);
        }
        assertTrue(!cleared());
        assertTrue(!billed());
    }

    /**
     * Проверяет факт вызова биллинг-каунтера (точнее его мока), и наличие временных отсечек о старте и финише сессии.
     */
    private boolean billed() {
        boolean success = billHappened.get();
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

    private boolean cleared() {
        return cleanHappened.get();
    }

    private void initDefaultBillingCounterBehaviour() {
        doAnswer(nothing -> {
            billHappened.set(true);
            return null;
        }).when(billingCounter).loadBilling(any());
    }

    private void initCleanerBehaviourForToday() {
        when(cleaner.getBillingPeriod()).thenReturn(new Pair<>(calendarToday(), calendarTomorrow()));
    }

    private void initCleanerBehaviourForTomorrow() {
        when(cleaner.getBillingPeriod()).thenReturn(new Pair<>(calendarTomorrow(), calendarDayAfterTomorrow()));
    }

    private void initCleanerBehaviourForYesterday() {
        doAnswer(nothing -> {
            cleanHappened.set(true);
            return null;
        }).when(cleaner).clean();

        when(cleaner.getBillingPeriod()).thenReturn(new Pair<>(calendarYesterday(), calendarToday()));
    }

    private void initSessionPeriodToday() {
        when(sessionManager.getBillingPeriod()).thenReturn(new Pair<>(calendarToday(), calendarTomorrow()));
    }

    private void initSessionPeriodTomorrow() {
        when(sessionManager.getBillingPeriod()).thenReturn(new Pair<>(calendarTomorrow(), calendarDayAfterTomorrow()));
    }

    private void initSessionPeriodYesterday() {
        when(sessionManager.getBillingPeriod()).thenReturn(new Pair<>(calendarYesterday(), calendarToday()));
    }

    private void initCleanerBehaviourLongPast() {
        Calendar longPast = calendarYesterday();
        longPast.set(Calendar.YEAR, 1812);
        Calendar yetLongerPast = calendarYesterday();
        yetLongerPast.set(Calendar.YEAR, 1676);
        when(cleaner.getBillingPeriod()).thenReturn(new Pair<>(yetLongerPast, longPast));
    }

    private void initExecutor(BillingLoaderJobExecutor ex) {
        ex.setBillingSessionMarks(billingSessionMarks);
        ex.setBillingCounter(billingCounter);
        ex.setBillingSessionManager(sessionManager);
        ex.setCleaner(cleaner);
        ex.setUpdateMarkStorePeriodSec(UPDATE_PERIOD_SEC);
        ex.setWaitForSessionUpdateSec(WAIT_SESSION_UPDATE_SEC);
        ex.setMessageWriter(messageWriterMock);
    }
}
