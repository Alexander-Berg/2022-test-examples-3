package ru.yandex.market.sc.core.domain.order.repository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author valter
 */
@EmbeddedDbTest
class ScOrderRepositoryTest {

    @MockBean
    Clock clock;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    TestFactory testFactory;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
    }

    @Test
    void save() {
        var expected = testFactory.scOrder();
        assertThat(scOrderRepository.save(expected))
                .isEqualToIgnoringGivenFields(expected, "id", "createdAt", "updatedAt");
    }

    @Test
    void acceptAndShipReturnedOrder() {
        var sortingCenter = testFactory.storedSortingCenter();
        var courierDto = testFactory.defaultCourier();
        testFactory.storedWarehouse("10001700279");
        testFactory.storedDeliveryService("ds_for_client_return", sortingCenter.getId(), true);
        var retOrder = testFactory.createClientReturnForToday(sortingCenter, courierDto, "VOZVRAT_TAR_1")
                .get();
        testFactory.accept(retOrder);
        retOrder = testFactory.getOrder(retOrder.getId());
        assertThat(retOrder.getFirstArrivedAt()).isEqualTo(Instant.now(clock));

        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.HOURS));

        testFactory.sortOrder(retOrder);
        testFactory.shipOrderRoute(retOrder);
        retOrder = testFactory.getOrder(retOrder.getId());
        assertThat(retOrder.getLastShippedAt()).isEqualTo(Instant.now(clock));
    }

}
