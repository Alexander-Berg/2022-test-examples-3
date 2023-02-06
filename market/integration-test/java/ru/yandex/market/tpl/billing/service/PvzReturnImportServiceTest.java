package ru.yandex.market.tpl.billing.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.queue.calculatepvzorderfee.CalculatePvzOrderFeeProducer;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;
import ru.yandex.market.tpl.billing.utils.PvzModelFactory;

import static org.mockito.Mockito.when;

class PvzReturnImportServiceTest extends AbstractFunctionalTest {
    @Autowired
    private PvzReturnImportService pvzReturnImportService;
    @Autowired
    private TestableClock clock;
    @Autowired
    private CalculatePvzOrderFeeProducer calculatePvzOrderFeeProducer;
    @Autowired
    private PvzClient pvzClient;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-02-23T12:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pvzreturnimport/before/setup.csv",
            after = "/database/service/pvzreturnimport/after/single_return.csv"
    )
    void importDispatchedReturns() {
        LocalDate yesterday = LocalDate.of(2021, 2, 22);
        when(pvzClient.getDispatchedReturns(yesterday, yesterday)).thenReturn(
                List.of(PvzModelFactory.getReturn(OffsetDateTime.now(clock)))
        );
        pvzReturnImportService.importDispatchedReturns(yesterday, yesterday);
    }
}
