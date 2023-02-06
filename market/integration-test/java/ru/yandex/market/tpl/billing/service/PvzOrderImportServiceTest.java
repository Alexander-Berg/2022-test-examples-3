package ru.yandex.market.tpl.billing.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.pvz.client.billing.dto.BillingOrderDto;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.queue.calculatepvzorderfee.CalculatePvzOrderFeeProducer;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;
import ru.yandex.market.tpl.billing.utils.PvzModelFactory;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PvzOrderImportServiceTest extends AbstractFunctionalTest {
    private static final long BATCH_SIZE = 20000;

    @Autowired
    private PvzOrderImportService pvzOrderImportService;
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
    @DbUnitDataSet(after = "/database/service/pvzorderimport/before/no_orders.csv")
    @DisplayName("Нет доставленных заказов")
    void noOrders() {
        LocalDate yesterday = LocalDate.of(2021, 2, 22);
        pvzOrderImportService.importDeliveredOrders(yesterday, yesterday);
        verify(pvzClient).getDeliveredOrders(yesterday, yesterday, BATCH_SIZE, 0L);
    }

    @Test
    @DbUnitDataSet(after = "/database/service/pvzorderimport/after/single_order.csv")
    @DisplayName("Есть доставленный заказ")
    void singleOrder() {

        LocalDate yesterday = LocalDate.of(2021, 2, 22);
        when(pvzClient.getDeliveredOrders(yesterday, yesterday, BATCH_SIZE, 0L)).thenReturn(
                List.of(PvzModelFactory.order(OffsetDateTime.now(clock)))
        );
        pvzOrderImportService.importDeliveredOrders(yesterday, yesterday);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/pvzorderimport/before/import_with_duplicate_order.csv",
            after = "/database/service/pvzorderimport/after/import_with_duplicate_order.csv")
    @DisplayName("Есть 2 заказа, один уже в базе")
    void singleOrderAlreadyExistsNoException() {

        LocalDate yesterday = LocalDate.of(2021, 2, 22);
        BillingOrderDto order1 = PvzModelFactory.order(OffsetDateTime.now(clock));
        BillingOrderDto order2 = PvzModelFactory.orderBuilder(OffsetDateTime.now(clock))
                .id(2L)
                .externalId("externalId2")
                .build();
        when(pvzClient.getDeliveredOrders(yesterday, yesterday, BATCH_SIZE, 0L)).thenReturn(
                List.of(order1, order2)
        );
        pvzOrderImportService.importDeliveredOrders(yesterday, yesterday);
    }
}
