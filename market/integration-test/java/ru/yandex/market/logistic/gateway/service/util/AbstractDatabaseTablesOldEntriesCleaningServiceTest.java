package ru.yandex.market.logistic.gateway.service.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Before;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;

public abstract class AbstractDatabaseTablesOldEntriesCleaningServiceTest extends AbstractIntegrationTest {

    private static final String DATE_TIME = "2019-12-16T06:00:00";

    private static final ZoneOffset MOSCOW_ZONE_OFFSET = ZoneOffset.ofHours(3);

    @SpyBean
    protected DatabaseTablesOldEntriesCleaningService cleaningService;

    @SpyBean(name = "readWriteTransactionTemplate")
    protected TransactionTemplate readWriteTransactionTemplate;

    @Before
    public void setUp() {
        clock.setFixed(LocalDateTime.parse(DATE_TIME).toInstant(MOSCOW_ZONE_OFFSET), ZoneId.of("Europe/Moscow"));
    }
}
