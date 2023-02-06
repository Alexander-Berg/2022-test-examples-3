package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData;
import ru.yandex.market.wms.autostart.model.dto.AOSSettingsDto;
import ru.yandex.market.wms.autostart.model.dto.AOSZoneSettingsDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.mapOf;

/**
 * Кейс: uniform - false
 * threshold - 30
 */
@ContextConfiguration(classes = {
        PickingOrdersBatchingServiceTest.TestPickingOrdersBatchingService.class,
})
class PickingOrdersBatchingServiceTest extends AutostartIntegrationTest {

    static AtomicInteger batchKey = new AtomicInteger();
    static AtomicInteger pickingOrderKey = new AtomicInteger();
    @Autowired
    protected PickingOrdersBatchingService service;

    @BeforeEach
    void beforeEach() {
        batchKey.set(0);
        pickingOrderKey.set(0);
    }

    @Test
    @Disabled
    void makePickingOrderBatches() {
        PickingOrdersPlanner planner = new PickingOrdersPlanner(OrderInventoryDetailTestData.sampleInventory());
        assertThat(
                service.makePickingOrderBatches(
                        Arrays.asList(
                                // CC 101, 2020-01-01
                                OrderBatchTestData.orderBatch0("0"),

                                // CC 101, 2020-01-01 + 2020-01-02
                                OrderBatchTestData.orderBatch1("1"),

                                // CC 102, 2020-01-01
                                OrderBatchTestData.orderBatch2("2"),

                                // CC 100, 2020-01-02
                                OrderBatchTestData.orderBatch3("3")
                        ),
                        mapOf(),
                        true,
                        10,
                        planner
                ),
                is(equalTo(
                        Arrays.asList(
                                PickingOrderBatchTestData.pickingOrderBatch0("0", "0"),
                                PickingOrderBatchTestData.pickingOrderBatch1("1", "1"),
                                PickingOrderBatchTestData.pickingOrderBatch2("2", "2"),
                                PickingOrderBatchTestData.pickingOrderBatch3("3", "3")
                        )
                ))
        );
    }

    @Configuration
    public static class TestPickingOrdersBatchingService {

        @Bean
        @Primary
        public PickingOrdersBatchingService pickingOrdersBatchingService() {
            return new PickingOrdersBatchingService(
                    () -> String.valueOf(batchKey.getAndIncrement()),
                    () -> String.valueOf(pickingOrderKey.getAndIncrement()),
                    zone -> AOSZoneSettingsDto.builder()
                            .itemsIntoPickingOrder(30)
                            .build(),
                    AOSSettingsDto.builder().uniformPickingOrdersEnabled(false).itemsIntoPickingOrder(0).build()
            );
        }
    }
}
