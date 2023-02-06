package ru.yandex.market.ff.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;

public class TrackerComponentServiceTest extends IntegrationTest {

    @Autowired
    private TrackerComponentService mapper;

    @Test
    @DatabaseSetup("classpath:repository/tracker-component-to-fulfillment-service-map-repository/" +
            "find-by-fulfillment-service-id/before.xml")
    void whenFound() {
        Long componentIdForFulfillmentService =
                mapper.findComponentIdForFulfillmentService(172L, SupplierType.FIRST_PARTY);
        assertions.assertThat(componentIdForFulfillmentService).isEqualTo(61306);
    }

    @Test
    @DatabaseSetup("classpath:repository/tracker-component-to-fulfillment-service-map-repository/" +
            "find-by-fulfillment-service-id/before.xml")
    void whenNotFound() {
        Long componentIdForFulfillmentService =
                mapper.findComponentIdForFulfillmentService(145L, SupplierType.FIRST_PARTY);
        assertions.assertThat(componentIdForFulfillmentService).isNull();
    }

}
