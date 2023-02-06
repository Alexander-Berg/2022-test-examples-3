package ru.yandex.market.sc.tms.stock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.stock.repository.StockLogEntity;
import ru.yandex.market.sc.core.domain.stock.repository.StockLogRepository;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.domain.stock.StockLogCleaner;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * @author valter
 */
@EmbeddedDbTmsTest
class StockLogCleanerTest {

    private static final Instant NOW = LocalDate.of(2020, 12, 21).atStartOfDay().toInstant(ZoneOffset.UTC);

    @Autowired
    StockLogCleaner stockLogCleaner;
    @Autowired
    StockLogRepository stockLogRepository;
    @Autowired
    TestFactory testFactory;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        doReturn(NOW).when(clock).instant();
        doReturn(ZoneId.of("UTC")).when(clock).getZone();
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void cleanLog() {
        doReturn(NOW).when(clock).instant();
        createStockLogEntity();
        assertThat(stockLogRepository.findAll().size()).isEqualTo(1);
        stockLogCleaner.cleanLog(NOW.plus(73, ChronoUnit.HOURS));
        assertThat(stockLogRepository.findAll().size()).isEqualTo(0);
    }

    @Test
    void doNotCleanNewLogEntries() {
        doReturn(NOW).when(clock).instant();
        createStockLogEntity();
        assertThat(stockLogRepository.findAll().size()).isEqualTo(1);
        stockLogCleaner.cleanLog(NOW.plus(72, ChronoUnit.HOURS));
        assertThat(stockLogRepository.findAll().size()).isEqualTo(1);
    }

    private void createStockLogEntity() {
        stockLogRepository.save(new StockLogEntity(
                Instant.now(clock),
                sortingCenter.getId(),
                "1", null, null, null, 1L,
                ApiOrderStatus.SORT_TO_WAREHOUSE
        ));
    }

}
