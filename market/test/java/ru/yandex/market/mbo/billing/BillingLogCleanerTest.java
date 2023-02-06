package ru.yandex.market.mbo.billing;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcOperations;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.history.messaging.MessageWriter;

import java.sql.Timestamp;
import java.util.Calendar;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BillingLogCleanerTest {

    MessageWriter messageWriter;
    BillingSessionManager sessionManager;
    BillingCleanSessionManager cleanSessionManager;
    BillingLogCleaner billingLogCleaner;
    JdbcOperations jdbcTemplate;
    BillingStatusUpdater billingStatusUpdater;
    YtBillingDumpSessionManager ytBillingDumpSessionManager;

    private static final int DEFAULT_AMOUNT = -1;
    private static final int YEAR = 2020;
    private static final int MONTH = 1;
    private static final int DATE = 2;
    private static Calendar calendarFrom;
    private static Pair<Calendar, Calendar> testPair;

    private static final String NG_PAID_OPERATION_QUERY = "DELETE FROM ng_paid_operation_log WHERE time >= ?";
    private static final String NG_SUSPENDED_OPERATION_QUERY = "DELETE FROM ng_suspended_operation_log WHERE time >= ?";
    private static final String NG_DALY_BALANCE_QUERY = "DELETE FROM ng_daily_balance WHERE day > ?";

    @Before
    public void setUp() {
        messageWriter = Mockito.mock(MessageWriter.class);
        sessionManager = Mockito.mock(BillingSessionManager.class);
        jdbcTemplate = Mockito.mock(JdbcOperations.class);
        billingStatusUpdater = Mockito.mock(BillingStatusUpdater.class);
        ytBillingDumpSessionManager = Mockito.mock(YtBillingDumpSessionManager.class);

        billingLogCleaner = new BillingLogCleaner();
        billingLogCleaner.setMessageWriter(messageWriter);
        billingLogCleaner.setSessionManager(sessionManager);
        billingLogCleaner.setJdbcTemplate(jdbcTemplate);
        billingLogCleaner.setvNgBillingBalanceJdbcTemplate(jdbcTemplate);
        billingLogCleaner.setBillingStatusUpdater(billingStatusUpdater);
        billingLogCleaner.setYtBillingDumpSessionManager(ytBillingDumpSessionManager);

        calendarFrom = Calendar.getInstance();
        calendarFrom.set(YEAR, MONTH, DATE, 0, 0, 0);
        testPair = new Pair<>(calendarFrom, Calendar.getInstance());
        Mockito.when(sessionManager.getBillingPeriod()).thenReturn(testPair);
    }

    @Test
    public void testCleanWithCleanSessionManager() {
        cleanSessionManager = new BillingCleanSessionManager();
        cleanSessionManager.setBillingSession(sessionManager);
        billingLogCleaner.setSessionManager(cleanSessionManager);

        billingLogCleaner.clean();

        Calendar fromWithOffset = (Calendar) calendarFrom.clone();
        fromWithOffset.add(Calendar.DATE, DEFAULT_AMOUNT);

        expectQueriesWith(fromWithOffset);
    }

    @Test
    public void testCleanWithCleanSessionManagerAndZeroAmount() {
        cleanSessionManager = new BillingCleanSessionManager();
        cleanSessionManager.setBillingSession(sessionManager);
        billingLogCleaner.setSessionManager(cleanSessionManager);
        cleanSessionManager.setDayOffset(0);

        billingLogCleaner.clean();
        expectQueriesWith(calendarFrom);
    }

    @Test
    public void testCleanWithSessionManager() {
        billingLogCleaner.clean();
        expectQueriesWith(calendarFrom);
    }

    private void expectQueriesWith(Calendar calendar) {
        Timestamp expectedTimestamp = new Timestamp(calendar.getTimeInMillis());
        verify(ytBillingDumpSessionManager).rollbackTo(calendar);
        verify(jdbcTemplate, times(1)).update(NG_PAID_OPERATION_QUERY, expectedTimestamp);
        verify(jdbcTemplate, times(1)).update(NG_SUSPENDED_OPERATION_QUERY, expectedTimestamp);
        verify(jdbcTemplate, times(1)).update(NG_DALY_BALANCE_QUERY, expectedTimestamp);
    }
}
