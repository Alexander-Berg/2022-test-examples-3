package ru.yandex.market.wms.autostart.autostartlogic.orderbatching;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000002002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000004001;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_03;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_04;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.stationProvider;

/**
 * Кейс: batchGroupedOrders - 3
 * combineCarriers - false
 * putWallsIntoBatch - 1
 * ordersIntoPutWall - 4
 */
@ContextConfiguration(classes = {
        OrderBatchingServiceTest3.TestBatchingServiceConfiguration.class,
})
class OrderBatchingServiceTest3
        extends AutostartIntegrationTest
        implements BatchingServiceTest3BatchGroupedOrdersCombineCarriersFalse {

    static final int MAX_ITEMS_PER_PUTWALL = 1_000_000;
    static AtomicInteger id = new AtomicInteger();
    @Autowired
    protected OrderBatchingService orderBatchingService;

    static List<Batch<OrderWithDetails>> expectedBatchesDefault() {
        return listOf(
                // door 1 is partly occupied - cannot host DEFAULT batch
                // door 2 is partly occupied - cannot host DEFAULT batch
                Batch.<OrderWithDetails>builder().id("0").subBatches(
                        listOf(
                                // CC 101, 2020-01-01
                                SubBatch.<OrderWithDetails>builder()
                                        .sortingStation(DOOR_S_03)
                                        .orders(
                                                listOf(
                                                        owdB000003003(), owdB000004001(), owdB000003004(),
                                                        owdB000003002()
                                                )
                                        )
                                        .build()
                        )
                ).build(),
                // door 4 is free, but not other CC has 4 orders
                // door 5 is partly occupied - cannot host DEFAULT batch
                Batch.<OrderWithDetails>builder().id("1").subBatches(
                        listOf(
                                SubBatch.<OrderWithDetails>builder()
                                        .sortingStation(DOOR_S_04)
                                        .orders(
                                                listOf(
                                                        owdB000001003(),
                                                        owdB000001004(),
                                                        owdB000002002())
                                        )
                                        .build()
                        )
                ).build()
        );
    }

    @BeforeEach
    void beforeEach() {
        id.set(0);
    }

    @Test
    void batchGroupedOrdersStartTypeDEFAULT() {
        assertThat(
                orderBatchingService.batchGroupedOrders(
                        exampleOrderGroup(), stationProvider(), MAX_ITEMS_PER_PUTWALL,
                        orderBatchingService.settings.getMinOrdersIntoPutWall(),
                        orderBatchingService.settings.getOrdersIntoPutWall()),
                is(expectedBatchesDefault())
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
                            .minOrdersIntoPutWall(3)
                            .putWallsIntoBatch(1)
                            .ordersIntoPutWall(4)
                            .freeOrdersForPutWall(0)
                            .maxItemsPerPutwall(MAX_ITEMS_PER_PUTWALL)
                            .activeBatchesPerPutwall(2)
                            .build()
            );
        }
    }
}
