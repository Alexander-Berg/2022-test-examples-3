package ru.yandex.market.loyalty.core.dao.ydb;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.yandex.ydb.table.TableClient;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserOrder;
import ru.yandex.market.loyalty.core.monitor.CoreMonitorType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.monitoring.PushMonitor;
import ru.yandex.market.loyalty.trace.Tracer;
import ru.yandex.market.request.trace.Module;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractSafeYdbDaoTest {
    private static final Long UID = 589L;

    private TableClient ydbClient = mock(TableClient.class);
    private Tracer tracer = new Tracer(Module.YDB);
    private String dbPath = "somePath";
    private Clock clock = mock(Clock.class);
    private PushMonitor monitor = mock(PushMonitor.class);
    private ConfigurationService configurationService = mock(ConfigurationService.class);
    private YdbAllUserOrdersDao ydbAllUserOrdersDao = new YdbAllUserOrdersDao(
            ydbClient, tracer, dbPath, clock, monitor, configurationService);

    @Test
    public void upsertShouldFail() {
        when(ydbClient.getOrCreateSession(any()))
                .thenThrow(new RuntimeException("upsertShouldFail"));

        ydbAllUserOrdersDao.upsert(prepareOrder());

        verify(monitor, times(1))
                .addTemporaryCritical(
                        eq(CoreMonitorType.YDB),
                        eq("Problem in executing YDB query"),
                        eq(1L),
                        eq(TimeUnit.HOURS)
                );
    }

    @Test
    public void selectByUidWithNotCancelledStatus() {
        when(ydbClient.getOrCreateSession(any()))
                .thenThrow(new RuntimeException("upsertShouldFail"));

        Collection<UserOrder> userOrders = ydbAllUserOrdersDao.selectByUidWithNotCancelledStatus(UID);

        verify(monitor, times(1))
                .addTemporaryCritical(
                        eq(CoreMonitorType.YDB),
                        eq("Problem in executing YDB query"),
                        eq(1L),
                        eq(TimeUnit.HOURS)
                );

        assertTrue(userOrders.isEmpty());
    }

    @Test
    public void findOrdersForWelcomePromo() {
        when(ydbClient.getOrCreateSession(any()))
                .thenThrow(new RuntimeException("upsertShouldFail"));

        Collection<Long> userOrders = ydbAllUserOrdersDao.findOrdersForWelcomePromo(UID, Instant.now(), Instant.now());

        verify(monitor, times(1))
                .addTemporaryCritical(
                        eq(CoreMonitorType.YDB),
                        eq("Problem in executing YDB query"),
                        eq(1L),
                        eq(TimeUnit.HOURS)
                );

        assertTrue(userOrders.isEmpty());
    }

    private UserOrder prepareOrder() {
        return new UserOrder(0L, "DELIVERED", Instant.now(), "binding-key", Platform.ALICE_BROWSER, 100L);
    }
}
