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
import static org.hamcrest.Matchers.contains;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000002002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000004001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000005002;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_03;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_04;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.stationProvider;

/**
 * Кейс: batchGroupedOrders - 3
 * combineCarriers - true
 * putWallsIntoBatch - 1
 * ordersIntoPutWall - 4
 */
@ContextConfiguration(classes = {
        OrderBatchingServiceTest1.TestBatchingServiceConfiguration.class,
})
class OrderBatchingServiceTest1
        extends AutostartIntegrationTest
        implements BatchingServiceTest3BatchGroupedOrdersCombineCarriersTrue {

    static final int MAX_ITEMS_PER_PUTWALL = 1_000_000;
    static AtomicInteger id = new AtomicInteger();
    @Autowired
    protected OrderBatchingService orderBatchingService;

    @BeforeEach
    void beforeEach() {
        id.set(0);
    }

    /**
     * Batch DEFAULT must be equals max qty orders or items
     */
    @Test
    void batchGroupedOrdersStartTypeDEFAULT() {
        List<Batch<OrderWithDetails>> batches = orderBatchingService.batchGroupedOrders(
                exampleOrderGroup(), stationProvider(),
                MAX_ITEMS_PER_PUTWALL, orderBatchingService.settings.getMinOrdersIntoPutWall(),
                orderBatchingService.settings.getOrdersIntoPutWall());
        assertThat(
                batches,
                contains(
                        // door 1 is partly occupied - cannot host DEFAULT batch
                        // door 2 is partly occupied - cannot host DEFAULT batch
                        Batch.builder().id("0").subBatches(
                                listOf(
                                        // 2020-01-01
                                        SubBatch.builder()
                                                .sortingStation(DOOR_S_03)
                                                .orders(listOf(owdB000003003(), owdB000004001(), owdB000005002(),
                                                        owdB000003004()))
                                                .build()
                                )
                        ).build(),

                        // 2020-01-01 + 2020-01-02
                        Batch.builder().id("1").subBatches(
                                listOf(
                                        // 2020-01-01
                                        SubBatch.builder()
                                                .sortingStation(DOOR_S_04)
                                                .orders(listOf(owdB000003002(), owdB000001003(), owdB000001004(),
                                                        owdB000002002()))
                                                .build()
                                )
                        ).build()
                        // door 5 is partly occupied - cannot host DEFAULT batch
                )
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
