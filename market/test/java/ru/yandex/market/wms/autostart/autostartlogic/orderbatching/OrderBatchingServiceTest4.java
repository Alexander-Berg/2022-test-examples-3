package ru.yandex.market.wms.autostart.autostartlogic.orderbatching;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.model.dto.AOSSettingsDto;
import ru.yandex.market.wms.common.spring.dao.entity.Batch;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.SubBatch;

import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000004001;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_03;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.stationProvider;

/**
 * Кейс: batchGroupedOrders - 3
 * combineCarriers - true
 * putWallsIntoBatch - 2
 * ordersIntoPutWall - 4
 */
@ContextConfiguration(classes = {
        OrderBatchingServiceTest4.TestBatchingServiceConfiguration.class,
})
class OrderBatchingServiceTest4
        extends AutostartIntegrationTest
        implements BatchingServiceTest3BatchGroupedOrdersCombineCarriersFalse {

    static final int MAX_ITEMS_PER_PUTWALL = 1_000_000;
    static AtomicInteger id = new AtomicInteger();
    @Autowired
    protected OrderBatchingService orderBatchingService;

    @BeforeEach
    void beforeEach() {
        id.set(0);
    }

    @Test
    void applyStartTypeDEFAULT() {
        // the same as with 1 put wall / batch: there is only one carrier with 4 orders
        List<Batch<OrderWithDetails>> batches = orderBatchingService.batchGroupedOrders(
                exampleOrderGroup(), stationProvider(),
                MAX_ITEMS_PER_PUTWALL,  orderBatchingService.settings.getMinOrdersIntoPutWall(),
                orderBatchingService.settings.getOrdersIntoPutWall());
        Assertions.assertEquals(
                batches.get(0),
                        // door 1 is partly occupied - cannot host DEFAULT batch
                        // door 2 is partly occupied - cannot host DEFAULT batch
                        Batch.builder().id("0").subBatches(
                                listOf(
                                        // CC 101, 2020-01-01
                                        SubBatch.builder()
                                                .sortingStation(DOOR_S_03)
                                                .orders(listOf(owdB000003003(), owdB000004001(), owdB000003004(),
                                                        owdB000003002()))
                                                .build()
                                )
                        ).build()
                        // door 5 is partly occupied - cannot host DEFAULT batch
                );
    }

    @Configuration
    public static class TestBatchingServiceConfiguration {

        @Bean
        @Primary
        public OrderBatchingService batchingService() {
            return new OrderBatchingService(
                    () -> String.valueOf(id.getAndIncrement()),
                    AOSSettingsDto.builder()
                            .minOrdersIntoPutWall(4)
                            .putWallsIntoBatch(2)
                            .ordersIntoPutWall(4)
                            .freeOrdersForPutWall(0)
                            .maxItemsPerPutwall(MAX_ITEMS_PER_PUTWALL)
                            .activeBatchesPerPutwall(2)
                            .build()
            );
        }
    }
}
