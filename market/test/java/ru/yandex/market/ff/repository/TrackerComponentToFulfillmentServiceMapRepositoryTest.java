package ru.yandex.market.ff.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.TrackerComponentToFulfillmentServiceMapEntity;

public class TrackerComponentToFulfillmentServiceMapRepositoryTest extends IntegrationTest {

    @Autowired
    private TrackerComponentToFulfillmentServiceMapRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/tracker-component-to-fulfillment-service-map-repository/" +
            "find-by-fulfillment-service-id/before.xml")
    void findByFulfillmentServiceIdSuccessfully() {
        TrackerComponentToFulfillmentServiceMapEntity map =
                repository.findByFulfillmentServiceIdAndSupplierType(172L, SupplierType.FIRST_PARTY);

        assertions.assertThat(map.getComponentId()).isEqualTo(61306);

    }

}
